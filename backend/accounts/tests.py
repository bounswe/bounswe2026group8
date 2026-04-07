"""
Automated tests for the auth API.

Run with:
    python manage.py test accounts
"""

from django.test import TestCase
from django.urls import reverse
from rest_framework.test import APIClient
from rest_framework import status
from rest_framework_simplejwt.tokens import RefreshToken

from .models import Hub, User, Resource, ExpertiseField


class RegisterTests(TestCase):
    def setUp(self):
        self.client = APIClient()
        self.url = '/register'

    def _base_payload(self, **overrides):
        payload = {
            'full_name': 'Test User',
            'email': 'test@example.com',
            'password': 'StrongPass123!',
            'confirm_password': 'StrongPass123!',
            'role': 'STANDARD',
        }
        payload.update(overrides)
        return payload

    # ── Happy paths ────────────────────────────────────────────────────────────

    def test_register_standard_user(self):
        """Standard user can register without expertise_field."""
        response = self.client.post(self.url, self._base_payload(), format='json')
        self.assertEqual(response.status_code, status.HTTP_201_CREATED)
        self.assertEqual(response.data['message'], 'Account created successfully')
        self.assertEqual(response.data['user']['role'], 'STANDARD')
        self.assertEqual(response.data['user']['expertise_fields'], [])

    def test_register_expert_user(self):
        """Expert user can register with an expertise_field."""
        payload = self._base_payload(
            role='EXPERT',
            expertise_field='Medical Doctor',
            neighborhood_address='Sariyer, Istanbul',
        )
        response = self.client.post(self.url, payload, format='json')
        self.assertEqual(response.status_code, status.HTTP_201_CREATED)
        self.assertEqual(response.data['user']['role'], 'EXPERT')
        self.assertEqual(response.data['user']['neighborhood_address'], 'Sariyer, Istanbul')

    def test_register_standard_user_without_optional_fields(self):
        """Neighborhood and expertise fields should be optional for standard users."""
        response = self.client.post(self.url, self._base_payload(), format='json')
        self.assertEqual(response.status_code, status.HTTP_201_CREATED)

    # ── Validation failures ────────────────────────────────────────────────────

    def test_register_expert_without_expertise_field_rejected(self):
        """Expert users must provide an expertise_field."""
        payload = self._base_payload(role='EXPERT')
        response = self.client.post(self.url, payload, format='json')
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)
        self.assertIn('expertise_field', response.data['errors'])

    def test_register_duplicate_email_rejected(self):
        """Registering with an already-used email is rejected."""
        self.client.post(self.url, self._base_payload(), format='json')
        response = self.client.post(self.url, self._base_payload(), format='json')
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)
        self.assertIn('email', response.data['errors'])

    def test_register_password_mismatch_rejected(self):
        """Mismatched password and confirm_password is rejected."""
        payload = self._base_payload(confirm_password='WrongPassword')
        response = self.client.post(self.url, payload, format='json')
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)
        self.assertIn('confirm_password', response.data['errors'])

    def test_register_invalid_role_rejected(self):
        """An invalid role value is rejected."""
        payload = self._base_payload(role='ADMIN')
        response = self.client.post(self.url, payload, format='json')
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)

    def test_register_missing_required_fields(self):
        """Submitting an empty body is rejected."""
        response = self.client.post(self.url, {}, format='json')
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)

    def test_password_is_hashed(self):
        """Passwords must never be stored in plaintext."""
        self.client.post(self.url, self._base_payload(), format='json')
        user = User.objects.get(email='test@example.com')
        self.assertNotEqual(user.password, 'StrongPass123!')
        self.assertTrue(user.password.startswith('pbkdf2_'))


class LoginTests(TestCase):
    def setUp(self):
        self.client = APIClient()
        self.url = '/login'
        self.user = User.objects.create_user(
            email='sheila@example.com',
            full_name='Sheila Davis',
            password='StrongPass123!',
            role='EXPERT',
            expertise_field='Medical Doctor',
        )

    def test_login_valid_credentials(self):
        """Valid credentials return JWT tokens and user data."""
        response = self.client.post(
            self.url,
            {'email': 'sheila@example.com', 'password': 'StrongPass123!'},
            format='json',
        )
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(response.data['message'], 'Login successful')
        self.assertIn('token', response.data)
        self.assertIn('refresh', response.data)
        self.assertEqual(response.data['user']['email'], 'sheila@example.com')
        self.assertEqual(response.data['user']['role'], 'EXPERT')

    def test_login_wrong_password(self):
        """Wrong password returns 400 with appropriate message."""
        response = self.client.post(
            self.url,
            {'email': 'sheila@example.com', 'password': 'WrongPassword'},
            format='json',
        )
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)
        self.assertEqual(response.data['message'], 'Invalid email or password')

    def test_login_nonexistent_email(self):
        """Non-existent email returns 400."""
        response = self.client.post(
            self.url,
            {'email': 'nobody@example.com', 'password': 'StrongPass123!'},
            format='json',
        )
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)

    def test_login_email_case_insensitive(self):
        """Login should work regardless of email casing."""
        response = self.client.post(
            self.url,
            {'email': 'SHEILA@EXAMPLE.COM', 'password': 'StrongPass123!'},
            format='json',
        )
        self.assertEqual(response.status_code, status.HTTP_200_OK)


