"""
Automated tests for the auth API.

Run with:
    python manage.py test accounts
"""

from django.test import TestCase
from django.urls import reverse
from rest_framework.test import APIClient
from rest_framework import status
from rest_framework.authtoken.models import Token

from .models import User


class RegisterTests(TestCase):
    def setUp(self):
        self.client = APIClient()
        self.url = '/register'

    def _base_payload(self, **overrides):
        payload = {
            'full_name': 'Test User',
            'email': 'test@example.com',
            'password': 'StrongPass123',
            'confirm_password': 'StrongPass123',
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
        self.assertIsNone(response.data['user']['expertise_field'])

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
        self.assertEqual(response.data['user']['expertise_field'], 'Medical Doctor')
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
        self.assertNotEqual(user.password, 'StrongPass123')
        self.assertTrue(user.password.startswith('pbkdf2_'))


class LoginTests(TestCase):
    def setUp(self):
        self.client = APIClient()
        self.url = '/login'
        self.user = User.objects.create_user(
            email='sheila@example.com',
            full_name='Sheila Davis',
            password='StrongPass123',
            role='EXPERT',
            expertise_field='Medical Doctor',
        )

    def test_login_valid_credentials(self):
        """Valid credentials return a token and user data."""
        response = self.client.post(
            self.url,
            {'email': 'sheila@example.com', 'password': 'StrongPass123'},
            format='json',
        )
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(response.data['message'], 'Login successful')
        self.assertIn('token', response.data)
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
            {'email': 'nobody@example.com', 'password': 'StrongPass123'},
            format='json',
        )
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)

    def test_login_email_case_insensitive(self):
        """Login should work regardless of email casing."""
        response = self.client.post(
            self.url,
            {'email': 'SHEILA@EXAMPLE.COM', 'password': 'StrongPass123'},
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
            password='StrongPass123',
            role='EXPERT',
            expertise_field='Medical Doctor',
            neighborhood_address='Sariyer, Istanbul',
        )
        self.token = Token.objects.create(user=self.user)

    def test_me_with_valid_token(self):
        """Authenticated user gets their profile data."""
        self.client.credentials(HTTP_AUTHORIZATION=f'Token {self.token.key}')
        response = self.client.get(self.url)
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(response.data['email'], 'sheila@example.com')
        self.assertEqual(response.data['full_name'], 'Sheila Davis')
        self.assertEqual(response.data['role'], 'EXPERT')
        self.assertEqual(response.data['expertise_field'], 'Medical Doctor')
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
            password='StrongPass123',
        )
        self.token = Token.objects.create(user=self.user)

    def test_logout_invalidates_token(self):
        """Logging out deletes the auth token so it can no longer be used."""
        self.client.credentials(HTTP_AUTHORIZATION=f'Token {self.token.key}')
        response = self.client.post(self.url)
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(response.data['message'], 'Logged out successfully')
        # Token must no longer exist
        self.assertFalse(Token.objects.filter(key=self.token.key).exists())

    def test_logout_without_token_returns_401(self):
        """Unauthenticated logout attempt returns 401."""
        response = self.client.post(self.url)
        self.assertEqual(response.status_code, status.HTTP_401_UNAUTHORIZED)

    def test_me_after_logout_returns_401(self):
        """After logout, the former token no longer grants /me access."""
        self.client.credentials(HTTP_AUTHORIZATION=f'Token {self.token.key}')
        self.client.post(self.url)
        response = self.client.get('/me')
        self.assertEqual(response.status_code, status.HTTP_401_UNAUTHORIZED)
