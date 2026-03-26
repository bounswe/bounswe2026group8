from django.conf import settings
from django.db import models

from accounts.models import Hub


class Post(models.Model):
    class ForumType(models.TextChoices):
        STANDARD = 'STANDARD', 'Standard'
        URGENT = 'URGENT', 'Urgent'
        GLOBAL = 'GLOBAL', 'Global'

    class Status(models.TextChoices):
        ACTIVE = 'ACTIVE', 'Active'
        HIDDEN = 'HIDDEN', 'Hidden'
        REMOVED = 'REMOVED', 'Removed'

    hub = models.ForeignKey(
        Hub, on_delete=models.CASCADE, related_name='posts', null=True, blank=True,
    )
    forum_type = models.CharField(max_length=10, choices=ForumType.choices, default=ForumType.STANDARD)
    author = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.CASCADE, related_name='posts')
    reposted_from = models.ForeignKey(
        'self', on_delete=models.SET_NULL, null=True, blank=True, related_name='reposts',
    )
    title = models.CharField(max_length=300)
    content = models.TextField()
    image_urls = models.JSONField(default=list, blank=True)
    status = models.CharField(max_length=10, choices=Status.choices, default=Status.ACTIVE)

    upvote_count = models.PositiveIntegerField(default=0)
    downvote_count = models.PositiveIntegerField(default=0)
    comment_count = models.PositiveIntegerField(default=0)
    report_count = models.PositiveIntegerField(default=0)
    repost_count = models.PositiveIntegerField(default=0)

    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)

    class Meta:
        ordering = ['-created_at']
        indexes = [
            models.Index(fields=['hub', 'forum_type', '-created_at']),
        ]

    def __str__(self):
        return f'[{self.forum_type}] {self.title}'


class Comment(models.Model):
    post = models.ForeignKey(Post, on_delete=models.CASCADE, related_name='comments')
    author = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.CASCADE, related_name='comments')
    content = models.TextField()
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        ordering = ['created_at']

    def __str__(self):
        return f'Comment by {self.author} on {self.post_id}'


class Vote(models.Model):
    class VoteType(models.TextChoices):
        UP = 'UP', 'Upvote'
        DOWN = 'DOWN', 'Downvote'

    post = models.ForeignKey(Post, on_delete=models.CASCADE, related_name='votes')
    user = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.CASCADE, related_name='votes')
    vote_type = models.CharField(max_length=4, choices=VoteType.choices)
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        constraints = [
            models.UniqueConstraint(fields=['user', 'post'], name='unique_vote_per_user_post'),
        ]

    def __str__(self):
        return f'{self.user} → {self.vote_type} on post {self.post_id}'


class Report(models.Model):
    class Reason(models.TextChoices):
        SPAM = 'SPAM', 'Spam'
        MISINFORMATION = 'MISINFORMATION', 'Misinformation'
        ABUSE = 'ABUSE', 'Abuse'
        IRRELEVANT = 'IRRELEVANT', 'Irrelevant'

    post = models.ForeignKey(Post, on_delete=models.CASCADE, related_name='reports')
    user = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.CASCADE, related_name='reports')
    reason = models.CharField(max_length=16, choices=Reason.choices)
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        constraints = [
            models.UniqueConstraint(fields=['user', 'post'], name='unique_report_per_user_post'),
        ]

    def __str__(self):
        return f'{self.user} reported post {self.post_id}: {self.reason}'
