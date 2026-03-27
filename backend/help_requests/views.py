"""
Views for the help_requests app.

Implements CRUD endpoints for HelpRequest. Follows the same patterns as
forum/views.py: APIView-based, permission toggling between AllowAny (GET)
and IsAuthenticated (mutations), author-only guards on update/delete.
"""

from django.shortcuts import get_object_or_404

from rest_framework.views import APIView
from rest_framework.response import Response
from rest_framework import status
from rest_framework.permissions import IsAuthenticated

from .models import HelpRequest
from .serializers import (
    HelpRequestListSerializer,
    HelpRequestDetailSerializer,
    HelpRequestCreateSerializer,
    HelpRequestUpdateSerializer,
)


class HelpRequestListCreateView(APIView):
    """
    GET  /help-requests/  — list active help requests, filterable by hub and category.
    POST /help-requests/  — create a new help request (authenticated users only).
    """
    permission_classes = [IsAuthenticated]

    def get(self, request):
        qs = HelpRequest.objects.all()

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
        help_request = serializer.save(author=request.user)
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
