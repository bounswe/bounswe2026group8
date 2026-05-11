from rest_framework import serializers
from django.contrib.auth import authenticate
from django.contrib.auth.password_validation import validate_password
from django.core.exceptions import ValidationError as DjangoValidationError
from django.utils.text import slugify

from .models import Hub, User, Profile, Resource, ExpertiseField, ExpertiseCategory, UserSettings, StaffAuditLog


class UserSettingsSerializer(serializers.ModelSerializer):
    """Serializer for notification and public-profile privacy settings."""

    class Meta:
        model = UserSettings
        fields = [
            'notify_help_requests',
            'notify_urgent_posts',
            'notify_expertise_matches_only',
            'show_phone_number',
            'show_emergency_contact',
            'show_medical_info',
            'show_availability_status',
            'show_bio',
            'show_location',
            'show_resources',
            'show_expertise',
            'created_at',
            'updated_at',
        ]
        read_only_fields = ['created_at', 'updated_at']


class ProfileSerializer(serializers.ModelSerializer):
    """Serializer for the extended profile object."""

    class Meta:
        model = Profile
        fields = [
            'phone_number',
            'blood_type',
            'emergency_contact_phone',
            'special_needs',
            'has_disability',
            'availability_status',
            'bio',
            'preferred_language',
            'emergency_contact',
            'created_at',
            'updated_at',
        ]
        read_only_fields = ['created_at', 'updated_at']

    def validate_phone_number(self, value):
        """Convert empty string to None so the unique constraint allows multiple blank phones."""
        return value if value else None

    def validate_emergency_contact_phone(self, value):
        """Normalise blank to None."""
        return value if value else None


class ResourceSerializer(serializers.ModelSerializer):
    """Serializer for user-owned resources."""

    class Meta:
        model = Resource
        fields = ['id', 'name', 'category', 'quantity', 'condition', 'created_at', 'updated_at']
        read_only_fields = ['id', 'created_at', 'updated_at']


class ExpertiseCategorySerializer(serializers.ModelSerializer):
    """Read serializer for ExpertiseCategory — used nested inside ExpertiseFieldSerializer."""

    class Meta:
        model = ExpertiseCategory
        fields = ['id', 'name', 'help_request_category', 'translations']


class ExpertiseFieldSerializer(serializers.ModelSerializer):
    """Serializer for expert-only expertise entries.

    Verification fields are read-only here. Non-staff users can never write
    them through this endpoint; the verification workflow lives behind
    `IsVerificationCoordinatorOrAdmin` endpoints.
    """

    category = ExpertiseCategorySerializer(read_only=True)
    category_id = serializers.PrimaryKeyRelatedField(
        queryset=ExpertiseCategory.objects.filter(is_active=True),
        source='category',
        write_only=True,
    )
    reviewed_by_id = serializers.PrimaryKeyRelatedField(source='reviewed_by', read_only=True)
    reviewed_by_name = serializers.SerializerMethodField()

    class Meta:
        model = ExpertiseField
        fields = [
            'id',
            'category',
            'category_id',
            'certification_level',
            'certification_document_url',
            'verification_status',
            'reviewed_by_id',
            'reviewed_by_name',
            'reviewed_at',
            'verification_note',
            'created_at',
            'updated_at',
        ]
        read_only_fields = [
            'id',
            'verification_status',
            'reviewed_by_id',
            'reviewed_by_name',
            'reviewed_at',
            'verification_note',
            'created_at',
            'updated_at',
        ]

    def get_reviewed_by_name(self, obj):
        return obj.reviewed_by.full_name if obj.reviewed_by_id else None


class HubSerializer(serializers.ModelSerializer):
    class Meta:
        model = Hub
        fields = ['id', 'name', 'slug', 'country', 'city', 'district']
        read_only_fields = fields


