"""
DRF permission classes and helpers for application staff roles.

Authorization is always read from the database user record, never from cached
JWT claims, so role changes take effect on the next request.
"""

from rest_framework.permissions import BasePermission

from .models import User


# ── Helpers ──────────────────────────────────────────────────────────────────

def user_is_admin(user) -> bool:
    return (
        user is not None
        and user.is_authenticated
        and user.is_active
        and user.staff_role == User.StaffRole.ADMIN
    )


def user_is_moderator_or_admin(user) -> bool:
    return (
        user is not None
        and user.is_authenticated
        and user.is_active
        and user.staff_role in (User.StaffRole.MODERATOR, User.StaffRole.ADMIN)
    )


def user_is_verification_coordinator_or_admin(user) -> bool:
    return (
        user is not None
        and user.is_authenticated
        and user.is_active
        and user.staff_role in (
            User.StaffRole.VERIFICATION_COORDINATOR,
            User.StaffRole.ADMIN,
        )
    )


# ── Permission classes ───────────────────────────────────────────────────────

class IsAdminStaffRole(BasePermission):
    """Application-level admins. Independent of Django `is_staff`/`is_superuser`."""

    message = 'Admin staff role required.'

    def has_permission(self, request, view):
        return user_is_admin(request.user)


class IsModeratorOrAdmin(BasePermission):
    """Forum/help content moderation."""

    message = 'Moderator or admin staff role required.'

    def has_permission(self, request, view):
        return user_is_moderator_or_admin(request.user)


class IsVerificationCoordinatorOrAdmin(BasePermission):
    """Expertise certification review."""

    message = 'Verification coordinator or admin staff role required.'

    def has_permission(self, request, view):
        return user_is_verification_coordinator_or_admin(request.user)
