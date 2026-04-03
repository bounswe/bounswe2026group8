import logging
import uuid
from pathlib import Path

from django.conf import settings
from django.core.files.storage import default_storage
from django.db import IntegrityError, transaction
from django.db.models import F
from django.shortcuts import get_object_or_404

from rest_framework.parsers import MultiPartParser
from rest_framework.views import APIView
from rest_framework.response import Response
from rest_framework import status
from rest_framework.permissions import AllowAny, IsAuthenticated

import firebase_admin
from firebase_admin import messaging

from accounts.models import User

from .models import Post, Comment, Vote, Report
from .serializers import (
    PostListSerializer,
    PostDetailSerializer,
    PostCreateSerializer,
    PostUpdateSerializer,
    CommentSerializer,
    VoteSerializer,
    ReportSerializer,
)

logger = logging.getLogger(__name__)


# ── Post CRUD ──────────────────────────────────────────────────────────────────

class PostListCreateView(APIView):
    """
    GET  /forum/posts/         — list posts (filtered by hub, forum_type)
    POST /forum/posts/         — create a post (authenticated)
    """

    def get_permissions(self):
        if self.request.method == 'GET':
            return [AllowAny()]
        return [IsAuthenticated()]

    def get(self, request):
        qs = Post.objects.filter(status=Post.Status.ACTIVE)
        forum_type = request.query_params.get('forum_type')
        if forum_type:
            ft = forum_type.upper()
            qs = qs.filter(forum_type=ft)
            if ft != Post.ForumType.GLOBAL:
                hub = request.query_params.get('hub')
                if hub:
                    qs = qs.filter(hub_id=hub)
        else:
            hub = request.query_params.get('hub')
            if hub:
                qs = qs.filter(hub_id=hub)
        serializer = PostListSerializer(qs, many=True, context={'request': request})
        return Response(serializer.data)

    def post(self, request):
        serializer = PostCreateSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        post = serializer.save(author=request.user)

        if post.forum_type == Post.ForumType.URGENT:
            _send_urgent_post_notification(post)

        return Response(
            PostDetailSerializer(post, context={'request': request}).data,
            status=status.HTTP_201_CREATED,
        )


class PostDetailView(APIView):
    """
    GET    /forum/posts/{id}/  — detail
    PUT    /forum/posts/{id}/  — update (author only)
    DELETE /forum/posts/{id}/  — delete (author only)
    """

    def get_permissions(self):
        if self.request.method == 'GET':
            return [AllowAny()]
        return [IsAuthenticated()]

    def get_object(self, pk):
        return get_object_or_404(Post, pk=pk)

    def get(self, request, pk):
        post = self.get_object(pk)
        serializer = PostDetailSerializer(post, context={'request': request})
        return Response(serializer.data)

    def put(self, request, pk):
        post = self.get_object(pk)
        if post.author != request.user:
            return Response(
                {'detail': 'You can only edit your own posts.'},
                status=status.HTTP_403_FORBIDDEN,
            )
        serializer = PostUpdateSerializer(post, data=request.data, partial=True)
        serializer.is_valid(raise_exception=True)
        serializer.save()
        return Response(PostDetailSerializer(post, context={'request': request}).data)

    def delete(self, request, pk):
        post = self.get_object(pk)
        if post.author != request.user:
            return Response(
                {'detail': 'You can only delete your own posts.'},
                status=status.HTTP_403_FORBIDDEN,
            )
        with transaction.atomic():
            if post.reposted_from_id:
                Post.objects.filter(pk=post.reposted_from_id).update(
                    repost_count=F('repost_count') - 1,
                )
            post.delete()
        return Response({'detail': 'Post deleted.'}, status=status.HTTP_204_NO_CONTENT)


# ── Comments ───────────────────────────────────────────────────────────────────

class CommentListCreateView(APIView):
    """
    GET  /forum/posts/{id}/comments/  — list comments for a post
    POST /forum/posts/{id}/comments/  — add a comment (authenticated)
    """

    def get_permissions(self):
        if self.request.method == 'GET':
            return [AllowAny()]
        return [IsAuthenticated()]

    def get(self, request, post_pk):
        post = get_object_or_404(Post, pk=post_pk)
        comments = post.comments.all()
        return Response(CommentSerializer(comments, many=True).data)

    def post(self, request, post_pk):
        post = get_object_or_404(Post, pk=post_pk)
        serializer = CommentSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        with transaction.atomic():
            serializer.save(author=request.user, post=post)
            Post.objects.filter(pk=post.pk).update(comment_count=F('comment_count') + 1)
        return Response(serializer.data, status=status.HTTP_201_CREATED)