class MeTests(TestCase):
    def setUp(self):
        self.client = APIClient()
        self.url = '/me'
        self.user = User.objects.create_user(
            email='sheila@example.com',
            full_name='Sheila Davis',
            password='StrongPass123!',
            role='EXPERT',
            expertise_field='Medical Doctor',
            neighborhood_address='Sariyer, Istanbul',
        )
        # Generate a JWT access token for the user
        refresh = RefreshToken.for_user(self.user)
        self.access_token = str(refresh.access_token)

    def test_me_with_valid_token(self):
        """Authenticated user gets their profile data."""
        self.client.credentials(HTTP_AUTHORIZATION=f'Bearer {self.access_token}')
        response = self.client.get(self.url)
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(response.data['email'], 'sheila@example.com')
        self.assertEqual(response.data['full_name'], 'Sheila Davis')
        self.assertEqual(response.data['role'], 'EXPERT')
        self.assertEqual(response.data['neighborhood_address'], 'Sariyer, Istanbul')

    def test_me_without_token_returns_401(self):
        """Unauthenticated request to /me returns 401."""
        response = self.client.get(self.url)
        self.assertEqual(response.status_code, status.HTTP_401_UNAUTHORIZED)


class LogoutTests(TestCase):
    def setUp(self):
        self.client = APIClient()
        self.url = '/logout'
        self.user = User.objects.create_user(
            email='sheila@example.com',
            full_name='Sheila Davis',
            password='StrongPass123!',
        )
        # Generate a JWT access token for the user
        refresh = RefreshToken.for_user(self.user)
        self.access_token = str(refresh.access_token)

    def test_logout_returns_success(self):
        """Logging out returns a success message."""
        self.client.credentials(HTTP_AUTHORIZATION=f'Bearer {self.access_token}')
        response = self.client.post(self.url)
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(response.data['message'], 'Logged out successfully')

    def test_logout_without_token_returns_401(self):
        """Unauthenticated logout attempt returns 401."""
        response = self.client.post(self.url)
        self.assertEqual(response.status_code, status.HTTP_401_UNAUTHORIZED)


class HubTests(TestCase):
    def setUp(self):
        self.client = APIClient()
        self.hub, _ = Hub.objects.get_or_create(name='Istanbul', defaults={'slug': 'istanbul'})

    def test_list_hubs_public(self):
        """Anyone can list hubs without authentication."""
        response = self.client.get('/hubs/')
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertTrue(len(response.data) >= 1)

    def test_hub_has_name_and_slug(self):
        """Hub list returns name and slug fields."""
        response = self.client.get('/hubs/')
        hub = next(h for h in response.data if h['slug'] == 'istanbul')
        self.assertEqual(hub['name'], 'Istanbul')


class MePatchTests(TestCase):
    def setUp(self):
        self.client = APIClient()
        self.hub, _ = Hub.objects.get_or_create(name='Istanbul', defaults={'slug': 'istanbul'})
        self.other_hub, _ = Hub.objects.get_or_create(name='Ankara', defaults={'slug': 'ankara'})
        self.user = User.objects.create_user(
            email='user@example.com',
            full_name='Test User',
            password='StrongPass123!',
            hub=self.hub,
        )
        refresh = RefreshToken.for_user(self.user)
        self.client.credentials(HTTP_AUTHORIZATION=f'Bearer {str(refresh.access_token)}')

    def test_patch_me_updates_hub(self):
        """PATCH /me can update the user's hub."""
        response = self.client.patch('/me', {'hub_id': self.other_hub.pk}, format='json')
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.user.refresh_from_db()
        self.assertEqual(self.user.hub, self.other_hub)

    def test_patch_me_unauthenticated_returns_401(self):
        """Unauthenticated PATCH /me returns 401."""
        anon = APIClient()
        response = anon.patch('/me', {'hub': self.other_hub.pk}, format='json')
        self.assertEqual(response.status_code, status.HTTP_401_UNAUTHORIZED)


