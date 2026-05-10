"""
Serializers for the help_requests app.

HelpRequestListSerializer  — compact representation for list views.
HelpRequestDetailSerializer — full representation with location and description.
HelpRequestCreateSerializer — validates input when creating a new request.
HelpRequestUpdateSerializer — validates input when updating an existing request.
HelpCommentSerializer       — comment with nested author (includes role for expert badge).
HelpOfferSerializer         — offer for both list display and create responses.
HelpOfferCreateSerializer   — validates input when creating a new offer.
"""

from rest_framework import serializers

from accounts.serializers import UserSerializer
from .models import HelpRequest, HelpComment, HelpOffer


class HelpRequestListSerializer(serializers.ModelSerializer):
    """
    Compact representation returned by GET /help-requests/.
    Includes the nested author object and the hub name for display,
    but omits heavy fields like description and location coordinates.
    """
    author = UserSerializer(read_only=True)
    assigned_expert = UserSerializer(read_only=True)
    assigned_expert_username = serializers.SerializerMethodField()
    is_expert_responding = serializers.SerializerMethodField()
    hub_name = serializers.SerializerMethodField()

    class Meta:
        model = HelpRequest
        fields = [
            'id', 'hub', 'hub_name', 'category', 'urgency',
            'author', 'title', 'status', 'comment_count',
            'assigned_expert', 'assigned_expert_username',
            'assigned_at', 'is_expert_responding',
            'created_at',
        ]
        read_only_fields = fields

    def get_hub_name(self, obj):
        return obj.hub.name if obj.hub_id else None

    def get_assigned_expert_username(self, obj):
        return obj.assigned_expert.full_name if obj.assigned_expert_id else None

    def get_is_expert_responding(self, obj):
        return obj.assigned_expert_id is not None


class HelpRequestDetailSerializer(serializers.ModelSerializer):
    """
    Full representation returned by GET /help-requests/{id}/.
    Adds description, location fields, and updated_at on top of the list fields.
    """
    author = UserSerializer(read_only=True)
    assigned_expert = UserSerializer(read_only=True)
    assigned_expert_username = serializers.SerializerMethodField()
    is_expert_responding = serializers.SerializerMethodField()
    hub_name = serializers.SerializerMethodField()

    class Meta:
        model = HelpRequest
        fields = [
            'id', 'hub', 'hub_name', 'category', 'urgency',
            'author', 'title', 'description', 'image_urls',
            'latitude', 'longitude', 'location_text',
            'status', 'comment_count',
            'assigned_expert', 'assigned_expert_username',
            'assigned_at', 'resolved_at', 'is_expert_responding',
            'created_at', 'updated_at',
        ]
        read_only_fields = fields

    def get_hub_name(self, obj):
        return obj.hub.name if obj.hub_id else None

    def get_assigned_expert_username(self, obj):
        return obj.assigned_expert.full_name if obj.assigned_expert_id else None

    def get_is_expert_responding(self, obj):
        return obj.assigned_expert_id is not None


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
            'image_urls', 'latitude', 'longitude', 'location_text',
        ]
        extra_kwargs = {
            'hub': {'required': False, 'allow_null': True},
            'urgency': {'required': False},
            'image_urls': {'required': False},
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
            'image_urls', 'latitude', 'longitude', 'location_text',
        ]


class HelpCommentSerializer(serializers.ModelSerializer):
    """
    Serializer for HelpComment, used for both list and create responses.

    The nested UserSerializer includes the author's role field, which the
    frontend uses to display an "Expert" badge next to expert comments.
    The request and author fields are read-only because they are set in the
    view — request comes from the URL, author comes from the auth token.
    """
    author = UserSerializer(read_only=True)

    class Meta:
        model = HelpComment
        fields = ['id', 'request', 'author', 'content', 'created_at']
        read_only_fields = ['id', 'request', 'author', 'created_at']


class HelpOfferSerializer(serializers.ModelSerializer):
    """
    Read-only serializer for displaying help offers in list responses.

    Includes the nested author object (so the frontend can show who is
    offering) and the hub name for display.
    """
    author = UserSerializer(read_only=True)
    hub_name = serializers.SerializerMethodField()

    class Meta:
        model = HelpOffer
        fields = [
            'id', 'hub', 'hub_name', 'category', 'author',
            'skill_or_resource', 'description', 'availability',
            'created_at',
        ]
        read_only_fields = fields

    def get_hub_name(self, obj):
        return obj.hub.name if obj.hub_id else None


class HelpOfferCreateSerializer(serializers.ModelSerializer):
    """
    Validates input for POST /help-offers/.

    The author is set in the view from the authenticated user, not from input.
    Hub is optional — an offer may not be tied to a specific neighborhood.
    """

    class Meta:
        model = HelpOffer
        fields = [
            'hub', 'category', 'skill_or_resource', 'description', 'availability',
        ]
        extra_kwargs = {
            'hub': {'required': False, 'allow_null': True},
        }
