"""
Custom permissions for the authentication app.
Reserved for future use.
"""

from rest_framework.permissions import BasePermission


class IsAuthenticated(BasePermission):
    """
    Custom permission to check if user is authenticated.
    Can be extended with additional logic as needed.
    """
    def has_permission(self, request, view):
        return request.user and request.user.is_authenticated


class IsOwnerOrReadOnly(BasePermission):
    """
    Custom permission to allow users to edit only their own profile.
    Reserved for future use when implementing user profile endpoints.
    """
    def has_object_permission(self, request, view, obj):
        return obj.user == request.user
