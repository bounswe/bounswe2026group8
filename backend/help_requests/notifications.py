"""
FCM push notifications for help requests.

When a new help request is created, all experts in the same hub
(except the author) receive a push notification via Firebase Cloud Messaging.
"""

import logging

import firebase_admin
from firebase_admin import messaging

from accounts.models import User

logger = logging.getLogger(__name__)


def send_help_request_notification(help_request):
    """
    Send an FCM notification to experts in the same hub as the help request.

    - Targets only EXPERT users with a valid FCM token in the request's hub.
    - Excludes the request author.
    - Skips silently if Firebase is not initialised or the request has no hub.
    - Invalid/expired tokens are logged but never raise.
    """
    if not firebase_admin._apps:
        return

    # No hub means no targeting — skip to avoid spamming all experts.
    if not help_request.hub_id:
        return

    tokens = list(
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

    if not tokens:
        return

    data = {
        'type': 'help_request',
        'request_id': str(help_request.id),
        'title': f'New Help Request: {help_request.title}',
        'body': (
            f'{help_request.get_category_display()} | '
            f'{help_request.get_urgency_display()} urgency'
        ),
    }

    # FCM supports max 500 tokens per multicast.
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
