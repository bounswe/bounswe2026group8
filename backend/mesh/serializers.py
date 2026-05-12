from rest_framework import serializers

from .models import MeshOfflineMessage


class MeshOfflineMessageSerializer(serializers.ModelSerializer):
    class Meta:
        model = MeshOfflineMessage
        fields = [
            'id',
            'author_device_id',
            'author_display_name',
            'body',
            'created_at',
            'received_at',
            'ttl_hours',
            'hop_count',
            'latitude',
            'longitude',
            'loc_accuracy_meters',
            'loc_captured_at',
            'title',
            'post_type',
            'parent_post_id',
            'uploaded_at',
        ]
        read_only_fields = ['uploaded_at']
