"""
Views for the help_requests app.

Implements CRUD endpoints for HelpRequest and list/create for HelpComment.
Follows the same patterns as forum/views.py: APIView-based, IsAuthenticated
permission, author-only guards on update/delete, transaction.atomic() for
multi-table writes.
"""

import uuid
from pathlib import Path

from django.conf import settings
from django.core.files.storage import default_storage
from django.db import transaction
from django.db.models import F
from django.shortcuts import get_object_or_404
from django.utils import timezone

from rest_framework.parsers import MultiPartParser
from rest_framework.views import APIView
from rest_framework.response import Response
from rest_framework import status
from rest_framework.permissions import IsAuthenticated

from accounts.audit import record_staff_action
from accounts.models import StaffAuditLog, User
from accounts.permissions import IsModeratorOrAdmin, user_is_moderator_or_admin
from .models import HelpRequest, HelpComment, HelpOffer
from .serializers import (
    HelpRequestListSerializer,
    HelpRequestDetailSerializer,
    HelpRequestCreateSerializer,
    HelpRequestUpdateSerializer,
    HelpCommentSerializer,
    HelpOfferSerializer,
    HelpOfferCreateSerializer,
)
from .notifications import send_help_request_notification
from .services import update_status_on_expert_comment


class HelpRequestListCreateView(APIView):
    """
    List help requests and create new ones.
    
    GET /help-requests
    List all active help requests with optional filtering.
    Query parameters:
    - author (integer): Filter by request author ID
    - hub_id (integer): Filter by hub ID
    - category (string): Filter by category (MEDICAL, SHELTER, FOOD, etc.)
    - expertise_match (boolean): For experts only - show only requests matching their expertise
    
    POST /help-requests
    Create a new help request. Request body:
    - title (string, required): Request title
    - description (string, required): Detailed description
    - category (string, required): Category (MEDICAL, SHELTER, FOOD, etc.)
    - urgency (string, required): CRITICAL, HIGH, MEDIUM, LOW
    - latitude (float, optional): Geographic latitude
    - longitude (float, optional): Geographic longitude
    - hub_id (integer, optional): Hub ID (auto-assigned if not provided)
    
    Authorization: Required (Bearer token)
    
    Returns: 200 OK with list (GET) or 201 Created (POST)
    """
    permission_classes = [IsAuthenticated]

    def get(self, request):
        qs = HelpRequest.objects.all()

        author_id = request.query_params.get('author')
        if author_id:
            qs = qs.filter(author_id=author_id)

        # Filter by hub if provided (e.g. ?hub_id=3).
        hub_id = request.query_params.get('hub_id')
        if hub_id:
            qs = qs.filter(hub_id=hub_id)

        # Filter by category if provided (e.g. ?category=MEDICAL).
        category = request.query_params.get('category')
        if category:
            qs = qs.filter(category=category.upper())

        # expertise_match=true: authenticated EXPERT sees only requests matching
        # their approved expertise categories. Standard users are unaffected.
        if (
            request.query_params.get('expertise_match', '').lower() == 'true'
            and request.user.is_authenticated
            and request.user.role == User.Role.EXPERT
        ):
            matched_categories = (
                request.user.expertise_fields
                .filter(is_approved=True)
                .values_list('category__help_request_category', flat=True)
                .distinct()
            )
            qs = qs.filter(category__in=matched_categories)

        serializer = HelpRequestListSerializer(qs, many=True, context={'request': request})
        return Response(serializer.data)

    def post(self, request):
        serializer = HelpRequestCreateSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        # Author is always the authenticated user, never from client input.
        # Auto-assign the user's hub if not explicitly provided.
        extra = {'author': request.user}
        if 'hub' not in request.data and request.user.hub_id:
            extra['hub'] = request.user.hub
        help_request = serializer.save(**extra)
        send_help_request_notification(help_request)
        return Response(
            HelpRequestDetailSerializer(help_request, context={'request': request}).data,
            status=status.HTTP_201_CREATED,
        )


