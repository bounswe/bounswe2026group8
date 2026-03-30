from rest_framework import serializers
from django.contrib.auth import authenticate

from .models import User, Profile, Resource, ExpertiseField


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


class ExpertiseFieldSerializer(serializers.ModelSerializer):
    """Serializer for expert-only expertise entries."""

    class Meta:
        model = ExpertiseField
        fields = ['id', 'field', 'certification_level', 'certification_document_url', 'created_at', 'updated_at']
        read_only_fields = ['id', 'created_at', 'updated_at']


class UserSerializer(serializers.ModelSerializer):
    """Read serializer for returning user data in API responses."""

    profile = ProfileSerializer(read_only=True)
    resources = ResourceSerializer(many=True, read_only=True)
    expertise_fields = ExpertiseFieldSerializer(many=True, read_only=True)

    class Meta:
        model = User
        fields = [
            'id',
            'full_name',
            'email',
            'role',
            'neighborhood_address',
            'expertise_field',
            'profile',
            'resources',
            'expertise_fields',
        ]
        read_only_fields = fields


class RegisterSerializer(serializers.Serializer):
    full_name = serializers.CharField(max_length=255)
    email = serializers.EmailField()
    password = serializers.CharField(write_only=True, min_length=8)
    confirm_password = serializers.CharField(write_only=True)
    role = serializers.ChoiceField(choices=User.Role.choices)
    neighborhood_address = serializers.CharField(
        max_length=255, required=False, allow_blank=True, allow_null=True
    )
    expertise_field = serializers.CharField(
        max_length=255, required=False, allow_blank=True, allow_null=True
    )

    def validate_email(self, value):
        if User.objects.filter(email__iexact=value).exists():
            raise serializers.ValidationError('A user with this email already exists.')
        return value.lower()

    def validate(self, data):
        # Password match check
        if data['password'] != data['confirm_password']:
            raise serializers.ValidationError({'confirm_password': ['Passwords do not match.']})

        # Experts must supply their expertise field
        if data['role'] == User.Role.EXPERT:
            expertise = data.get('expertise_field', '').strip() if data.get('expertise_field') else ''
            if not expertise:
                raise serializers.ValidationError(
                    {'expertise_field': ['Expertise field is required for Expert users.']}
                )

        # Standard users: silently ignore expertise_field
        if data['role'] == User.Role.STANDARD:
            data['expertise_field'] = None

        return data

    def create(self, validated_data):
        validated_data.pop('confirm_password')
        password = validated_data.pop('password')
        user = User.objects.create_user(
            email=validated_data.pop('email'),
            full_name=validated_data.pop('full_name'),
            password=password,
            **validated_data,
        )
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
