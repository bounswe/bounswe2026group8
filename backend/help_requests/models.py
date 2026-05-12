"""
Models for the help_requests app.

This app handles help requests, comments on those requests, and standalone
help offers. It is separate from the forum app. forum is for community
discussion, while help_requests is for actionable assistance.
"""

from django.conf import settings
from django.db import models

from accounts.models import Hub, User


class Category(models.TextChoices):
    """
    Shared category choices used by both HelpRequest and HelpOffer.
    Defined at module level to avoid duplication across models.
    """
    MEDICAL = 'MEDICAL', 'Medical'
    FOOD = 'FOOD', 'Food'
    SHELTER = 'SHELTER', 'Shelter'
    TRANSPORT = 'TRANSPORT', 'Transport'
    OTHER = 'OTHER', 'Other'


# Maps each help-request category to the set of ExpertiseCategory names that
# qualify an expert to take on requests in that category.  Used by
# HelpRequest.can_be_taken_by() to enforce expertise-matching.
CATEGORY_EXPERTISE_MAP = {
    'MEDICAL': {'First Aid', 'Doctor/Nurse', 'Paramedic', 'Psychologist', 'Pharmacist'},
    'SHELTER': {'Search & Rescue', 'Civil Engineer', 'Firefighter'},
    'TRANSPORT': {'Driver', 'Logistics'},
    'FOOD': {'Food Safety', 'Nutrition'},
    'OTHER': {'General Volunteer'},
}


class HelpRequest(models.Model):
    """
    A request for help posted by a user.

    Lifecycle: starts as OPEN, moves to EXPERT_RESPONDING when an expert
    takes on the request, and finally RESOLVED when the requester marks it done.
    Status never reverts from RESOLVED.
    """

    class Urgency(models.TextChoices):
        LOW = 'LOW', 'Low'
        MEDIUM = 'MEDIUM', 'Medium'
        HIGH = 'HIGH', 'High'

    class Status(models.TextChoices):
        OPEN = 'OPEN', 'Open'
        EXPERT_RESPONDING = 'EXPERT_RESPONDING', 'Expert Responding'
        RESOLVED = 'RESOLVED', 'Resolved'

    # Who posted this request.
    author = models.ForeignKey(
        settings.AUTH_USER_MODEL, on_delete=models.CASCADE, related_name='help_requests',
    )
    # The neighborhood hub this request belongs to (optional for general requests).
    hub = models.ForeignKey(
        Hub, on_delete=models.CASCADE, related_name='help_requests', null=True, blank=True,
    )
    # What kind of help is needed.
    category = models.CharField(max_length=10, choices=Category.choices)
    # How urgent the request is; defaults to LOW.
    urgency = models.CharField(max_length=6, choices=Urgency.choices, default=Urgency.LOW)

    title = models.CharField(max_length=300)
    description = models.TextField()

    # Optional GPS coordinates for map display (6 decimal places ≈ 11 cm precision).
    latitude = models.DecimalField(max_digits=9, decimal_places=6, null=True, blank=True)
    longitude = models.DecimalField(max_digits=9, decimal_places=6, null=True, blank=True)
    # Optional human-readable address (e.g. "Buyukdere Cad., Istanbul").
    # Uses default='' instead of null=True, following Django's convention for text fields.
    location_text = models.CharField(max_length=255, blank=True, default='')

    # Optional images attached by the requester (list of URLs or relative media paths).
    image_urls = models.JSONField(default=list, blank=True)

    # Tracks the lifecycle of the request (see Status choices above).
    status = models.CharField(max_length=20, choices=Status.choices, default=Status.OPEN)
    # Denormalized counter — avoids an extra COUNT query every time we list requests.
    # Kept in sync via F() expressions in the views layer.
    comment_count = models.PositiveIntegerField(default=0)

    # ── Expert assignment (responsibility tracking) ────────────────────────
    # The expert who has formally taken responsibility for this request.
    # Remains stored after resolve so we can later query resolved-by-expert counts.
    assigned_expert = models.ForeignKey(
        settings.AUTH_USER_MODEL, on_delete=models.SET_NULL,
        null=True, blank=True, related_name='assigned_help_requests',
    )
    assigned_at = models.DateTimeField(null=True, blank=True)
    resolved_at = models.DateTimeField(null=True, blank=True)

    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)

    class Meta:
        ordering = ['-created_at']
        indexes = [
            # Speeds up the most common query: filter by hub + category, sort by newest.
            models.Index(fields=['hub', 'category', '-created_at']),
        ]

    def __str__(self):
        return f'[{self.urgency}] {self.title}'

    # ── Eligibility check ──────────────────────────────────────────────────

    def can_be_taken_by(self, user):
        """
        Check whether *user* is eligible to take on this help request.

        Returns (True, '') on success, or (False, error_message) on failure.
        All eligibility rules are centralised here so they are not scattered
        across views.
        """
        if user.role != User.Role.EXPERT:
            return False, 'Only experts can take on help requests.'

        if self.author_id == user.id:
            return False, 'You cannot take on your own help request.'

        if self.assigned_expert_id is not None:
            return False, 'This help request already has an assigned expert.'

        if self.status == self.Status.RESOLVED:
            return False, 'Resolved requests cannot be taken on.'

        # Check expertise match: at least one of the expert's approved
        # expertise fields must be in the allowed set for this category.
        allowed_names = CATEGORY_EXPERTISE_MAP.get(self.category, set())
        has_match = user.expertise_fields.filter(
            is_approved=True,
            category__name__in=allowed_names,
        ).exists()
        if not has_match:
            return False, 'Your expertise does not match this request category.'

        return True, ''


class HelpComment(models.Model):
    """
    A comment on a HelpRequest.

    When an EXPERT user creates a comment, the parent request's status is
    automatically set to EXPERT_RESPONDING (handled in views, not here).
    """

    # The help request this comment belongs to.
    request = models.ForeignKey(
        HelpRequest, on_delete=models.CASCADE, related_name='comments',
    )
    # Who wrote this comment.
    author = models.ForeignKey(
        settings.AUTH_USER_MODEL, on_delete=models.CASCADE, related_name='help_comments',
    )
    content = models.TextField()
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        # Comments are displayed oldest-first (chronological conversation order).
        ordering = ['created_at']

    def __str__(self):
        return f'Comment by {self.author} on help request {self.request_id}'


class HelpOffer(models.Model):
    """
    A standalone offer of help, independent of any specific request.

    Users post these to advertise skills or resources they can provide
    to their neighborhood (e.g. "I have a vehicle for transport").
    """

    # Who is offering help.
    author = models.ForeignKey(
        settings.AUTH_USER_MODEL, on_delete=models.CASCADE, related_name='help_offers',
    )
    # The neighborhood hub this offer is for (optional).
    hub = models.ForeignKey(
        Hub, on_delete=models.CASCADE, related_name='help_offers', null=True, blank=True,
    )
    # What kind of help is offered.
    category = models.CharField(max_length=10, choices=Category.choices)
    # Short label for the skill or resource, e.g. "First Aid", "Vehicle".
    skill_or_resource = models.CharField(max_length=255)
    # Detailed explanation of what the person can provide.
    description = models.TextField()
    # When the person is available, e.g. "Weekdays 9-17", "Anytime".
    availability = models.CharField(max_length=255)
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        ordering = ['-created_at']
        indexes = [
            # Same query pattern as HelpRequest: filter by hub + category, sort by newest.
            models.Index(fields=['hub', 'category', '-created_at']),
        ]

    def __str__(self):
        return f'{self.skill_or_resource} ({self.category})'