class HelpRequestDetailView(APIView):
    """
    Get, update, or delete a help request.
    
    GET /help-requests/{id}
    Retrieve full details of a specific help request.
    
    PUT /help-requests/{id}
    Update a help request (author or staff only). Supports partial updates.
    Request body: Any of title, description, category, urgency, latitude, longitude
    
    DELETE /help-requests/{id}
    Delete a help request (author or staff only).
    
    Authorization: Required (Bearer token)
    
    Returns: 200 OK (GET/PUT) or 204 No Content (DELETE)
    Error: 403 Forbidden if not author/staff, 404 if not found
    """
    permission_classes = [IsAuthenticated]

    def get_object(self, pk):
        return get_object_or_404(HelpRequest, pk=pk)

    def get(self, request, pk):
        help_request = self.get_object(pk)
        serializer = HelpRequestDetailSerializer(help_request, context={'request': request})
        return Response(serializer.data)

    def put(self, request, pk):
        help_request = self.get_object(pk)
        if help_request.author != request.user:
            return Response(
                {'detail': 'You can only edit your own help requests.'},
                status=status.HTTP_403_FORBIDDEN,
            )
        serializer = HelpRequestUpdateSerializer(
            help_request, data=request.data, partial=True,
        )
        serializer.is_valid(raise_exception=True)
        serializer.save()
        return Response(
            HelpRequestDetailSerializer(help_request, context={'request': request}).data,
        )

    def delete(self, request, pk):
        help_request = self.get_object(pk)
        is_author = help_request.author_id == request.user.id
        is_staff = user_is_moderator_or_admin(request.user)
        if not is_author and not is_staff:
            return Response(
                {'detail': 'You can only delete your own help requests.'},
                status=status.HTTP_403_FORBIDDEN,
            )
        snapshot = {
            'id': help_request.pk,
            'author_id': help_request.author_id,
            'title': help_request.title,
            'category': help_request.category,
            'urgency': help_request.urgency,
            'status': help_request.status,
        }
        reason = (request.data.get('reason') if hasattr(request, 'data') else '') or ''
        with transaction.atomic():
            help_request.delete()
            if is_staff and not is_author:
                record_staff_action(
                    actor=request.user,
                    action=StaffAuditLog.Action.HELP_REQUEST_DELETED,
                    target_type=StaffAuditLog.TargetType.HELP_REQUEST,
                    target_id=snapshot['id'],
                    previous_state=snapshot,
                    reason=reason,
                )
        return Response(
            {'detail': 'Help request deleted.'},
            status=status.HTTP_204_NO_CONTENT,
        )


class HelpRequestStatusView(APIView):
    """
    Update the status of a help request.
    
    PATCH /help-requests/{id}/status
    
    Mark a help request as resolved (author only).
    Request body:
    - status (string, required): Must be "RESOLVED"
    
    Authorization: Required (Bearer token)
    
    Returns: 200 OK with updated request
    Error: 403 Forbidden if not author, 400 if invalid status
    """
    permission_classes = [IsAuthenticated]

    def patch(self, request, pk):
        help_request = get_object_or_404(HelpRequest, pk=pk)

        # Author-only guard.
        if help_request.author != request.user:
            return Response(
                {'detail': 'Only the author can change the status.'},
                status=status.HTTP_403_FORBIDDEN,
            )

        new_status = request.data.get('status')
        if new_status != HelpRequest.Status.RESOLVED:
            return Response(
                {'detail': 'Only RESOLVED status is allowed from this endpoint.'},
                status=status.HTTP_400_BAD_REQUEST,
            )

        help_request.status = HelpRequest.Status.RESOLVED
        help_request.resolved_at = timezone.now()
        help_request.save(update_fields=['status', 'resolved_at'])

        return Response(
            HelpRequestDetailSerializer(help_request, context={'request': request}).data,
        )


