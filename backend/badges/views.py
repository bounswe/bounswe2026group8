from django.shortcuts import get_object_or_404
from rest_framework import generics, permissions
from rest_framework.views import APIView
from rest_framework.response import Response
from rest_framework.permissions import IsAuthenticated

from accounts.models import User
from .models import UserBadge
from .serializers import UserBadgeSerializer

class MyBadgesView(generics.ListAPIView):
    """
    Get badges earned by the current user.
    
    GET /api/badges/my-badges
    
    Retrieve list of all badges and achievement progress for the authenticated user.
    Shows progress toward earning badges and criteria completion status.
    
    Authorization: Required (Bearer token)
    
    Returns: 200 OK with list of user badge progress objects
    """
    serializer_class = UserBadgeSerializer
    permission_classes = [permissions.IsAuthenticated]

    def get_queryset(self):
        # The Bouncer logic: ONLY return badges where the user matches the request token
        # .select_related('badge') is a performance optimization to prevent extra database queries
        return UserBadge.objects.filter(user=self.request.user).select_related('badge')
    

class UserBadgeListView(APIView):
    """
    Get badges earned by a specific user.
    
    GET /api/badges/users/{user_id}
    
    Retrieve list of all badges and achievement progress for a specific user.
    
    Authorization: Required (Bearer token)
    
    Parameters:
    - user_id (integer, path): ID of the user to retrieve badges for
    
    Returns: 200 OK with list of user badge progress objects
    Error: 404 Not Found if user doesn't exist
    """
    permission_classes = [IsAuthenticated] 

    def get(self, request, user_id):
        # 1. Verify the user actually exists
        target_user = get_object_or_404(User, id=user_id)
        
        # 2. Fetch all badges belonging to that user
        # .select_related('badge') added here too for the same performance boost!
        user_badges = UserBadge.objects.filter(user=target_user).select_related('badge')
        
        # 3. Serialize and return the data
        serializer = UserBadgeSerializer(user_badges, many=True)
        return Response(serializer.data)