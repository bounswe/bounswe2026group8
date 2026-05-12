import logging

from rest_framework.permissions import IsAuthenticated
from rest_framework.response import Response
from rest_framework.views import APIView

from .models import MeshOfflineMessage
from .serializers import MeshOfflineMessageSerializer

logger = logging.getLogger(__name__)


class MeshSyncView(APIView):
    """
    Sync offline mesh messages to the server.
    
    POST /mesh-messages/sync
    
    Batch-upload messages that were created offline on mobile devices.
    Messages include both posts and comments. Idempotent: messages with
    duplicate IDs are skipped silently.
    
    Request body:
    - messages (array, required): List of offline message objects
      Each message should have: id, type, content, timestamp, etc.
    
    Authorization: Required (Bearer token)
    
    Returns: 200 OK with list of accepted message IDs
    Error: 400 Bad Request if messages is not a list
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
    List offline mesh posts.
    
    GET /mesh-messages
    
    Retrieve list of top-level posts from the mesh (posts without parent).
    Ordered newest first. These are messages synced from offline mobile clients.
    
    Authorization: Required (Bearer token)
    
    Returns: 200 OK with list of mesh posts
    """

    permission_classes = [IsAuthenticated]

    def get(self, request):
        qs = MeshOfflineMessage.objects.filter(parent_post_id__isnull=True)
        serializer = MeshOfflineMessageSerializer(qs, many=True)
        return Response(serializer.data)


class MeshCommentListView(APIView):
    """
    List comments on a mesh post.
    
    GET /mesh-messages/{post_id}/comments
    
    Retrieve comments/replies to a specific mesh post.
    Ordered by creation time (oldest first).
    
    Authorization: Required (Bearer token)
    
    Parameters:
    - post_id (integer, path): ID of the parent mesh post
    
    Returns: 200 OK with list of mesh comments
    """

    permission_classes = [IsAuthenticated]

    def get(self, request, post_id):
        qs = MeshOfflineMessage.objects.filter(
            parent_post_id=post_id
        ).order_by('created_at')
        serializer = MeshOfflineMessageSerializer(qs, many=True)
        return Response(serializer.data)
