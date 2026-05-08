from django.db import IntegrityError, transaction
from django.db.models import F

from .models import Profile, RewardEvent


HELP_COMMENT_POINTS = 2
HELP_OFFER_POINTS = 5
HELP_RESOLVED_POINTS = 10


def reward_summary(profile):
    """Return a compact reward summary for API responses."""
    points = profile.trust_points
    completed = profile.completed_help_count
    contributions = profile.contribution_count

    if points >= 100:
        level = 'Community Mentor'
    elif points >= 50:
        level = 'Trusted Helper'
    elif points >= 20:
        level = 'Active Helper'
    else:
        level = 'New Helper'

    badges = []
    if contributions >= 1:
        badges.append({'code': 'FIRST_HELP', 'label': 'First Help'})
    if contributions >= 5:
        badges.append({'code': 'COMMUNITY_SUPPORTER', 'label': 'Community Supporter'})
    if completed >= 3:
        badges.append({'code': 'RELIABLE_HELPER', 'label': 'Reliable Helper'})
    if points >= 50:
        badges.append({'code': 'TRUSTED_HELPER', 'label': 'Trusted Helper'})

    return {
        'trust_points': points,
        'level': level,
        'contribution_count': contributions,
        'completed_help_count': completed,
        'badges': badges,
    }


def award_reward(user, event_type, points, source_key, completed_help=False):
    """
    Award a user once for a source action.

    Returns True when a new reward was created, False when the source had
    already been rewarded for this user.
    """
    try:
        with transaction.atomic():
            RewardEvent.objects.create(
                user=user,
                event_type=event_type,
                points=points,
                source_key=source_key,
            )
            Profile.objects.filter(user=user).update(
                trust_points=F('trust_points') + points,
                contribution_count=F('contribution_count') + 1,
                completed_help_count=F('completed_help_count') + (1 if completed_help else 0),
            )
        return True
    except IntegrityError:
        return False


def revoke_reward(user, source_key):
    """Remove a reward event and subtract its contribution from profile totals."""
    with transaction.atomic():
        event = (
            RewardEvent.objects
            .select_for_update()
            .filter(user=user, source_key=source_key)
            .first()
        )
        if not event:
            return False

        profile = Profile.objects.select_for_update().get(user=user)
        profile.trust_points = max(0, profile.trust_points - event.points)
        profile.contribution_count = max(0, profile.contribution_count - 1)
        if event.event_type == RewardEvent.EventType.HELP_REQUEST_RESOLVED:
            profile.completed_help_count = max(0, profile.completed_help_count - 1)
        profile.save(update_fields=['trust_points', 'contribution_count', 'completed_help_count'])
        event.delete()
        return True
