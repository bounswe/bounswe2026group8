from rest_framework.views import APIView
from rest_framework.response import Response
from rest_framework import status
from rest_framework.permissions import AllowAny, IsAuthenticated
from rest_framework_simplejwt.tokens import RefreshToken

from .models import User, Profile, Resource, ExpertiseField
from .models import Hub
from .serializers import (
    RegisterSerializer, LoginSerializer, UserSerializer,
    ProfileSerializer, ResourceSerializer, ExpertiseFieldSerializer,HubSerializer,
) 


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


class ProfileView(APIView):
    """GET and PATCH /profile for viewing/updating the authenticated user's profile object."""
    permission_classes = [IsAuthenticated]

    def get(self, request):
        profile, _ = Profile.objects.get_or_create(user=request.user)
        return Response(ProfileSerializer(profile).data, status=status.HTTP_200_OK)

    def patch(self, request):
        profile, _ = Profile.objects.get_or_create(user=request.user)
        serializer = ProfileSerializer(profile, data=request.data, partial=True)
        if serializer.is_valid():
            serializer.save()
            return Response(serializer.data, status=status.HTTP_200_OK)
        return Response({'message': 'Profile update failed', 'errors': serializer.errors}, status=status.HTTP_400_BAD_REQUEST)


class ResourceListView(APIView):
    """
    GET  /resources  — list all resources for the current user
    POST /resources  — add a resource for the current user
    """
    permission_classes = [IsAuthenticated]

    def get(self, request):
        resources = request.user.resources.all()
        return Response(ResourceSerializer(resources, many=True).data, status=status.HTTP_200_OK)

    def post(self, request):
        serializer = ResourceSerializer(data=request.data)
        if serializer.is_valid():
            serializer.save(user=request.user)
            return Response(serializer.data, status=status.HTTP_201_CREATED)
        return Response({'message': 'Invalid resource data', 'errors': serializer.errors}, status=status.HTTP_400_BAD_REQUEST)


class ResourceDetailView(APIView):
    """
    PATCH  /resources/<id>  — update a resource
    DELETE /resources/<id>  — delete a resource
    """
    permission_classes = [IsAuthenticated]

    def _get_resource(self, request, pk):
        try:
            return request.user.resources.get(pk=pk)
        except Resource.DoesNotExist:
            return None

    def patch(self, request, pk):
        resource = self._get_resource(request, pk)
        if resource is None:
            return Response({'message': 'Resource not found'}, status=status.HTTP_404_NOT_FOUND)
        serializer = ResourceSerializer(resource, data=request.data, partial=True)
        if serializer.is_valid():
            serializer.save()
            return Response(serializer.data, status=status.HTTP_200_OK)
        return Response({'message': 'Invalid resource data', 'errors': serializer.errors}, status=status.HTTP_400_BAD_REQUEST)

    def delete(self, request, pk):
        resource = self._get_resource(request, pk)
        if resource is None:
            return Response({'message': 'Resource not found'}, status=status.HTTP_404_NOT_FOUND)
        resource.delete()
        return Response(status=status.HTTP_204_NO_CONTENT)


class ExpertiseFieldListView(APIView):
    """
    GET  /expertise  — list expertise fields for the current EXPERT user
    POST /expertise  — add an expertise field (EXPERT only)
    """
    permission_classes = [IsAuthenticated]

    def _check_expert(self, request):
        if request.user.role != User.Role.EXPERT:
            return Response({'message': 'Only EXPERT users can manage expertise fields.'}, status=status.HTTP_403_FORBIDDEN)
        return None

    def get(self, request):
        error = self._check_expert(request)
        if error:
            return error
        fields = request.user.expertise_fields.all()
        return Response(ExpertiseFieldSerializer(fields, many=True).data, status=status.HTTP_200_OK)

    def post(self, request):
        error = self._check_expert(request)
        if error:
            return error
        serializer = ExpertiseFieldSerializer(data=request.data)
        if serializer.is_valid():
            serializer.save(user=request.user)
            return Response(serializer.data, status=status.HTTP_201_CREATED)
        return Response({'message': 'Invalid expertise data', 'errors': serializer.errors}, status=status.HTTP_400_BAD_REQUEST)


class ExpertiseFieldDetailView(APIView):
    """
    PATCH  /expertise/<id>  — update expertise field
    DELETE /expertise/<id>  — delete expertise field
    """
    permission_classes = [IsAuthenticated]

    def _get_expertise(self, request, pk):
        if request.user.role != User.Role.EXPERT:
            return None, Response({'message': 'Only EXPERT users can manage expertise fields.'}, status=status.HTTP_403_FORBIDDEN)
        try:
            return request.user.expertise_fields.get(pk=pk), None
        except ExpertiseField.DoesNotExist:
            return None, Response({'message': 'Expertise field not found'}, status=status.HTTP_404_NOT_FOUND)

    def patch(self, request, pk):
        obj, error = self._get_expertise(request, pk)
        if error:
            return error
        serializer = ExpertiseFieldSerializer(obj, data=request.data, partial=True)
        if serializer.is_valid():
            serializer.save()
            return Response(serializer.data, status=status.HTTP_200_OK)
        return Response({'message': 'Invalid expertise data', 'errors': serializer.errors}, status=status.HTTP_400_BAD_REQUEST)

    def delete(self, request, pk):
        obj, error = self._get_expertise(request, pk)
        if error:
            return error
        obj.delete()
        return Response(status=status.HTTP_204_NO_CONTENT)



class HubListView(APIView):
    """
    GET /hubs/
    Public list of all hubs.
    """
    permission_classes = [AllowAny]

    def get(self, request):
        hubs = Hub.objects.all()
        return Response(HubSerializer(hubs, many=True).data, status=status.HTTP_200_OK)
