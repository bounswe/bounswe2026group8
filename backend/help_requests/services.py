"""
Service functions for the help_requests app.

Business logic that involves multiple models or has rules beyond simple CRUD
lives here, keeping views thin. This is where the status-transition rules are
enforced so that every caller (views, tests, management commands) goes through
the same logic.
"""

from accounts.models import User
from .models import HelpRequest


def update_status_on_expert_comment(help_request):
    """
    If the help request is still open, promote its status to EXPERT_RESPONDING.

    Called after a comment is created by an expert user. The rule is:
      - OPEN -> EXPERT_RESPONDING  (an expert has started helping)
      - EXPERT_RESPONDING -> no change  (already being handled)
      - RESOLVED -> no change  (never revert a resolved request)

    This function writes directly to the database row using .update() to avoid
    race conditions — it doesn't load and re-save the Python object, so two
    concurrent updates won't overwrite each other.

    Returns True if the status was changed, False otherwise.
    """
    if help_request.status == HelpRequest.Status.OPEN:
        HelpRequest.objects.filter(pk=help_request.pk).update(
            status=HelpRequest.Status.EXPERT_RESPONDING,
        )
        # Update the in-memory object so the caller sees the new status
        # without needing to re-fetch from the database.
        help_request.status = HelpRequest.Status.EXPERT_RESPONDING
        return True
    return False