class CommentDeleteView(APIView):
    """
    DELETE /forum/comments/{id}/  — delete a comment (author only)
    """
    permission_classes = [IsAuthenticated]

    def delete(self, request, pk):
        comment = get_object_or_404(Comment, pk=pk)
        if comment.author != request.user:
            return Response(
                {'detail': 'You can only delete your own comments.'},
                status=status.HTTP_403_FORBIDDEN,
            )
        post_pk = comment.post_id
        with transaction.atomic():
            comment.delete()
            Post.objects.filter(pk=post_pk).update(comment_count=F('comment_count') - 1)
        return Response({'detail': 'Comment deleted.'}, status=status.HTTP_204_NO_CONTENT)


# ── Voting ─────────────────────────────────────────────────────────────────────

class VoteView(APIView):
    """
    POST /forum/posts/{id}/vote/
    Toggle or switch a vote. Same vote type twice → remove. Different type → switch.
    """
    permission_classes = [IsAuthenticated]

    def post(self, request, post_pk):
        post = get_object_or_404(Post, pk=post_pk)
        serializer = VoteSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        new_type = serializer.validated_data['vote_type']

        with transaction.atomic():
            post_row = Post.objects.select_for_update().get(pk=post.pk)
            existing = Vote.objects.filter(user=request.user, post=post_row).first()

            if existing:
                if existing.vote_type == new_type:
                    # Toggle off
                    if existing.vote_type == Vote.VoteType.UP:
                        post_row.upvote_count = max(0, post_row.upvote_count - 1)
                    else:
                        post_row.downvote_count = max(0, post_row.downvote_count - 1)
                    existing.delete()
                    post_row.save(update_fields=['upvote_count', 'downvote_count'])
                    return Response({'detail': 'Vote removed.', 'vote': None})
                else:
                    # Switch vote
                    if existing.vote_type == Vote.VoteType.UP:
                        post_row.upvote_count = max(0, post_row.upvote_count - 1)
                        post_row.downvote_count += 1
                    else:
                        post_row.downvote_count = max(0, post_row.downvote_count - 1)
                        post_row.upvote_count += 1
                    existing.vote_type = new_type
                    existing.save(update_fields=['vote_type'])
                    post_row.save(update_fields=['upvote_count', 'downvote_count'])
                    return Response({'detail': 'Vote changed.', 'vote': new_type})
            else:
                # New vote
                Vote.objects.create(user=request.user, post=post_row, vote_type=new_type)
                if new_type == Vote.VoteType.UP:
                    post_row.upvote_count += 1
                else:
                    post_row.downvote_count += 1
                post_row.save(update_fields=['upvote_count', 'downvote_count'])
                return Response({'detail': 'Vote recorded.', 'vote': new_type}, status=status.HTTP_201_CREATED)


# ── Reporting ──────────────────────────────────────────────────────────────────

class ReportView(APIView):
    """
    POST /forum/posts/{id}/report/
    One report per user per post. Auto-hides the post when threshold is exceeded.
    """
    permission_classes = [IsAuthenticated]

    def post(self, request, post_pk):
        post = get_object_or_404(Post, pk=post_pk)
        serializer = ReportSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        reason = serializer.validated_data['reason']

        try:
            with transaction.atomic():
                Report.objects.create(user=request.user, post=post, reason=reason)
                post_row = Post.objects.select_for_update().get(pk=post.pk)
                post_row.report_count += 1
                threshold = getattr(settings, 'FORUM_REPORT_HIDE_THRESHOLD', 5)
                if post_row.report_count >= threshold:
                    post_row.status = Post.Status.HIDDEN
                post_row.save(update_fields=['report_count', 'status'])
        except IntegrityError:
            return Response(
                {'detail': 'You have already reported this post.'},
                status=status.HTTP_400_BAD_REQUEST,
            )

        return Response({'detail': 'Report submitted.'}, status=status.HTTP_201_CREATED)


# ── Repost ─────────────────────────────────────────────────────────────────────