class ProfileTests(TestCase):
    def setUp(self):
        self.client = APIClient()
        self.user = User.objects.create_user(
            email='user@example.com',
            full_name='Test User',
            password='StrongPass123!',
        )
        refresh = RefreshToken.for_user(self.user)
        self.client.credentials(HTTP_AUTHORIZATION=f'Bearer {str(refresh.access_token)}')

    def test_get_profile(self):
        """GET /profile returns the user's extended profile."""
        response = self.client.get('/profile')
        self.assertEqual(response.status_code, status.HTTP_200_OK)

    def test_patch_profile_updates_bio(self):
        """PATCH /profile can update bio."""
        response = self.client.patch('/profile', {'bio': 'Hello world'}, format='json')
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(response.data['bio'], 'Hello world')

    def test_get_profile_unauthenticated_returns_401(self):
        """Unauthenticated GET /profile returns 401."""
        anon = APIClient()
        response = anon.get('/profile')
        self.assertEqual(response.status_code, status.HTTP_401_UNAUTHORIZED)


class UserPublicProfileTests(TestCase):
    def setUp(self):
        self.client = APIClient()
        self.user = User.objects.create_user(
            email='user@example.com',
            full_name='Test User',
            password='StrongPass123!',
        )
        self.other = User.objects.create_user(
            email='other@example.com',
            full_name='Other User',
            password='StrongPass123!',
        )
        refresh = RefreshToken.for_user(self.user)
        self.client.credentials(HTTP_AUTHORIZATION=f'Bearer {str(refresh.access_token)}')

    def test_get_public_profile(self):
        """GET /users/<id>/ returns the public profile of another user."""
        response = self.client.get(f'/users/{self.other.pk}/')
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(response.data['full_name'], 'Other User')

    def test_get_public_profile_unauthenticated_returns_401(self):
        """Unauthenticated request to /users/<id>/ returns 401."""
        anon = APIClient()
        response = anon.get(f'/users/{self.other.pk}/')
        self.assertEqual(response.status_code, status.HTTP_401_UNAUTHORIZED)

    def test_get_nonexistent_user_returns_404(self):
        """Requesting a non-existent user returns 404."""
        response = self.client.get('/users/99999/')
        self.assertEqual(response.status_code, status.HTTP_404_NOT_FOUND)


class ResourceTests(TestCase):
    """Tests for GET/POST /resources and DELETE /resources/<pk>."""

    def setUp(self):
        self.client = APIClient()
        self.user = User.objects.create_user(
            email='user@example.com',
            full_name='Test User',
            password='StrongPass123!',
        )
        refresh = RefreshToken.for_user(self.user)
        self.client.credentials(HTTP_AUTHORIZATION=f'Bearer {str(refresh.access_token)}')

    def _create_payload(self, **overrides):
        payload = {
            'name': 'First Aid Kit',
            'category': 'MEDICAL',
            'quantity': 2,
            'condition': True,
        }
        payload.update(overrides)
        return payload

    # ── Happy paths ────────────────────────────────────────────────────────────

    def test_create_resource(self):
        """Authenticated user can create a resource."""
        response = self.client.post('/resources', self._create_payload(), format='json')
        self.assertEqual(response.status_code, status.HTTP_201_CREATED)
        self.assertEqual(response.data['name'], 'First Aid Kit')
        self.assertEqual(response.data['category'], 'MEDICAL')

    def test_list_resources_returns_own_resources(self):
        """GET /resources returns only the authenticated user's resources."""
        Resource.objects.create(
            user=self.user, name='Water', category='FOOD', quantity=10, condition=True
        )
        # Another user's resource should not appear
        other = User.objects.create_user(
            email='other@example.com', full_name='Other', password='Pass123!'
        )
        Resource.objects.create(
            user=other, name='Tent', category='SHELTER', quantity=1, condition=True
        )

        response = self.client.get('/resources')
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        names = [r['name'] for r in response.data]
        self.assertIn('Water', names)
        self.assertNotIn('Tent', names)

    def test_delete_resource(self):
        """User can delete their own resource."""
        resource = Resource.objects.create(
            user=self.user, name='Blanket', category='SHELTER', quantity=3, condition=True
        )
        response = self.client.delete(f'/resources/{resource.pk}')
        self.assertEqual(response.status_code, status.HTTP_204_NO_CONTENT)
        self.assertFalse(Resource.objects.filter(pk=resource.pk).exists())

    # ── Validation failures ────────────────────────────────────────────────────

    def test_create_resource_missing_name_rejected(self):
        """Resource without a name is rejected."""
        payload = self._create_payload()
        del payload['name']
        response = self.client.post('/resources', payload, format='json')
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)

    def test_create_resource_missing_category_rejected(self):
        """Resource without a category is rejected."""
        payload = self._create_payload()
        del payload['category']
        response = self.client.post('/resources', payload, format='json')
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)

    # ── Authorization ──────────────────────────────────────────────────────────

    def test_create_resource_unauthenticated_returns_401(self):
        """Unauthenticated request to create resource returns 401."""
        anon = APIClient()
        response = anon.post('/resources', self._create_payload(), format='json')
        self.assertEqual(response.status_code, status.HTTP_401_UNAUTHORIZED)

    def test_delete_other_users_resource_returns_403(self):
        """User cannot delete another user's resource."""
        other = User.objects.create_user(
            email='other@example.com', full_name='Other', password='Pass123!'
        )
        resource = Resource.objects.create(
            user=other, name='Tent', category='SHELTER', quantity=1, condition=True
        )
        response = self.client.delete(f'/resources/{resource.pk}')
        self.assertIn(response.status_code, [status.HTTP_403_FORBIDDEN, status.HTTP_404_NOT_FOUND])


