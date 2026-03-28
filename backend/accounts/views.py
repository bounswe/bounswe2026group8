from rest_framework.views import APIView
from rest_framework.response import Response
from rest_framework import status
from rest_framework.permissions import AllowAny, IsAuthenticated
from rest_framework_simplejwt.tokens import RefreshToken

from .serializers import RegisterSerializer, LoginSerializer, UserSerializer


class RegisterView(APIView):
    """
    POST /register
    Open to unauthenticated requests.
    """
    permission_classes = [AllowAny]

    def post(self, request):
        serializer = RegisterSerializer(data=request.data)
        if serializer.is_valid():
            user = serializer.save()
            return Response(
                {
                    'message': 'Account created successfully',
                    'user': UserSerializer(user).data,
                },
                status=status.HTTP_201_CREATED,
            )
        return Response(
            {'message': 'Registration failed', 'errors': serializer.errors},
            status=status.HTTP_400_BAD_REQUEST,
        )


class LoginView(APIView):
    """
    POST /login
    Returns JWT tokens on success.
    """
    permission_classes = [AllowAny]

    def post(self, request):
        serializer = LoginSerializer(data=request.data)
        if serializer.is_valid():
            user = serializer.validated_data['user']
            # Generate JWT token pair
            refresh = RefreshToken.for_user(user)
            return Response(
                {
                    'message': 'Login successful',
                    'token': str(refresh.access_token),
                    'refresh': str(refresh),
                    'user': UserSerializer(user).data,
                },
                status=status.HTTP_200_OK,
            )
        return Response(
            {'message': 'Invalid email or password'},
            status=status.HTTP_400_BAD_REQUEST,
        )


class LogoutView(APIView):
    """
    POST /logout
    For JWT, logout is handled client-side by discarding the token.
    This endpoint exists for API compatibility.
    """
    permission_classes = [IsAuthenticated]

    def post(self, request):
        return Response({'message': 'Logged out successfully'}, status=status.HTTP_200_OK)


class MeView(APIView):
    """
    GET /me
    Returns the currently authenticated user's profile.
    """
    permission_classes = [IsAuthenticated]

    def get(self, request):
        return Response(UserSerializer(request.user).data, status=status.HTTP_200_OK)
