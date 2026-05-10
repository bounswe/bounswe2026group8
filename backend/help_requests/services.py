"""
Service functions for the help_requests app.

Business logic that involves multiple models or has rules beyond simple CRUD
lives here, keeping views thin. This is where the status-transition rules are
enforced so that every caller (views, tests, management commands) goes through
the same logic.
"""

from django.utils import timezone

from accounts.models import User
from .models import HelpRequest


def update_status_on_expert_comment(help_request):
    """
    Legacy hook — expert comments no longer promote status.

    The "expert responding" label now depends solely on
    ``assigned_expert`` being set (via the take-on endpoint).
    This function is intentionally a no-op so that any remaining
    callers do not break, but it will not change any state.

    Returns False (status was NOT changed).
    """
    return False