class ExpertiseFieldTests(TestCase):
    """Tests for GET/POST /expertise and role-based access control."""

    def setUp(self):
        self.client = APIClient()
        self.expert = User.objects.create_user(
            email='expert@example.com',
            full_name='Expert User',
            password='StrongPass123!',
            role='EXPERT',
            expertise_field='Medical Doctor',
        )
        self.standard = User.objects.create_user(
            email='standard@example.com',
            full_name='Standard User',
            password='StrongPass123!',
            role='STANDARD',
        )
        expert_refresh = RefreshToken.for_user(self.expert)
        self.expert_client = APIClient()
        self.expert_client.credentials(
            HTTP_AUTHORIZATION=f'Bearer {str(expert_refresh.access_token)}'
        )
        standard_refresh = RefreshToken.for_user(self.standard)
        self.standard_client = APIClient()
        self.standard_client.credentials(
            HTTP_AUTHORIZATION=f'Bearer {str(standard_refresh.access_token)}'
        )

    def _create_payload(self, **overrides):
        payload = {
            'field': 'Cardiology',
            'certification_level': 'ADVANCED',
        }
        payload.update(overrides)
        return payload

    # ── Expert user happy paths ────────────────────────────────────────────────

    def test_expert_can_create_expertise_field(self):
        """Expert user can add an expertise field."""
        response = self.expert_client.post('/expertise', self._create_payload(), format='json')
        self.assertEqual(response.status_code, status.HTTP_201_CREATED)
        self.assertEqual(response.data['field'], 'Cardiology')
        self.assertEqual(response.data['certification_level'], 'ADVANCED')

    def test_expert_can_list_own_expertise_fields(self):
        """Expert user can list their own expertise fields."""
        ExpertiseField.objects.create(
            user=self.expert, field='Neurology', certification_level='BEGINNER'
        )
        response = self.expert_client.get('/expertise')
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        fields = [e['field'] for e in response.data]
        self.assertIn('Neurology', fields)

    def test_expert_can_delete_expertise_field(self):
        """Expert user can delete their own expertise field."""
        ef = ExpertiseField.objects.create(
            user=self.expert, field='Radiology', certification_level='BEGINNER'
        )
        response = self.expert_client.delete(f'/expertise/{ef.pk}')
        self.assertEqual(response.status_code, status.HTTP_204_NO_CONTENT)
        self.assertFalse(ExpertiseField.objects.filter(pk=ef.pk).exists())

    def test_expertise_defaults_certification_level_to_beginner(self):
        """Omitting certification_level defaults to BEGINNER."""
        payload = {'field': 'Surgery'}
        response = self.expert_client.post('/expertise', payload, format='json')
        self.assertEqual(response.status_code, status.HTTP_201_CREATED)
        self.assertEqual(response.data['certification_level'], 'BEGINNER')

    # ── Standard user restrictions ─────────────────────────────────────────────

    def test_standard_user_cannot_create_expertise_field(self):
        """Standard user is forbidden from creating expertise fields."""
        response = self.standard_client.post('/expertise', self._create_payload(), format='json')
        self.assertIn(response.status_code, [status.HTTP_403_FORBIDDEN, status.HTTP_400_BAD_REQUEST])

    # ── Authorization ──────────────────────────────────────────────────────────

    def test_create_expertise_unauthenticated_returns_401(self):
        """Unauthenticated request to create expertise returns 401."""
        anon = APIClient()
        response = anon.post('/expertise', self._create_payload(), format='json')
        self.assertEqual(response.status_code, status.HTTP_401_UNAUTHORIZED)

    def test_expertise_fields_not_visible_across_users(self):
        """Expert can only see their own expertise fields, not other users'."""
        other_expert = User.objects.create_user(
            email='other_expert@example.com',
            full_name='Other Expert',
            password='Pass123!',
            role='EXPERT',
            expertise_field='Nurse',
        )
        ExpertiseField.objects.create(
            user=other_expert, field='Oncology', certification_level='ADVANCED'
        )
        response = self.expert_client.get('/expertise')
        fields = [e['field'] for e in response.data]
        self.assertNotIn('Oncology', fields)
