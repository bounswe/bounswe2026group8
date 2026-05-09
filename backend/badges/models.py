from django.conf import settings
from django.db import models


class Badge(models.Model):
    """The Blueprint for a Badge"""
    
    class CriteriaType(models.TextChoices):
        POSTS = 'POSTS', 'Forum Posts Created'
        COMMENTS = 'COMMENTS', 'Comments Made'
        VOTES = 'VOTES', 'Votes Cast'
        # You can easily add more later, e.g., HELP_REQUESTS = 'HELP_REQUESTS'

    name = models.CharField(max_length=100)
    description = models.TextField()
    icon = models.CharField(max_length=10, help_text="Emoji or short text icon")
    
    criteria_type = models.CharField(
        max_length=50, 
        choices=CriteriaType.choices, 
        unique=True,
        help_text="The exact action this badge tracks."
    )

    # 📍 THIS IS WHERE THE MAGIC HAPPENS 
    milestones = models.JSONField(
        default=list, 
        help_text="Enter a JSON array of integers. Example: [1, 5, 10, 20, 50, 100]"
    )

    @property
    def max_level(self):
        """The max level is just the number of milestones provided."""
        return len(self.milestones)

    def __str__(self):
        return f"{self.name} ({self.criteria_type})"


class UserBadge(models.Model):
    """The Save File: Tracks a specific user's progress against a Blueprint"""
    
    user = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.CASCADE, related_name='badges')
    badge = models.ForeignKey(Badge, on_delete=models.CASCADE)
    
    current_level = models.IntegerField(default=0)
    current_progress = models.IntegerField(default=0)
    
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)

    class Meta:
        constraints = [
            models.UniqueConstraint(fields=['user', 'badge'], name='unique_user_badge'),
        ]

    @property
    def next_level_goal(self):
        """Dynamically fetches the next threshold from the Badge blueprint."""
        if self.current_level < self.badge.max_level:
            return self.badge.milestones[self.current_level]
        # If they are max level, just return the final milestone
        return self.badge.milestones[-1] if self.badge.milestones else 0

    @property
    def is_max_level(self):
        return self.current_level >= self.badge.max_level

    def __str__(self):
        return f"{self.email} - {self.badge.name} (Lvl {self.current_level})"