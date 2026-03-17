"""
Views for the authentication app.
"""

from rest_framework.views import APIView
from rest_framework.response import Response
from rest_framework import status
from rest_framework.permissions import AllowAny

from .serializers import LoginSerializer, LoginResponseSerializer, UserSerializer
from .services import AuthService


class LoginView(APIView):
    """
    Login endpoint.
    POST /auth/login
    
    Request body:
    {
        "email": "user@example.com",
        "password": "password123"
    }
    
    Response (200):
    {
        "accessToken": "...",
        "refreshToken": "...",
        "user": {
            "id": 1,
            "email": "user@example.com",
            "username": "username"
        }
    }
    
    Response (401):
    {
        "detail": "Invalid email or password"
    }
    """
    permission_classes = [AllowAny]

    def post(self, request):
        """
        Handle POST request for login.
        Validates credentials and returns tokens if valid.
        """
        serializer = LoginSerializer(data=request.data)
        
        if not serializer.is_valid():
            return Response(
                {"detail": serializer.errors},
                status=status.HTTP_400_BAD_REQUEST
            )
        
        email = serializer.validated_data.get('email')
        password = serializer.validated_data.get('password')
        
        # Validate credentials
        user = AuthService.validate_credentials(email, password)
        
        if user is None:
            return Response(
                {"detail": "Invalid email or password"},
                status=status.HTTP_401_UNAUTHORIZED
            )
        
        # Generate tokens
        try:
            access_token, refresh_token = AuthService.generate_tokens(user)
        except Exception as e:
            return Response(
                {"detail": f"Token generation failed: {str(e)}"},
                status=status.HTTP_500_INTERNAL_SERVER_ERROR
            )
        
        # Prepare response
        response_data = {
            "accessToken": access_token,
            "refreshToken": refresh_token,
            "user": {
                "id": user.id,
                "email": user.email,
                "username": user.username
            }
        }
        
        return Response(response_data, status=status.HTTP_200_OK)