class HelpRequestTakeOnView(APIView):
    """
    Expert takes responsibility for a help request.
    
    POST /help-requests/{id}/take-on
    
    An expert user claims responsibility for helping with this request.
    Request status changes to EXPERT_RESPONDING and expert is recorded.
    
    Eligibility: Expert must have matching expertise category approval.
    
    Authorization: Required (Bearer token, EXPERT role)
    
    Returns: 200 OK with updated request
    Error: 403 Forbidden if not eligible, 409 if already assigned
    """
    permission_classes = [IsAuthenticated]

    def post(self, request, pk):
        help_request = get_object_or_404(HelpRequest, pk=pk)

        ok, error = help_request.can_be_taken_by(request.user)
        if not ok:
            # Use 409 for "already assigned" to distinguish from auth errors.
            if 'already has an assigned expert' in error:
                http_status = status.HTTP_409_CONFLICT
            elif 'Resolved' in error:
                http_status = status.HTTP_400_BAD_REQUEST
            else:
                http_status = status.HTTP_403_FORBIDDEN
            return Response({'detail': error}, status=http_status)

        help_request.assigned_expert = request.user
        help_request.assigned_at = timezone.now()
        help_request.status = HelpRequest.Status.EXPERT_RESPONDING
        help_request.save(update_fields=['assigned_expert', 'assigned_at', 'status'])

        return Response(
            HelpRequestDetailSerializer(help_request, context={'request': request}).data,
        )


class HelpRequestReleaseView(APIView):
    """
    Release responsibility for a help request.
    
    POST /help-requests/{id}/release
    
    An assigned expert releases responsibility for a help request.
    Request status returns to OPEN and assigned_expert is cleared.
    
    Authorization: Required (Bearer token, must be assigned expert)
    
    Returns: 200 OK with updated request
    Error: 403 Forbidden if not assigned expert, 400 if already resolved
    """
    permission_classes = [IsAuthenticated]

    def post(self, request, pk):
        help_request = get_object_or_404(HelpRequest, pk=pk)

        if help_request.status == HelpRequest.Status.RESOLVED:
            return Response(
                {'detail': 'Resolved requests cannot be released.'},
                status=status.HTTP_400_BAD_REQUEST,
            )

        if help_request.assigned_expert_id != request.user.id:
            return Response(
                {'detail': 'Only the assigned expert can release this request.'},
                status=status.HTTP_403_FORBIDDEN,
            )

        help_request.assigned_expert = None
        help_request.assigned_at = None
        help_request.status = HelpRequest.Status.OPEN
        help_request.save(update_fields=['assigned_expert', 'assigned_at', 'status'])

        return Response(
            HelpRequestDetailSerializer(help_request, context={'request': request}).data,
        )


# ── Comments ───────────────────────────────────────────────────────────────────

class HelpCommentListCreateView(APIView):
    """
    List and create comments on a help request.
    
    GET /help-requests/{id}/comments
    List all comments on a help request, ordered by creation time.
    
    POST /help-requests/{id}/comments
    Add a comment to a help request.
    Request body:
    - content (string, required): Comment text
    - image_url (string, optional): URL to attached image
    
    Note: When an expert comments, the request may be marked as EXPERT_RESPONDING.
    
    Authorization: Required (Bearer token)
    
    Returns: 200 OK (GET) or 201 Created (POST)
    """
    permission_classes = [IsAuthenticated]

    def get(self, request, request_pk):
        help_request = get_object_or_404(HelpRequest, pk=request_pk)
        # Comments are ordered by created_at ascending (oldest first) via model Meta.
        comments = help_request.comments.all()
        return Response(HelpCommentSerializer(comments, many=True).data)

    def post(self, request, request_pk):
        help_request = get_object_or_404(HelpRequest, pk=request_pk)
        serializer = HelpCommentSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)

        # Everything inside this block is wrapped in a single database transaction, so if any part fails 
        # (e.g. the comment fails to save, or the request fails to update), the entire transaction will be rolled back,
        # ensuring data integrity.
        with transaction.atomic():
            # Create the comment, setting author and request from server state.
            serializer.save(author=request.user, request=help_request)

            # Increment the denormalized comment_count using F() to avoid
            # race conditions (two comments created at the same instant).
            HelpRequest.objects.filter(pk=help_request.pk).update(
                comment_count=F('comment_count') + 1,
            )

            # Expert comments no longer trigger status promotion.
            # The "expert responding" label now depends solely on
            # assigned_expert being set via the take-on endpoint.

        return Response(serializer.data, status=status.HTTP_201_CREATED)