class HubWriteSerializer(serializers.ModelSerializer):
    """Admin-only serializer for creating and updating hubs.

    `slug` is auto-derived from `name` when omitted so admins don't have to
    spell out the URL slug for every neighborhood.
    """

    slug = serializers.CharField(required=False, allow_blank=True, max_length=200)

    class Meta:
        model = Hub
        fields = ['id', 'name', 'slug', 'country', 'city', 'district']
        read_only_fields = ['id']

    def validate(self, attrs):
        slug = (attrs.get('slug') or '').strip()
        if not slug:
            base = attrs.get('name') or (self.instance.name if self.instance else '')
            slug = slugify(base)
        if not slug:
            raise serializers.ValidationError({'slug': ['Could not derive a slug; provide one explicitly.']})
        attrs['slug'] = slug
        return attrs


def resolve_or_create_hub(country, city, district=''):
    """Look up an existing Hub by (country, city, district) or create one.

    `country` and `city` are required; `district` is optional. Returns the Hub
    instance (created or found).
    """
    country = (country or '').strip()
    city = (city or '').strip()
    district = (district or '').strip()
    if not country or not city:
        raise serializers.ValidationError({'detail': 'country and city are required.'})

    hub, _ = Hub.objects.get_or_create(
        country=country,
        city=city,
        district=district,
        defaults=_hub_defaults(country, city, district),
    )
    return hub


def _hub_defaults(country, city, district):
    label = f'{city} / {district}, {country}' if district else f'{city}, {country}'
    base_slug = slugify(f'{country}-{city}-{district}' if district else f'{country}-{city}') or 'hub'
    slug = base_slug
    suffix = 2
    while Hub.objects.filter(slug=slug).exists():
        slug = f'{base_slug}-{suffix}'
        suffix += 1
    name = label
    name_suffix = 2
    while Hub.objects.filter(name=name).exists():
        name = f'{label} ({name_suffix})'
        name_suffix += 1
    return {'name': name, 'slug': slug}


class UserSerializer(serializers.ModelSerializer):
    """Read serializer for returning user data in API responses."""

    profile = ProfileSerializer(read_only=True)
    resources = ResourceSerializer(many=True, read_only=True)
    expertise_fields = ExpertiseFieldSerializer(many=True, read_only=True)
    hub = HubSerializer(read_only=True)

    class Meta:
        model = User
        fields = [
            'id',
            'full_name',
            'email',
            'role',
            'staff_role',
            'neighborhood_address',
            'profile',
            'resources',
            'expertise_fields',
            'hub',
        ]
        read_only_fields = fields


class StaffUserListSerializer(serializers.ModelSerializer):
    """Compact, admin-only user representation for `/staff/users/`."""

    hub = HubSerializer(read_only=True)

    class Meta:
        model = User
        fields = [
            'id',
            'full_name',
            'email',
            'role',
            'staff_role',
            'is_active',
            'hub',
            'created_at',
        ]
        read_only_fields = fields


class StaffRoleUpdateSerializer(serializers.Serializer):
    staff_role = serializers.ChoiceField(choices=User.StaffRole.choices)
    reason = serializers.CharField(required=False, allow_blank=True, max_length=500)


class AccountStatusUpdateSerializer(serializers.Serializer):
    is_active = serializers.BooleanField()
    reason = serializers.CharField(max_length=500)

    def validate_reason(self, value):
        if not value or not value.strip():
            raise serializers.ValidationError('A reason is required.')
        return value.strip()


class ExpertiseVerificationDecisionSerializer(serializers.Serializer):
    """
    Used by VerificationCoordinator/Admin to decide on an expertise record.
    Rejection requires a non-empty note; reopen clears the prior decision.
    """

    status = serializers.ChoiceField(choices=ExpertiseField.VerificationStatus.choices)
    note = serializers.CharField(required=False, allow_blank=True, max_length=2000)

    def validate(self, attrs):
        decision = attrs.get('status')
        note = (attrs.get('note') or '').strip()
        if decision == ExpertiseField.VerificationStatus.REJECTED and not note:
            raise serializers.ValidationError({'note': ['A note is required when rejecting.']})
        attrs['note'] = note
        return attrs


