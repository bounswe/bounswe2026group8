"""
Business logic services for authentication.
"""

import jwt
import json
from datetime import datetime, timedelta
from django.contrib.auth.models import User
from django.contrib.auth.hashers import check_password
from django.conf import settings
from typing import Dict, Optional, Tuple


class AuthService:
    """
    Service class for authentication operations.
    Handles credential validation and token generation.
    """

    @staticmethod
    def validate_credentials(email: str, password: str) -> Optional[User]:
        """
        Validate user credentials (email and password).
        
        Args:
            email: User's email address
            password: User's password (plain text)
            
        Returns:
            User object if credentials are valid, None otherwise
        """
        try:
            # Find user by email (email is unique in Django's User model)
            user = User.objects.get(email=email)
            
            # Check if password matches
            if check_password(password, user.password):
                return user
        except User.DoesNotExist:
            pass
        
        return None

    @staticmethod
    def generate_tokens(user: User) -> Tuple[str, str]:
        """
        Generate JWT access and refresh tokens for a user.
        
        Args:
            user: Django User object
            
        Returns:
            Tuple of (access_token, refresh_token)
        """
        now = datetime.utcnow()
        
        # Access token payload
        access_payload = {
            'user_id': user.id,
            'email': user.email,
            'username': user.username,
            'type': 'access',
            'iat': now,
            'exp': now + timedelta(hours=settings.JWT_EXPIRATION_HOURS),
        }
        
        # Refresh token payload (longer expiration)
        refresh_payload = {
            'user_id': user.id,
            'type': 'refresh',
            'iat': now,
            'exp': now + timedelta(days=7),  # 7 days
        }
        
        try:
            access_token = jwt.encode(
                access_payload,
                settings.JWT_SECRET,
                algorithm=settings.JWT_ALGORITHM
            )
            refresh_token = jwt.encode(
                refresh_payload,
                settings.JWT_SECRET,
                algorithm=settings.JWT_ALGORITHM
            )
            return access_token, refresh_token
        except Exception as e:
            raise Exception(f"Token generation failed: {str(e)}")

    @staticmethod
    def decode_token(token: str, token_type: str = 'access') -> Optional[Dict]:
        """
        Decode and validate a JWT token.
        
        Args:
            token: JWT token string
            token_type: Type of token (access or refresh)
            
        Returns:
            Decoded token payload if valid, None otherwise
        """
        try:
            payload = jwt.decode(
                token,
                settings.JWT_SECRET,
                algorithms=[settings.JWT_ALGORITHM]
            )
            
            # Verify token type
            if payload.get('type') != token_type:
                return None
            
            return payload
        except jwt.ExpiredSignatureError:
            return None
        except jwt.InvalidTokenError:
            return None
