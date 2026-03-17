"""
Tests for authentication login endpoint.
"""

from django.test import TestCase
from django.contrib.auth.models import User
from rest_framework.test import APIClient
from rest_framework import status
import json


class LoginViewTests(TestCase):
    """
    Test cases for the login endpoint.
    """

    def setUp(self):
        """
        Set up test fixtures.
        """
        self.client = APIClient()
        self.login_url = '/auth/login'
        
        # Create a test user
        self.test_user_email = 'testuser@example.com'
        self.test_user_password = 'testpass123'
        self.test_user = User.objects.create_user(
            username='testuser',
            email=self.test_user_email,
            password=self.test_user_password
        )

    def test_login_with_valid_credentials(self):
        """
        Test login with valid email and password.
        """
        response = self.client.post(
            self.login_url,
            {
                'email': self.test_user_email,
                'password': self.test_user_password
            },
            format='json'
        )
        
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        data = response.json()
        
        # Check response structure
        self.assertIn('accessToken', data)
        self.assertIn('refreshToken', data)
        self.assertIn('user', data)
        
        # Check user data
        self.assertEqual(data['user']['email'], self.test_user_email)
        self.assertEqual(data['user']['username'], 'testuser')
        self.assertIn('id', data['user'])

    def test_login_with_invalid_password(self):
        """
        Test login with correct email but wrong password.
        """
        response = self.client.post(
            self.login_url,
            {
                'email': self.test_user_email,
                'password': 'wrongpassword'
            },
            format='json'
        )
        
        self.assertEqual(response.status_code, status.HTTP_401_UNAUTHORIZED)
        data = response.json()
        self.assertIn('detail', data)

    def test_login_with_invalid_email(self):
        """
        Test login with non-existent email.
        """
        response = self.client.post(
            self.login_url,
            {
                'email': 'nonexistent@example.com',
                'password': self.test_user_password
            },
            format='json'
        )
        
        self.assertEqual(response.status_code, status.HTTP_401_UNAUTHORIZED)

    def test_login_with_missing_email(self):
        """
        Test login without providing email.
        """
        response = self.client.post(
            self.login_url,
            {'password': self.test_user_password},
            format='json'
        )
        
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)

    def test_login_with_missing_password(self):
        """
        Test login without providing password.
        """
        response = self.client.post(
            self.login_url,
            {'email': self.test_user_email},
            format='json'
        )
        
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)

    def test_login_with_empty_payload(self):
        """
        Test login with empty payload.
        """
        response = self.client.post(
            self.login_url,
            {},
            format='json'
        )
        
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)