class HelpCommentDeleteView(APIView):
    """
    Delete a comment from a help request.
    
    DELETE /help-requests/comments/{id}
    
    Delete a comment (author or staff only).
    
    Authorization: Required (Bearer token)
    
    Returns: 204 No Content
    Error: 403 Forbidden if not author/staff, 404 if not found
    """
    permission_classes = [IsAuthenticated]

    def delete(self, request, pk):
        comment = get_object_or_404(HelpComment, pk=pk)
        is_author = comment.author_id == request.user.id
        is_staff = user_is_moderator_or_admin(request.user)
        if not is_author and not is_staff:
            return Response(
                {'detail': 'You can only delete your own comments.'},
                status=status.HTTP_403_FORBIDDEN,
            )
        snapshot = {
            'id': comment.pk,
            'request_id': comment.request_id,
            'author_id': comment.author_id,
            'content': comment.content,
        }
        reason = (request.data.get('reason') if hasattr(request, 'data') else '') or ''
        with transaction.atomic():
            HelpRequest.objects.filter(pk=comment.request_id).update(
                comment_count=F('comment_count') - 1,
            )
            comment.delete()
            if is_staff and not is_author:
                record_staff_action(
                    actor=request.user,
                    action=StaffAuditLog.Action.HELP_COMMENT_DELETED,
                    target_type=StaffAuditLog.TargetType.HELP_COMMENT,
                    target_id=snapshot['id'],
                    previous_state=snapshot,
                    reason=reason,
                )
        return Response({'detail': 'Comment deleted.'}, status=status.HTTP_204_NO_CONTENT)


# ── Help Offers ────────────────────────────────────────────────────────────────

class HelpOfferListCreateView(APIView):
    """
    List and create help offers.
    
    GET /help-offers
    List all help offers with optional filtering.
    Query parameters:
    - author (integer): Filter by offer author ID
    - hub_id (integer): Filter by hub ID
    - category (string): Filter by category (MEDICAL, TRANSPORT, SHELTER, etc.)
    
    POST /help-offers
    Create a new help offer to advertise skills or resources.
    Request body:
    - skill_or_resource (string, required): Description of what you're offering
    - category (string, required): Category (MEDICAL, TRANSPORT, SHELTER, etc.)
    - description (string, optional): Additional details
    - hub_id (integer, optional): Hub ID (auto-assigned if not provided)
    
    Authorization: Required (Bearer token)
    
    Returns: 200 OK with list (GET) or 201 Created (POST)
    """
    permission_classes = [IsAuthenticated]

    def get(self, request):
        qs = HelpOffer.objects.all()

        author_id = request.query_params.get('author')
        if author_id:
            qs = qs.filter(author_id=author_id)

        # Filter by hub if provided (e.g. ?hub_id=3).
        hub_id = request.query_params.get('hub_id')
        if hub_id:
            qs = qs.filter(hub_id=hub_id)

        # Filter by category if provided (e.g. ?category=TRANSPORT).
        category = request.query_params.get('category')
        if category:
            qs = qs.filter(category=category.upper())

        serializer = HelpOfferSerializer(qs, many=True, context={'request': request})
        return Response(serializer.data)

    def post(self, request):
        serializer = HelpOfferCreateSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        # Author is always the authenticated user, never from client input.
        # Auto-assign the user's hub if not explicitly provided.
        extra = {'author': request.user}
        if 'hub' not in request.data and request.user.hub_id:
            extra['hub'] = request.user.hub
        offer = serializer.save(**extra)
        return Response(
            HelpOfferSerializer(offer, context={'request': request}).data,
            status=status.HTTP_201_CREATED,
        )


