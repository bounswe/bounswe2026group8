from rest_framework import serializers
from django.contrib.auth import authenticate
from django.contrib.auth.password_validation import validate_password
from django.core.exceptions import ValidationError as DjangoValidationError

from .models import Hub, User, Profile, Resource, ExpertiseField, ExpertiseCategory


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
    """Serializer for expert-only expertise entries."""

    category = ExpertiseCategorySerializer(read_only=True)
    category_id = serializers.PrimaryKeyRelatedField(
        queryset=ExpertiseCategory.objects.filter(is_active=True),
        source='category',
        write_only=True,
    )

    class Meta:
        model = ExpertiseField
        fields = [
            'id', 'category', 'category_id', 'is_approved',
            'certification_level', 'certification_document_url',
            'created_at', 'updated_at',
        ]
        read_only_fields = ['id', 'is_approved', 'created_at', 'updated_at']


class HubSerializer(serializers.ModelSerializer):
    class Meta:
        model = Hub
        fields = ['id', 'name', 'slug']
        read_only_fields = fields


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
            'neighborhood_address',
            'profile',
            'resources',
            'expertise_fields',
            'hub', 
        ]
        read_only_fields = fields


class RegisterSerializer(serializers.Serializer):
    full_name = serializers.CharField(max_length=255)
    email = serializers.EmailField()
    password = serializers.CharField(write_only=True, min_length=8)
    confirm_password = serializers.CharField(write_only=True)
    role = serializers.ChoiceField(choices=User.Role.choices)
    hub_id = serializers.IntegerField(required=False, allow_null=True)
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
        category = validated_data.pop('category_id', None)
        if hub_id is not None:
            validated_data['hub_id'] = hub_id
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
