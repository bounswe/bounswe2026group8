from rest_framework.views import APIView
from rest_framework.response import Response
from rest_framework import status
from rest_framework.permissions import AllowAny, IsAuthenticated
from rest_framework_simplejwt.tokens import RefreshToken

from .models import User, Profile, Resource, ExpertiseField, ExpertiseCategory, UserSettings
from .models import Hub
from .serializers import (
    RegisterSerializer, LoginSerializer, UserSerializer,
    ProfileSerializer, ResourceSerializer, ExpertiseFieldSerializer, HubSerializer,
    ExpertiseCategorySerializer, UserSettingsSerializer, resolve_or_create_hub,
)


class RegisterView(APIView):
    """
    Register a new user account.
    
    POST /register
    
    Create a new user account for either Standard or Expert role.
    
    Request body:
    - full_name (string, required): User's full name
    - email (string, required): Unique email address
    - password (string, required): Strong password
    - confirm_password (string, required): Must match password
    - role (string, required): "STANDARD" or "EXPERT"
    - expertise_field (string, optional): Required if role is "EXPERT"
    - neighborhood_address (string, optional): Required if role is "EXPERT"
    
    Returns: 201 Created with user object and success message
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
    Authenticate user and retrieve JWT tokens.
    
    POST /login
    
    Authenticate with email and password to receive access and refresh tokens.
    Use the returned access token in the Authorization header for subsequent requests.
    
    Request body:
    - email (string, required): User's email address
    - password (string, required): User's password
    
    Returns: 200 OK with access token, refresh token, and user object
    Error: 400 Bad Request if credentials are invalid
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
    Logout the current user.
    
    POST /logout
    
    Invalidate the current session. For JWT-based auth, the client should also
    discard the stored access and refresh tokens.
    
    Authorization: Required (Bearer token)
    
    Returns: 200 OK with confirmation message
    """
    permission_classes = [IsAuthenticated]

    def post(self, request):
        return Response({'message': 'Logged out successfully'}, status=status.HTTP_200_OK)


class MeView(APIView):
    """
    Get or update the currently authenticated user's profile.
    
    GET /me
    Returns the authenticated user's full profile information.
    
    PATCH /me
    Update the user's hub location. Provide either hub_id or country+city.
    Request body:
    - hub_id (integer, optional): ID of an existing hub
    - country (string, optional): Country name
    - city (string, optional): City name
    - district (string, optional): District/suburb name
    
    Authorization: Required (Bearer token)
    
    Returns: 200 OK with updated user profile
    """
    permission_classes = [IsAuthenticated]

    def get(self, request):
        return Response(UserSerializer(request.user).data, status=status.HTTP_200_OK)

    def patch(self, request):
        country = (request.data.get('country') or '').strip()
        city = (request.data.get('city') or '').strip()
        district = (request.data.get('district') or '').strip()
        hub_id = request.data.get('hub_id')

        if country and city:
            hub = resolve_or_create_hub(country, city, district)
            request.user.hub = hub
            request.user.save(update_fields=['hub'])
        elif hub_id is not None:
            hub = Hub.objects.filter(pk=hub_id).first()
            if not hub:
                return Response(
                    {'detail': 'Hub not found.'},
                    status=status.HTTP_400_BAD_REQUEST,
                )
            request.user.hub = hub
            request.user.save(update_fields=['hub'])
        return Response(UserSerializer(request.user).data, status=status.HTTP_200_OK)


class FCMTokenView(APIView):
    """
    Register device FCM token for push notifications.
    
    POST /accounts/fcm-token/
    
    Submit the device's Firebase Cloud Messaging (FCM) token to enable
    receiving push notifications for help requests, forum posts, etc.
    
    Request body:
    - fcm_token (string, required): Firebase device token
    
    Authorization: Required (Bearer token)
    
    Returns: 200 OK with confirmation
    """
    permission_classes = [IsAuthenticated]

    def post(self, request):
        token = request.data.get('fcm_token')
        if not token:
            return Response(
                {'detail': 'fcm_token is required.'},
                status=status.HTTP_400_BAD_REQUEST,
            )
        request.user.fcm_token = token
        request.user.save(update_fields=['fcm_token'])
        return Response({'detail': 'FCM token saved.'}, status=status.HTTP_200_OK)


