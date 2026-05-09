from django.shortcuts import render
from rest_framework import generics, permissions
from .models import UserBadge
from .serializers import UserBadgeSerializer

class MyBadgesView(generics.ListAPIView):
    """
    Returns a list of all badge progress for the currently authenticated user.
    """
    serializer_class = UserBadgeSerializer
    permission_classes = [permissions.IsAuthenticated]

    def get_queryset(self):
        # The Bouncer logic: ONLY return badges where the user matches the request token
        # .select_related('badge') is a performance optimization to prevent extra database queries
        return UserBadge.objects.filter(user=self.request.user).select_related('badge')
