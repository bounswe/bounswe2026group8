"""
Serializers for the authentication app.
"""

from rest_framework import serializers
from django.contrib.auth.models import User


class LoginSerializer(serializers.Serializer):
    """
    Serializer for login endpoint.
    Validates email and password credentials.
    """
    email = serializers.EmailField()
    password = serializers.CharField(write_only=True)

    def validate(self, data):
        """
        Validate that email and password are provided.
        """
        if not data.get('email'):
            raise serializers.ValidationError("Email is required.")
        if not data.get('password'):
            raise serializers.ValidationError("Password is required.")
        return data


class UserSerializer(serializers.ModelSerializer):
    """
    Serializer for User representation in responses.
    """
    class Meta:
        model = User
        fields = ['id', 'email', 'username']
        read_only_fields = ['id', 'email', 'username']


class LoginResponseSerializer(serializers.Serializer):
    """
    Serializer for login response.
    """
    accessToken = serializers.CharField()
    refreshToken = serializers.CharField()
    user = UserSerializer()
