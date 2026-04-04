from rest_framework import serializers
from django.contrib.auth import authenticate
from django.contrib.auth.password_validation import validate_password
from django.core.exceptions import ValidationError as DjangoValidationError

from .models import Hub, User


class HubSerializer(serializers.ModelSerializer):
    class Meta:
        model = Hub
        fields = ['id', 'name', 'slug']
        read_only_fields = fields


class UserSerializer(serializers.ModelSerializer):
    """Read-only serializer for returning user data in API responses."""
    hub = HubSerializer(read_only=True)

    class Meta:
        model = User
        fields = [
            'id', 'full_name', 'email', 'role',
            'hub', 'neighborhood_address', 'expertise_field',
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
    expertise_field = serializers.CharField(
        max_length=255, required=False, allow_blank=True, allow_null=True
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

        # Validate password against Django's password validators
        try:
            validate_password(data['password'])
        except DjangoValidationError as e:
            raise serializers.ValidationError({'password': e.messages})

        if data['role'] == User.Role.EXPERT:
            expertise = data.get('expertise_field', '').strip() if data.get('expertise_field') else ''
            if not expertise:
                raise serializers.ValidationError(
                    {'expertise_field': ['Expertise field is required for Expert users.']}
                )

        if data['role'] == User.Role.STANDARD:
            data['expertise_field'] = None

        return data

    def create(self, validated_data):
        validated_data.pop('confirm_password')
        password = validated_data.pop('password')
        hub_id = validated_data.pop('hub_id', None)
        if hub_id is not None:
            validated_data['hub_id'] = hub_id
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