class HelpOfferDeleteView(APIView):
    """
    Delete a help offer.
    
    DELETE /help-offers/{id}
    
    Delete a help offer (author or staff only).
    
    Authorization: Required (Bearer token)
    
    Returns: 204 No Content
    Error: 403 Forbidden if not author/staff, 404 if not found
    """
    permission_classes = [IsAuthenticated]

    def delete(self, request, pk):
        offer = get_object_or_404(HelpOffer, pk=pk)
        is_author = offer.author_id == request.user.id
        is_staff = user_is_moderator_or_admin(request.user)
        if not is_author and not is_staff:
            return Response(
                {'detail': 'You can only delete your own help offers.'},
                status=status.HTTP_403_FORBIDDEN,
            )
        snapshot = {
            'id': offer.pk,
            'author_id': offer.author_id,
            'skill_or_resource': offer.skill_or_resource,
            'category': offer.category,
        }
        reason = (request.data.get('reason') if hasattr(request, 'data') else '') or ''
        with transaction.atomic():
            offer.delete()
            if is_staff and not is_author:
                record_staff_action(
                    actor=request.user,
                    action=StaffAuditLog.Action.HELP_OFFER_DELETED,
                    target_type=StaffAuditLog.TargetType.HELP_OFFER,
                    target_id=snapshot['id'],
                    previous_state=snapshot,
                    reason=reason,
                )
        return Response(
            {'detail': 'Help offer deleted.'},
            status=status.HTTP_204_NO_CONTENT,
        )


# ── Image Upload ───────────────────────────────────────────────────────────────

ALLOWED_IMAGE_TYPES = {'image/jpeg', 'image/png', 'image/gif', 'image/webp'}
MAX_IMAGE_SIZE = 5 * 1024 * 1024  # 5 MB


class ImageUploadView(APIView):
    """
    Upload images for help requests.
    
    POST /help-requests/upload
    
    Upload one or more images for use in help requests or comments.
    Accepts JPEG, PNG, GIF, WebP up to 5MB each.
    
    Request body: multipart/form-data
    - images (file, required, multiple): Image files to upload
    
    Authorization: Required (Bearer token)
    
    Returns: 200 OK with list of image URLs
    Error: 400 Bad Request if invalid file type or exceeds size limit
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
            urls.append(f'{settings.MEDIA_URL}{saved}')

        return Response({'urls': urls}, status=status.HTTP_201_CREATED)


# ── Moderation ────────────────────────────────────────────────────────────────

class HelpRequestModerationListView(APIView):
    """GET /help-requests/moderation/ — moderator-only request list with all statuses."""

    permission_classes = [IsModeratorOrAdmin]

    def get(self, request):
        qs = HelpRequest.objects.all().select_related('author', 'hub')

        for key in ('category', 'urgency', 'status'):
            value = request.query_params.get(key)
            if value:
                qs = qs.filter(**{key: value.upper()})

        hub_id = request.query_params.get('hub_id')
        if hub_id:
            qs = qs.filter(hub_id=hub_id)

        return Response(HelpRequestListSerializer(qs, many=True, context={'request': request}).data)


class HelpOfferModerationListView(APIView):
    """GET /help-offers/moderation/ — moderator-only offer list."""

    permission_classes = [IsModeratorOrAdmin]

    def get(self, request):
        qs = HelpOffer.objects.all().select_related('author', 'hub')

        category = request.query_params.get('category')
        if category:
            qs = qs.filter(category=category.upper())

        hub_id = request.query_params.get('hub_id')
        if hub_id:
            qs = qs.filter(hub_id=hub_id)

        return Response(HelpOfferSerializer(qs, many=True, context={'request': request}).data)