class RepostView(APIView):
    """
    POST /forum/posts/{id}/repost/
    Creates a new post in the user's selected hub that references the original.
    Users cannot repost their own posts.
    """
    permission_classes = [IsAuthenticated]

    def post(self, request, post_pk):
        original = get_object_or_404(Post, pk=post_pk)

        if original.author == request.user:
            return Response(
                {'detail': 'You cannot repost your own post.'},
                status=status.HTTP_400_BAD_REQUEST,
            )

        if Post.objects.filter(author=request.user, reposted_from=original).exists():
            return Response(
                {'detail': 'You have already reposted this post.'},
                status=status.HTTP_400_BAD_REQUEST,
            )

        if original.forum_type == Post.ForumType.GLOBAL:
            hub_id = None
        else:
            hub_id = request.data.get('hub') or (
                request.user.hub_id if request.user.hub_id else original.hub_id
            )

        with transaction.atomic():
            repost_obj = Post.objects.create(
                hub_id=hub_id,
                forum_type=original.forum_type,
                author=request.user,
                reposted_from=original,
                title=original.title,
                content=original.content,
                image_urls=original.image_urls,
            )
            Post.objects.filter(pk=original.pk).update(repost_count=F('repost_count') + 1)

        return Response(
            PostDetailSerializer(repost_obj, context={'request': request}).data,
            status=status.HTTP_201_CREATED,
        )


# ── FCM Notifications ─────────────────────────────────────────────────────────

def _send_urgent_post_notification(post):
    """Send a push notification to hub members (or all users for GLOBAL) when an urgent post is created."""
    if not firebase_admin._apps:
        return

    # Determine target users: hub members or all users
    users = User.objects.filter(is_active=True).exclude(fcm_token__isnull=True).exclude(fcm_token='')
    if post.hub_id:
        users = users.filter(hub_id=post.hub_id)
    # Exclude the post author
    tokens = list(users.exclude(pk=post.author_id).values_list('fcm_token', flat=True))

    if not tokens:
        return

    data = {
        'post_id': str(post.id),
        'title': f'🚨 Urgent: {post.title}',
        'body': (post.content[:200] + '…') if len(post.content) > 200 else post.content,
    }

    # FCM supports max 500 tokens per multicast
    for i in range(0, len(tokens), 500):
        batch = tokens[i:i + 500]
        message = messaging.MulticastMessage(
            data=data,
            tokens=batch,
            android=messaging.AndroidConfig(priority='high'),
        )
        try:
            response = messaging.send_each_for_multicast(message)
            if response.failure_count > 0:
                logger.warning('FCM: %d/%d messages failed', response.failure_count, len(batch))
        except Exception:
            logger.exception('FCM: failed to send multicast')


# ── Image Upload ──────────────────────────────────────────────────────────────

ALLOWED_IMAGE_TYPES = {'image/jpeg', 'image/png', 'image/gif', 'image/webp'}
MAX_IMAGE_SIZE = 5 * 1024 * 1024  # 5 MB


class ImageUploadView(APIView):
    """
    POST /forum/upload/
    Accepts multipart image files, saves to media/uploads/, returns URLs.
    """
    permission_classes = [IsAuthenticated]
    parser_classes = [MultiPartParser]

    def post(self, request):
        files = request.FILES.getlist('images')
        if not files:
            return Response(
                {'detail': 'No images provided.'},
                status=status.HTTP_400_BAD_REQUEST,
            )

        urls = []
        for f in files:
            if f.content_type not in ALLOWED_IMAGE_TYPES:
                return Response(
                    {'detail': f'Unsupported file type: {f.content_type}. Allowed: JPEG, PNG, GIF, WebP.'},
                    status=status.HTTP_400_BAD_REQUEST,
                )
            if f.size > MAX_IMAGE_SIZE:
                return Response(
                    {'detail': f'File "{f.name}" exceeds the 5 MB limit.'},
                    status=status.HTTP_400_BAD_REQUEST,
                )

            ext = Path(f.name).suffix.lower() or '.jpg'
            filename = f'uploads/{uuid.uuid4().hex}{ext}'
            saved = default_storage.save(filename, f)
            urls.append(request.build_absolute_uri(f'{settings.MEDIA_URL}{saved}'))

        return Response({'urls': urls}, status=status.HTTP_201_CREATED)
