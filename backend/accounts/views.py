from rest_framework.views import APIView
from rest_framework.response import Response
from rest_framework import status
from rest_framework.permissions import AllowAny, IsAuthenticated
from rest_framework.authtoken.models import Token

from .models import Hub
from .serializers import HubSerializer, RegisterSerializer, LoginSerializer, UserSerializer


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
    Returns an auth token on success.
    """
    permission_classes = [AllowAny]

    def post(self, request):
        serializer = LoginSerializer(data=request.data)
        if serializer.is_valid():
            user = serializer.validated_data['user']
            # Get or create a DRF token for the user
            token, _ = Token.objects.get_or_create(user=user)
            return Response(
                {
                    'message': 'Login successful',
                    'token': token.key,
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
    Requires a valid token. Deletes the token so it can no longer be used.
    """
    permission_classes = [IsAuthenticated]

    def post(self, request):
        # Delete the user's auth token to invalidate the session
        try:
            request.user.auth_token.delete()
        except Token.DoesNotExist:
            pass
        return Response({'message': 'Logged out successfully'}, status=status.HTTP_200_OK)


class MeView(APIView):
    """
    GET /me
    Returns the currently authenticated user's profile.
    """
    permission_classes = [IsAuthenticated]

    def get(self, request):
        return Response(UserSerializer(request.user).data, status=status.HTTP_200_OK)


class HubListView(APIView):
    """
    GET /hubs/
    Public list of all hubs.
    """
    permission_classes = [AllowAny]

    def get(self, request):
        hubs = Hub.objects.all()
        return Response(HubSerializer(hubs, many=True).data, status=status.HTTP_200_OK)