class ProfileView(APIView):
    """
    Get or update the authenticated user's detailed profile.
    
    GET /profile
    Retrieve detailed profile information (bio, phone, availability, etc.).
    
    PATCH /profile
    Update profile fields. Supports partial updates.
    Request body: Any combination of:
    - bio (string): User's bio/description
    - phone_number (string): Contact phone number
    - availability_status (string): AVAILABLE_TO_HELP, BUSY, etc.
    - emergency_contact (string): Emergency contact name
    - emergency_contact_phone (string): Emergency contact phone
    - blood_type (string): Blood type for medical info
    - has_disability (boolean): Whether user has disabilities
    - special_needs (text): Description of special needs
    
    Authorization: Required (Bearer token)
    
    Returns: 200 OK with profile object
    """
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


class UserSettingsView(APIView):
    """
    Get or update user notification and privacy settings.
    
    GET /settings
    Retrieve notification preferences and profile visibility settings.
    
    PATCH /settings
    Update notification and privacy settings. Supports partial updates.
    Request body: Any combination of:
    - email_notifications_enabled (boolean): Enable email notifications
    - push_notifications_enabled (boolean): Enable push notifications  
    - show_location (boolean): Show location on public profile
    - show_resources (boolean): Show resources on public profile
    - show_expertise (boolean): Show expertise on public profile
    - show_phone_number (boolean): Show phone number on public profile
    - show_emergency_contact (boolean): Show emergency contact on public profile
    - show_medical_info (boolean): Show medical info on public profile
    - show_availability_status (boolean): Show availability status on public profile
    - show_bio (boolean): Show bio on public profile
    
    Authorization: Required (Bearer token)
    
    Returns: 200 OK with settings object
    """
    permission_classes = [IsAuthenticated]

    def get(self, request):
        settings_obj, _ = UserSettings.objects.get_or_create(user=request.user)
        return Response(UserSettingsSerializer(settings_obj).data, status=status.HTTP_200_OK)

    def patch(self, request):
        settings_obj, _ = UserSettings.objects.get_or_create(user=request.user)
        serializer = UserSettingsSerializer(settings_obj, data=request.data, partial=True)
        if serializer.is_valid():
            serializer.save()
            return Response(serializer.data, status=status.HTTP_200_OK)
        return Response(
            {'message': 'Settings update failed', 'errors': serializer.errors},
            status=status.HTTP_400_BAD_REQUEST,
        )