class StaffAuditLogSerializer(serializers.ModelSerializer):
    actor_email = serializers.SerializerMethodField()
    target_user_email = serializers.SerializerMethodField()

    class Meta:
        model = StaffAuditLog
        fields = [
            'id',
            'actor',
            'actor_email',
            'target_user',
            'target_user_email',
            'target_type',
            'target_id',
            'action',
            'previous_state',
            'new_state',
            'reason',
            'created_at',
        ]
        read_only_fields = fields

    def get_actor_email(self, obj):
        return obj.actor.email if obj.actor_id else None

    def get_target_user_email(self, obj):
        return obj.target_user.email if obj.target_user_id else None


class RegisterSerializer(serializers.Serializer):
    full_name = serializers.CharField(max_length=255)
    email = serializers.EmailField()
    password = serializers.CharField(write_only=True, min_length=8)
    confirm_password = serializers.CharField(write_only=True)
    role = serializers.ChoiceField(choices=User.Role.choices)
    hub_id = serializers.IntegerField(required=False, allow_null=True)
    hub_country = serializers.CharField(required=False, allow_blank=True, max_length=100)
    hub_city = serializers.CharField(required=False, allow_blank=True, max_length=120)
    hub_district = serializers.CharField(required=False, allow_blank=True, max_length=120)
    neighborhood_address = serializers.CharField(
        max_length=255, required=False, allow_blank=True, allow_null=True
    )
    category_id = serializers.PrimaryKeyRelatedField(
        queryset=ExpertiseCategory.objects.filter(is_active=True),
        required=False,
        allow_null=True,
        write_only=True,
    )

    def validate_email(self, value):
        if User.objects.filter(email__iexact=value).exists():
            raise serializers.ValidationError('A user with this email already exists.')
        return value.lower()

    def validate_hub_id(self, value):
        if value is not None and not Hub.objects.filter(pk=value).exists():
            raise serializers.ValidationError('Hub not found.')
        return value

    def validate(self, data):
        if data['password'] != data['confirm_password']:
            raise serializers.ValidationError({'confirm_password': ['Passwords do not match.']})
        if data.get('role') == User.Role.EXPERT and not data.get('category_id'):
            raise serializers.ValidationError({'category_id': ['Expertise category is required for Expert users.']})

        # Validate password against Django's password validators
        try:
            validate_password(data['password'])
        except DjangoValidationError as e:
            raise serializers.ValidationError({'password': e.messages})

        return data

    def create(self, validated_data):
        validated_data.pop('confirm_password')
        password = validated_data.pop('password')
        hub_id = validated_data.pop('hub_id', None)
        hub_country = validated_data.pop('hub_country', '').strip()
        hub_city = validated_data.pop('hub_city', '').strip()
        hub_district = validated_data.pop('hub_district', '').strip()
        category = validated_data.pop('category_id', None)
        if hub_country and hub_city:
            hub = resolve_or_create_hub(hub_country, hub_city, hub_district)
            validated_data['hub_id'] = hub.id
        elif hub_id is not None:
            validated_data['hub_id'] = hub_id
        # Defensive: registration must never set staff authority or active state.
        for forbidden in ('staff_role', 'is_active', 'is_staff', 'is_superuser'):
            validated_data.pop(forbidden, None)
        user = User.objects.create_user(
            email=validated_data.pop('email'),
            full_name=validated_data.pop('full_name'),
            password=password,
            **validated_data,
        )
        if category:
            ExpertiseField.objects.create(user=user, category=category)
        return user


class LoginSerializer(serializers.Serializer):
    email = serializers.EmailField()
    password = serializers.CharField(write_only=True)

    def validate(self, data):
        user = authenticate(username=data['email'].lower(), password=data['password'])
        if user is None:
            raise serializers.ValidationError({'non_field_errors': ['Invalid email or password.']})
        if not user.is_active:
            raise serializers.ValidationError({'non_field_errors': ['This account has been disabled.']})
        data['user'] = user
        return data
