import logging

from rest_framework.permissions import IsAuthenticated
from rest_framework.response import Response
from rest_framework.views import APIView

from .models import MeshOfflineMessage
from .serializers import MeshOfflineMessageSerializer

logger = logging.getLogger(__name__)


class MeshSyncView(APIView):
    """
    POST /mesh-messages/sync/

    Batch-upload a list of mesh messages (posts and/or comments).
    Idempotent: messages whose id already exists are skipped silently. The
    response reports which ids were newly accepted so the mobile client can
    mark them syncedToServer locally.

    Request body:  { "messages": [ {...}, ... ] }
    Response:      { "accepted": ["id1", "id2", ...] }
    """

    permission_classes = [IsAuthenticated]

    def post(self, request):
        messages = request.data.get('messages', [])
        if not isinstance(messages, list):
            return Response({'detail': 'messages must be a list'}, status=400)

        existing = set(
            MeshOfflineMessage.objects.filter(
                id__in=[m.get('id') for m in messages if m.get('id')]
            ).values_list('id', flat=True)
        )

        accepted = []
        for item in messages:
            mid = item.get('id')
            if not mid or mid in existing:
                continue
            serializer = MeshOfflineMessageSerializer(data=item)
            if serializer.is_valid():
                serializer.save(uploaded_by=request.user)
                accepted.append(mid)
            else:
                logger.warning('mesh sync: invalid message %s: %s', mid, serializer.errors)

        return Response({'accepted': accepted})


class MeshPostListView(APIView):
    """
    GET /mesh-messages/

    List top-level posts (parent_post_id is null). Newest first.
    """

    permission_classes = [IsAuthenticated]

    def get(self, request):
        qs = MeshOfflineMessage.objects.filter(parent_post_id__isnull=True)
        serializer = MeshOfflineMessageSerializer(qs, many=True)
        return Response(serializer.data)


class MeshCommentListView(APIView):
    """
    GET /mesh-messages/<post_id>/comments/

    Comments on a given post, oldest first (chat-thread order).
    """

    permission_classes = [IsAuthenticated]

    def get(self, request, post_id):
        qs = MeshOfflineMessage.objects.filter(
            parent_post_id=post_id
        ).order_by('created_at')
        serializer = MeshOfflineMessageSerializer(qs, many=True)
        return Response(serializer.data)
