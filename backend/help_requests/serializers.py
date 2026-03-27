"""
Serializers for the help_requests app.

HelpRequestListSerializer  — compact representation for list views.
HelpRequestDetailSerializer — full representation with location and description.
HelpRequestCreateSerializer — validates input when creating a new request.
HelpRequestUpdateSerializer — validates input when updating an existing request.
"""

from rest_framework import serializers

from accounts.serializers import UserSerializer
from .models import HelpRequest


class HelpRequestListSerializer(serializers.ModelSerializer):
    """
    Compact representation returned by GET /help-requests/.
    Includes the nested author object and the hub name for display,
    but omits heavy fields like description and location coordinates.
    """
    author = UserSerializer(read_only=True)
    hub_name = serializers.SerializerMethodField()

    class Meta:
        model = HelpRequest
        fields = [
            'id', 'hub', 'hub_name', 'category', 'urgency',
            'author', 'title', 'status', 'comment_count',
            'created_at',
        ]
        read_only_fields = fields

    def get_hub_name(self, obj):
        return obj.hub.name if obj.hub_id else None


class HelpRequestDetailSerializer(serializers.ModelSerializer):
    """
    Full representation returned by GET /help-requests/{id}/.
    Adds description, location fields, and updated_at on top of the list fields.
    """
    author = UserSerializer(read_only=True)
    hub_name = serializers.SerializerMethodField()

    class Meta:
        model = HelpRequest
        fields = [
            'id', 'hub', 'hub_name', 'category', 'urgency',
            'author', 'title', 'description',
            'latitude', 'longitude', 'location_text',
            'status', 'comment_count',
            'created_at', 'updated_at',
        ]
        read_only_fields = fields

    def get_hub_name(self, obj):
        return obj.hub.name if obj.hub_id else None


class HelpRequestCreateSerializer(serializers.ModelSerializer):
    """
    Validates input for POST /help-requests/.
    The author is set in the view (from the authenticated user), not from input.
    Hub is optional — a request may not belong to any specific hub.
    """

    class Meta:
        model = HelpRequest
        fields = [
            'hub', 'category', 'urgency', 'title', 'description',
            'latitude', 'longitude', 'location_text',
        ]
        extra_kwargs = {
            'hub': {'required': False, 'allow_null': True},
            'urgency': {'required': False},
        }


class HelpRequestUpdateSerializer(serializers.ModelSerializer):
    """
    Validates input for PUT /help-requests/{id}/.
    Only allows updating content fields — author, hub, and status cannot be
    changed through this endpoint (status transitions are a separate issue).
    """

    class Meta:
        model = HelpRequest
        fields = [
            'category', 'urgency', 'title', 'description',
            'latitude', 'longitude', 'location_text',
        ]
