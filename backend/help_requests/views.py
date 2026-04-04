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

from rest_framework.parsers import MultiPartParser
from rest_framework.views import APIView
from rest_framework.response import Response
from rest_framework import status
from rest_framework.permissions import IsAuthenticated

from accounts.models import User
from .models import HelpRequest, HelpOffer
from .serializers import (
    HelpRequestListSerializer,
    HelpRequestDetailSerializer,
    HelpRequestCreateSerializer,
    HelpRequestUpdateSerializer,
    HelpCommentSerializer,
    HelpOfferSerializer,
    HelpOfferCreateSerializer,
)
from .services import update_status_on_expert_comment


class HelpRequestListCreateView(APIView):
    """
    GET  /help-requests/  — list active help requests, filterable by hub and category.
    POST /help-requests/  — create a new help request (authenticated users only).
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
        return Response(
            HelpRequestDetailSerializer(help_request, context={'request': request}).data,
            status=status.HTTP_201_CREATED,
        )


class HelpRequestDetailView(APIView):
    """
    GET    /help-requests/{id}/  — full detail of a single help request.
    PUT    /help-requests/{id}/  — update a help request (author only).
    DELETE /help-requests/{id}/  — delete a help request (author only).
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
        if help_request.author != request.user:
            return Response(
                {'detail': 'You can only delete your own help requests.'},
                status=status.HTTP_403_FORBIDDEN,
            )
        help_request.delete()
        return Response(
            {'detail': 'Help request deleted.'},
            status=status.HTTP_204_NO_CONTENT,
        )


class HelpRequestStatusView(APIView):
    """
    PATCH /help-requests/{id}/status/  — update the status of a help request.

    Only the request's author can change the status.  The only status transition
    accepted from this endpoint is setting status to RESOLVED.
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
        help_request.save(update_fields=['status'])

        return Response(
            HelpRequestDetailSerializer(help_request, context={'request': request}).data,
        )


# ── Comments ───────────────────────────────────────────────────────────────────

class HelpCommentListCreateView(APIView):
    """
    GET  /help-requests/{id}/comments/  — list comments on a help request.
    POST /help-requests/{id}/comments/  — add a comment to a help request.

    When the commenter has role=EXPERT and the request is still OPEN,
    the request status is automatically promoted to EXPERT_RESPONDING.
    The comment_count on the parent request is kept in sync within the
    same database transaction.
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

            # If the commenter is an expert, promote the request status.
            # This call is safe — it won't overwrite RESOLVED status.
            if request.user.role == User.Role.EXPERT:
                update_status_on_expert_comment(help_request)

        return Response(serializer.data, status=status.HTTP_201_CREATED)


# ── Help Offers ────────────────────────────────────────────────────────────────

class HelpOfferListCreateView(APIView):
    """
    GET  /help-offers/  — list help offers, filterable by hub and category.
    POST /help-offers/  — create a new help offer (authenticated users only).

    Offers are standalone — they are not tied to any specific help request.
    A user posts an offer to advertise a skill or resource they can provide
    to the community (e.g. "I have a vehicle for transport").
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
    DELETE /help-offers/{id}/  — delete a help offer (author only).

    Only the user who created the offer can delete it. Returns 403 if
    the requester is not the author.
    """
    permission_classes = [IsAuthenticated]

    def delete(self, request, pk):
        offer = get_object_or_404(HelpOffer, pk=pk)
        if offer.author != request.user:
            return Response(
                {'detail': 'You can only delete your own help offers.'},
                status=status.HTTP_403_FORBIDDEN,
            )
        offer.delete()
        return Response(
            {'detail': 'Help offer deleted.'},
            status=status.HTTP_204_NO_CONTENT,
        )


# ── Image Upload ───────────────────────────────────────────────────────────────

ALLOWED_IMAGE_TYPES = {'image/jpeg', 'image/png', 'image/gif', 'image/webp'}
MAX_IMAGE_SIZE = 5 * 1024 * 1024  # 5 MB


class ImageUploadView(APIView):
    """
    POST /help-requests/upload/
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
            urls.append(f'{settings.MEDIA_URL}{saved}')

        return Response({'urls': urls}, status=status.HTTP_201_CREATED)
