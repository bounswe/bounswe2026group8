from django.conf import settings
from django.db import models


class MeshOfflineMessage(models.Model):
    """
    Mirror of mobile's MeshMessage row, uploaded once a user comes online.

    The primary key is the UUID string the mobile generated when the post or
    comment was first authored. That makes the upload idempotent — if peer A
    and peer B both gossiped message X around the mesh and then both come
    online, the second upload of X is a no-op (already exists by id).

    Posts have title + post_type set and parent_post_id null.
    Comments have parent_post_id set and title/post_type null.
    """

    id = models.CharField(max_length=64, primary_key=True)
    author_device_id = models.CharField(max_length=64)
    author_display_name = models.CharField(max_length=128, null=True, blank=True)
    body = models.TextField()

    # Mobile-side timestamps as epoch millis (preserved verbatim so receivers
    # see the original author's clock, same as the mesh protocol does).
    created_at = models.BigIntegerField()
    received_at = models.BigIntegerField()
    ttl_hours = models.IntegerField(default=72)
    hop_count = models.IntegerField(default=0)

    # Optional location attached at send time.
    latitude = models.FloatField(null=True, blank=True)
    longitude = models.FloatField(null=True, blank=True)
    loc_accuracy_meters = models.FloatField(null=True, blank=True)
    loc_captured_at = models.BigIntegerField(null=True, blank=True)

    # Forum fields. parent_post_id is the UUID of the parent post for comments.
    title = models.CharField(max_length=300, null=True, blank=True)
    post_type = models.CharField(max_length=16, null=True, blank=True)
    parent_post_id = models.CharField(max_length=64, null=True, blank=True, db_index=True)

    # Server-side bookkeeping
    uploaded_at = models.DateTimeField(auto_now_add=True)
    uploaded_by = models.ForeignKey(
        settings.AUTH_USER_MODEL,
        on_delete=models.SET_NULL,
        null=True,
        blank=True,
        related_name='uploaded_mesh_messages',
    )

    class Meta:
        ordering = ['-created_at']
        indexes = [
            models.Index(fields=['parent_post_id', '-created_at']),
        ]

    def __str__(self):
        kind = 'comment' if self.parent_post_id else (self.post_type or 'post')
        return f'[{kind}] {self.id}'
