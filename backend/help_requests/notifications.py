"""
FCM push notifications for help requests.

When a new help request is created:
- Experts whose approved ExpertiseField matches the request's category are
  notified first (primary targeting).
- If no matching expert exists in the hub, ALL hub experts are notified as a
  fallback so the request is never silently ignored. The requester also receives
  a notification informing them that no specialist was available.
"""

import logging

import firebase_admin
from firebase_admin import messaging

from accounts.models import User

logger = logging.getLogger(__name__)


def send_help_request_notification(help_request):
    """
    Send FCM notifications for a newly created help request.

    Primary path:  matching experts (same hub, approved expertise in request category).
    Fallback path: all hub experts + requester informed that no specialist was found.
    Skips silently if Firebase is not initialised or the request has no hub.
    """
    if not firebase_admin._apps:
        return

    if not help_request.hub_id:
        return

    # Primary: experts with an approved expertise matching the request's category.
    matching_tokens = list(
        User.objects.filter(
            is_active=True,
            role='EXPERT',
            hub_id=help_request.hub_id,
            expertise_fields__category__help_request_category=help_request.category,
            expertise_fields__is_approved=True,
        )
        .exclude(fcm_token__isnull=True)
        .exclude(fcm_token='')
        .exclude(pk=help_request.author_id)
        .values_list('fcm_token', flat=True)
        .distinct()
    )

    if matching_tokens:
        _send_expert_multicast(matching_tokens, help_request)
        return

    # Fallback: no specialist found — notify all hub experts.
    fallback_tokens = list(
        User.objects.filter(
            is_active=True,
            role='EXPERT',
            hub_id=help_request.hub_id,
        )
        .exclude(fcm_token__isnull=True)
        .exclude(fcm_token='')
        .exclude(pk=help_request.author_id)
        .values_list('fcm_token', flat=True)
    )

    if fallback_tokens:
        _send_expert_multicast(fallback_tokens, help_request)

    # Notify the requester that no specialist was available.
    requester_token = help_request.author.fcm_token
    if requester_token:
        _notify_requester_no_match(requester_token, help_request)


def _send_expert_multicast(tokens, help_request):
    """Multicast the standard help-request notification to a list of FCM tokens."""
    data = {
        'type': 'help_request',
        'request_id': str(help_request.id),
        'title': f'New Help Request: {help_request.title}',
        'body': (
            f'{help_request.get_category_display()} | '
            f'{help_request.get_urgency_display()} urgency'
        ),
    }
    for i in range(0, len(tokens), 500):
        batch = tokens[i:i + 500]
        message = messaging.MulticastMessage(
            data=data,
            tokens=batch,
            android=messaging.AndroidConfig(priority='high'),
        )
        try:
            response = messaging.send_each_for_multicast(message)
            if response.failure_count > 0:
                logger.warning(
                    'FCM help-request: %d/%d messages failed',
                    response.failure_count,
                    len(batch),
                )
        except Exception:
            logger.exception('FCM help-request: failed to send multicast')


def _notify_requester_no_match(fcm_token, help_request):
    """Send a single FCM message to the requester when no matching expert was found."""
    message = messaging.Message(
        data={
            'type': 'no_expert_available',
            'request_id': str(help_request.id),
            'title': 'No specialist available',
            'body': (
                'No expert with matching expertise is available right now. '
                'Other experts in your hub have been notified.'
            ),
        },
        token=fcm_token,
        android=messaging.AndroidConfig(priority='high'),
    )
    try:
        messaging.send(message)
    except Exception:
        logger.exception('FCM help-request: failed to notify requester')