class ResourceListView(APIView):
    """
    Manage user resources (items available to share/lend).
    
    GET /resources
    List all resources owned by the authenticated user.
    
    POST /resources
    Add a new resource. Request body:
    - name (string, required): Resource name (e.g., "First Aid Kit")
    - description (string, optional): Detailed description
    - quantity (integer, optional): Number of items available
    - category (string, optional): Category (e.g., "Medical", "Tools")
    - availability (string, optional): AVAILABLE, NOT_AVAILABLE, etc.
    
    Authorization: Required (Bearer token)
    
    Returns: 200 OK (GET) or 201 Created (POST) with resource list or created resource
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
    Update or delete a specific resource.
    
    PATCH /resources/{id}
    Update a resource (owner only). Supports partial updates.
    
    DELETE /resources/{id}
    Delete a resource (owner only).
    
    Authorization: Required (Bearer token)
    
    Returns: 200 OK (PATCH), 204 No Content (DELETE), or 404 if not found
    Error: 403 Forbidden if not the resource owner
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
    Manage expert user's expertise fields.
    
    GET /expertise
    List all expertise fields for the authenticated expert user.
    EXPERT role required.
    
    POST /expertise
    Add a new expertise field. Always created in PENDING verification state.
    Request body:
    - category_id (integer, required): Expertise category ID
    - certification_level (string, required): BEGINNER or ADVANCED
    - certification_document_url (string, optional): URL to certification document
    - notes (string, optional): Additional notes
    
    Authorization: Required (Bearer token, EXPERT role)
    
    Returns: 200 OK (GET) or 201 Created (POST)
    Error: 403 Forbidden if user is not EXPERT role
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
            # New submissions always start as PENDING regardless of input.
            serializer.save(
                user=request.user,
                verification_status=ExpertiseField.VerificationStatus.PENDING,
                reviewed_by=None,
                reviewed_at=None,
                verification_note='',
            )
            return Response(serializer.data, status=status.HTTP_201_CREATED)
        return Response({'message': 'Invalid expertise data', 'errors': serializer.errors}, status=status.HTTP_400_BAD_REQUEST)


class ExpertiseFieldDetailView(APIView):
    """
    Update or delete an expertise field.
    
    PATCH /expertise/{id}
    Update an expertise field (owner only). Changes to certification content
    will reset verification status to PENDING.
    
    DELETE /expertise/{id}
    Delete an expertise field (owner only).
    
    Authorization: Required (Bearer token, EXPERT role)
    
    Returns: 200 OK (PATCH), 204 No Content (DELETE), or 404 if not found
    Error: 403 Forbidden if not the owner or not EXPERT role
    """
    permission_classes = [IsAuthenticated]

    # Editable fields that re-trigger verification when changed.
    _VERIFICATION_RESET_FIELDS = ('category_id', 'certification_level', 'certification_document_url')

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
        previous_snapshot = {f: getattr(obj, f) for f in self._VERIFICATION_RESET_FIELDS}
        serializer = ExpertiseFieldSerializer(obj, data=request.data, partial=True)
        if serializer.is_valid():
            updated = serializer.save()
            content_changed = any(
                getattr(updated, f) != previous_snapshot[f]
                for f in self._VERIFICATION_RESET_FIELDS
            )
            if content_changed and updated.verification_status != ExpertiseField.VerificationStatus.PENDING:
                updated.verification_status = ExpertiseField.VerificationStatus.PENDING
                updated.reviewed_by = None
                updated.reviewed_at = None
                updated.verification_note = ''
                updated.save(update_fields=[
                    'verification_status',
                    'reviewed_by',
                    'reviewed_at',
                    'verification_note',
                ])
            return Response(ExpertiseFieldSerializer(updated).data, status=status.HTTP_200_OK)
        return Response({'message': 'Invalid expertise data', 'errors': serializer.errors}, status=status.HTTP_400_BAD_REQUEST)

    def delete(self, request, pk):
        obj, error = self._get_expertise(request, pk)
        if error:
            return error
        obj.delete()
        return Response(status=status.HTTP_204_NO_CONTENT)



class UserPublicProfileView(APIView):
    """
    Get a user's public profile.
    
    GET /users/{id}
    
    Retrieve public profile for a specific user. Respects the user's privacy
    settings - certain fields may be hidden based on their preferences.
    
    Authorization: Required (Bearer token)
    
    Parameters:
    - id (integer, path): User ID
    
    Returns: 200 OK with public profile
    Error: 404 Not Found if user doesn't exist
    """
    permission_classes = [IsAuthenticated]

    def get(self, request, pk):
        try:
            user = User.objects.get(pk=pk)
        except User.DoesNotExist:
            return Response({'detail': 'User not found.'}, status=status.HTTP_404_NOT_FOUND)
        settings_obj, _ = UserSettings.objects.get_or_create(user=user)
        data = UserSerializer(user).data

        # Settings are private. They control this public shape, but are not exposed.
        data.pop('settings', None)

        if not settings_obj.show_location:
            data['neighborhood_address'] = None
        if not settings_obj.show_resources:
            data['resources'] = []
        if not settings_obj.show_expertise:
            data['expertise_fields'] = []

        profile = data.get('profile') or {}
        if not settings_obj.show_phone_number:
            profile['phone_number'] = None
        if not settings_obj.show_emergency_contact:
            profile['emergency_contact'] = None
            profile['emergency_contact_phone'] = None
        if not settings_obj.show_medical_info:
            profile['blood_type'] = None
            profile['has_disability'] = False
            profile['special_needs'] = None
        if not settings_obj.show_availability_status:
            profile['availability_status'] = None
        if not settings_obj.show_bio:
            profile['bio'] = None
        data['profile'] = profile

        return Response(data, status=status.HTTP_200_OK)


class HubListView(APIView):
    """
    List all emergency hubs.
    
    GET /hubs
    
    Retrieve list of all registered emergency hubs/neighborhoods.
    Public endpoint - no authentication required.
    
    Returns: 200 OK with list of hubs
    """
    permission_classes = [AllowAny]

    def get(self, request):
        hubs = Hub.objects.all()
        return Response(HubSerializer(hubs, many=True).data, status=status.HTTP_200_OK)


class ExpertiseCategoryListView(APIView):
    """
    List available expertise categories.
    
    GET /expertise-categories
    
    Retrieve list of all active expertise categories that experts can register.
    Public endpoint - no authentication required.
    
    Returns: 200 OK with list of expertise categories
    """
    permission_classes = [AllowAny]

    def get(self, request):
        qs = ExpertiseCategory.objects.filter(is_active=True)
        return Response(ExpertiseCategorySerializer(qs, many=True).data)
