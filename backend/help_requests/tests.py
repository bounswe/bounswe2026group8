"""
Tests for the help_requests app.

Covers all acceptance criteria from issues #120-#124:
  - Help request CRUD with permission enforcement
  - Comment list/create with comment_count sync
  - Auto status update to EXPERT_RESPONDING on expert comment
  - Status not overwritten when already RESOLVED
  - Help offer CRUD with permission checks
  - Unauthenticated access returns 401
  - FCM notifications sent to hub experts on help request creation
  - Image upload endpoint validation and auth
"""

from unittest.mock import patch, MagicMock

from django.core.files.uploadedfile import SimpleUploadedFile

from django.test import TestCase
from rest_framework.test import APIClient
from rest_framework import status
from rest_framework_simplejwt.tokens import RefreshToken

from accounts.models import Hub, User, ExpertiseCategory, ExpertiseField
from .models import HelpRequest, HelpComment, HelpOffer


class HelpTestBase(TestCase):
    """
    Shared setup for all help_requests tests.

    Creates a hub, a standard user, an expert user, and an anonymous client.
    Each user gets an APIClient pre-configured with their auth token.
    Tests run in isolation — Django wraps each test in a transaction that
    is rolled back at the end, so no state leaks between tests.
    """

    def setUp(self):
        # Use get_or_create because the seed migration (accounts.0003_seed_hubs)
        # may have already created this hub.
        self.hub, _ = Hub.objects.get_or_create(
            name='Istanbul', defaults={'slug': 'istanbul'},
        )

        # Standard user — no special privileges.
        self.standard_user = User.objects.create_user(
            email='standard@example.com', full_name='Standard User',
            password='Pass1234', hub=self.hub, role=User.Role.STANDARD,
        )
        # Expert user — commenting on a request triggers status promotion.
        self.expert_user = User.objects.create_user(
            email='expert@example.com', full_name='Expert User',
            password='Pass1234', hub=self.hub, role=User.Role.EXPERT,
        )

        # Pre-configured API clients with JWT Bearer tokens.
        self.standard_client = APIClient()
        token1 = RefreshToken.for_user(self.standard_user).access_token
        self.standard_client.credentials(HTTP_AUTHORIZATION=f'Bearer {token1}')

        self.expert_client = APIClient()
        token2 = RefreshToken.for_user(self.expert_user).access_token
        self.expert_client.credentials(HTTP_AUTHORIZATION=f'Bearer {token2}')

        # Anonymous client — no auth token, should always get 401.
        self.anon = APIClient()

    def _create_help_request(self, client=None, **overrides):
        """Helper to create a help request with sensible defaults."""
        payload = {
            'hub': self.hub.pk,
            'category': 'MEDICAL',
            'urgency': 'HIGH',
            'title': 'Need medical help',
            'description': 'Someone is injured.',
        }
        payload.update(overrides)
        return (client or self.standard_client).post(
            '/help-requests/', payload, format='json',
        )

    def _create_help_offer(self, client=None, **overrides):
        """Helper to create a help offer with sensible defaults."""
        payload = {
            'hub': self.hub.pk,
            'category': 'MEDICAL',
            'skill_or_resource': 'First Aid',
            'description': 'I am a certified first aider.',
            'availability': 'Weekdays 9-17',
        }
        payload.update(overrides)
        return (client or self.standard_client).post(
            '/help-offers/', payload, format='json',
        )


# ── Help Request CRUD Tests ───────────────────────────────────────────────────

class HelpRequestCRUDTests(HelpTestBase):

    # -- Create --

    def test_create_help_request(self):
        """Authenticated user can create a help request."""
        res = self._create_help_request()
        self.assertEqual(res.status_code, status.HTTP_201_CREATED)
        self.assertEqual(res.data['title'], 'Need medical help')
        self.assertEqual(res.data['category'], 'MEDICAL')
        self.assertEqual(res.data['urgency'], 'HIGH')
        # Author should be set from the token, not from input.
        self.assertEqual(res.data['author']['email'], 'standard@example.com')
        # Status defaults to OPEN on creation.
        self.assertEqual(res.data['status'], 'OPEN')

    def test_create_help_request_unauthenticated(self):
        """Unauthenticated requests return 401."""
        res = self._create_help_request(client=self.anon)
        self.assertEqual(res.status_code, status.HTTP_401_UNAUTHORIZED)

    def test_create_help_request_without_hub(self):
        """Hub is optional — a request can be general."""
        res = self._create_help_request(hub=None)
        self.assertEqual(res.status_code, status.HTTP_201_CREATED)
        self.assertIsNone(res.data['hub'])

    def test_create_help_request_defaults_urgency_to_low(self):
        """If urgency is not provided, it defaults to LOW."""
        payload = {
            'hub': self.hub.pk,
            'category': 'FOOD',
            'title': 'Need food',
            'description': 'Running low on supplies.',
        }
        res = self.standard_client.post('/help-requests/', payload, format='json')
        self.assertEqual(res.status_code, status.HTTP_201_CREATED)
        self.assertEqual(res.data['urgency'], 'LOW')

    # -- List --

    def test_list_help_requests(self):
        """GET /help-requests/ returns all help requests."""
        self._create_help_request()
        self._create_help_request(title='Second request')
        res = self.standard_client.get('/help-requests/')
        self.assertEqual(res.status_code, status.HTTP_200_OK)
        self.assertEqual(len(res.data), 2)

    def test_list_help_requests_unauthenticated(self):
        """Unauthenticated list requests return 401."""
        res = self.anon.get('/help-requests/')
        self.assertEqual(res.status_code, status.HTTP_401_UNAUTHORIZED)

    def test_list_help_requests_filter_by_hub(self):
        """Filtering by hub_id returns only requests for that hub."""
        other_hub, _ = Hub.objects.get_or_create(name='Ankara', defaults={'slug': 'ankara'})
        self._create_help_request()
        self._create_help_request(hub=other_hub.pk, title='Ankara request')
        res = self.standard_client.get(f'/help-requests/?hub_id={self.hub.pk}')
        self.assertEqual(len(res.data), 1)
        self.assertEqual(res.data[0]['title'], 'Need medical help')

    def test_list_help_requests_filter_by_category(self):
        """Filtering by category returns only matching requests."""
        self._create_help_request()
        self._create_help_request(category='FOOD', title='Need food')
        res = self.standard_client.get('/help-requests/?category=FOOD')
        self.assertEqual(len(res.data), 1)
        self.assertEqual(res.data[0]['title'], 'Need food')

    def test_list_help_requests_filter_category_case_insensitive(self):
        """Category filter is case-insensitive (lowercase input works)."""
        self._create_help_request(category='SHELTER')
        res = self.standard_client.get('/help-requests/?category=shelter')
        self.assertEqual(len(res.data), 1)

    # -- Detail --

    def test_get_help_request_detail(self):
        """GET /help-requests/{id}/ returns full detail including location."""
        pk = self._create_help_request(
            latitude='41.015137', longitude='28.979530',
            location_text='Taksim, Istanbul',
        ).data['id']
        res = self.standard_client.get(f'/help-requests/{pk}/')
        self.assertEqual(res.status_code, status.HTTP_200_OK)
        self.assertEqual(res.data['description'], 'Someone is injured.')
        self.assertEqual(res.data['location_text'], 'Taksim, Istanbul')
        self.assertIn('latitude', res.data)
        self.assertIn('longitude', res.data)
        self.assertIn('updated_at', res.data)

    def test_get_help_request_detail_not_found(self):
        """Requesting a non-existent ID returns 404."""
        res = self.standard_client.get('/help-requests/99999/')
        self.assertEqual(res.status_code, status.HTTP_404_NOT_FOUND)

    # -- Update --

    def test_update_help_request_by_author(self):
        """Author can update their own help request."""
        pk = self._create_help_request().data['id']
        res = self.standard_client.put(
            f'/help-requests/{pk}/', {'title': 'Updated title'}, format='json',
        )
        self.assertEqual(res.status_code, status.HTTP_200_OK)
        self.assertEqual(res.data['title'], 'Updated title')

    def test_update_help_request_by_non_author(self):
        """Non-author gets 403 when trying to update."""
        pk = self._create_help_request().data['id']
        res = self.expert_client.put(
            f'/help-requests/{pk}/', {'title': 'Nope'}, format='json',
        )
        self.assertEqual(res.status_code, status.HTTP_403_FORBIDDEN)

    def test_update_help_request_unauthenticated(self):
        """Unauthenticated update returns 401."""
        pk = self._create_help_request().data['id']
        res = self.anon.put(
            f'/help-requests/{pk}/', {'title': 'Nope'}, format='json',
        )
        self.assertEqual(res.status_code, status.HTTP_401_UNAUTHORIZED)

    # -- Delete --

    def test_delete_help_request_by_author(self):
        """Author can delete their own help request."""
        pk = self._create_help_request().data['id']
        res = self.standard_client.delete(f'/help-requests/{pk}/')
        self.assertEqual(res.status_code, status.HTTP_204_NO_CONTENT)
        self.assertFalse(HelpRequest.objects.filter(pk=pk).exists())

    def test_delete_help_request_by_non_author(self):
        """Non-author gets 403 when trying to delete."""
        pk = self._create_help_request().data['id']
        res = self.expert_client.delete(f'/help-requests/{pk}/')
        self.assertEqual(res.status_code, status.HTTP_403_FORBIDDEN)

    def test_delete_help_request_unauthenticated(self):
        """Unauthenticated delete returns 401."""
        pk = self._create_help_request().data['id']
        res = self.anon.delete(f'/help-requests/{pk}/')
        self.assertEqual(res.status_code, status.HTTP_401_UNAUTHORIZED)


# ── Comment Tests ─────────────────────────────────────────────────────────────

class HelpCommentTests(HelpTestBase):

    # -- List --

    def test_list_comments_empty(self):
        """A fresh help request has no comments."""
        pk = self._create_help_request().data['id']
        res = self.standard_client.get(f'/help-requests/{pk}/comments/')
        self.assertEqual(res.status_code, status.HTTP_200_OK)
        self.assertEqual(len(res.data), 0)

    def test_list_comments_ordered_by_created_at(self):
        """Comments are returned oldest-first (ascending created_at)."""
        pk = self._create_help_request().data['id']
        self.standard_client.post(
            f'/help-requests/{pk}/comments/', {'content': 'First'}, format='json',
        )
        self.expert_client.post(
            f'/help-requests/{pk}/comments/', {'content': 'Second'}, format='json',
        )
        res = self.standard_client.get(f'/help-requests/{pk}/comments/')
        self.assertEqual(len(res.data), 2)
        self.assertEqual(res.data[0]['content'], 'First')
        self.assertEqual(res.data[1]['content'], 'Second')

    def test_list_comments_unauthenticated(self):
        """Unauthenticated comment list returns 401."""
        pk = self._create_help_request().data['id']
        res = self.anon.get(f'/help-requests/{pk}/comments/')
        self.assertEqual(res.status_code, status.HTTP_401_UNAUTHORIZED)

    # -- Create --

    def test_create_comment(self):
        """Authenticated user can create a comment on a help request."""
        pk = self._create_help_request().data['id']
        res = self.expert_client.post(
            f'/help-requests/{pk}/comments/', {'content': 'I can help!'}, format='json',
        )
        self.assertEqual(res.status_code, status.HTTP_201_CREATED)
        self.assertEqual(res.data['content'], 'I can help!')
        # Author set from token.
        self.assertEqual(res.data['author']['email'], 'expert@example.com')

    def test_create_comment_unauthenticated(self):
        """Unauthenticated comment creation returns 401."""
        pk = self._create_help_request().data['id']
        res = self.anon.post(
            f'/help-requests/{pk}/comments/', {'content': 'Hello'}, format='json',
        )
        self.assertEqual(res.status_code, status.HTTP_401_UNAUTHORIZED)

    def test_create_comment_on_nonexistent_request(self):
        """Commenting on a non-existent request returns 404."""
        res = self.standard_client.post(
            '/help-requests/99999/comments/', {'content': 'Hello'}, format='json',
        )
        self.assertEqual(res.status_code, status.HTTP_404_NOT_FOUND)

    def test_comment_includes_author_role(self):
        """Comment response includes the author's role for expert badge display."""
        pk = self._create_help_request().data['id']
        res = self.expert_client.post(
            f'/help-requests/{pk}/comments/', {'content': 'Expert here'}, format='json',
        )
        self.assertEqual(res.data['author']['role'], 'EXPERT')

    # -- comment_count sync --

    def test_comment_count_increments(self):
        """comment_count on the parent request increments when a comment is created."""
        pk = self._create_help_request().data['id']
        self.assertEqual(HelpRequest.objects.get(pk=pk).comment_count, 0)

        self.standard_client.post(
            f'/help-requests/{pk}/comments/', {'content': 'One'}, format='json',
        )
        self.assertEqual(HelpRequest.objects.get(pk=pk).comment_count, 1)

        self.expert_client.post(
            f'/help-requests/{pk}/comments/', {'content': 'Two'}, format='json',
        )
        self.assertEqual(HelpRequest.objects.get(pk=pk).comment_count, 2)


# ── Auto Status Update Tests ─────────────────────────────────────────────────

class StatusUpdateTests(HelpTestBase):

    def test_standard_user_comment_does_not_change_status(self):
        """A standard user commenting does NOT change the request status."""
        pk = self._create_help_request().data['id']
        self.standard_client.post(
            f'/help-requests/{pk}/comments/', {'content': 'Hello'}, format='json',
        )
        self.assertEqual(
            HelpRequest.objects.get(pk=pk).status,
            HelpRequest.Status.OPEN,
        )

    def test_expert_comment_sets_status_to_expert_responding(self):
        """An expert commenting on an OPEN request sets status to EXPERT_RESPONDING."""
        pk = self._create_help_request().data['id']
        self.expert_client.post(
            f'/help-requests/{pk}/comments/', {'content': 'On my way'}, format='json',
        )
        self.assertEqual(
            HelpRequest.objects.get(pk=pk).status,
            HelpRequest.Status.EXPERT_RESPONDING,
        )

    def test_expert_comment_on_already_expert_responding(self):
        """An expert commenting on an EXPERT_RESPONDING request keeps the same status."""
        pk = self._create_help_request().data['id']
        # First expert comment: OPEN -> EXPERT_RESPONDING.
        self.expert_client.post(
            f'/help-requests/{pk}/comments/', {'content': 'First'}, format='json',
        )
        # Second expert comment: stays EXPERT_RESPONDING.
        self.expert_client.post(
            f'/help-requests/{pk}/comments/', {'content': 'Update'}, format='json',
        )
        self.assertEqual(
            HelpRequest.objects.get(pk=pk).status,
            HelpRequest.Status.EXPERT_RESPONDING,
        )

    def test_resolved_status_not_overwritten_by_expert_comment(self):
        """
        An expert commenting on a RESOLVED request does NOT revert the status.
        This is a critical business rule — resolved means resolved.
        """
        pk = self._create_help_request().data['id']
        # Manually set status to RESOLVED (simulating a prior resolution).
        HelpRequest.objects.filter(pk=pk).update(status=HelpRequest.Status.RESOLVED)

        self.expert_client.post(
            f'/help-requests/{pk}/comments/', {'content': 'Late reply'}, format='json',
        )
        self.assertEqual(
            HelpRequest.objects.get(pk=pk).status,
            HelpRequest.Status.RESOLVED,
        )


# ── Help Offer Tests ──────────────────────────────────────────────────────────

class HelpOfferTests(HelpTestBase):

    # -- Create --

    def test_create_help_offer(self):
        """Authenticated user can create a help offer."""
        res = self._create_help_offer()
        self.assertEqual(res.status_code, status.HTTP_201_CREATED)
        self.assertEqual(res.data['skill_or_resource'], 'First Aid')
        self.assertEqual(res.data['category'], 'MEDICAL')
        self.assertEqual(res.data['author']['email'], 'standard@example.com')

    def test_create_help_offer_unauthenticated(self):
        """Unauthenticated offer creation returns 401."""
        res = self._create_help_offer(client=self.anon)
        self.assertEqual(res.status_code, status.HTTP_401_UNAUTHORIZED)

    def test_create_help_offer_without_hub(self):
        """Hub is optional for offers."""
        res = self._create_help_offer(hub=None)
        self.assertEqual(res.status_code, status.HTTP_201_CREATED)
        self.assertIsNone(res.data['hub'])

    def test_create_help_offer_as_expert(self):
        """Expert users can also create offers."""
        res = self._create_help_offer(client=self.expert_client)
        self.assertEqual(res.status_code, status.HTTP_201_CREATED)
        self.assertEqual(res.data['author']['email'], 'expert@example.com')

    # -- List --

    def test_list_help_offers(self):
        """GET /help-offers/ returns all offers."""
        self._create_help_offer()
        self._create_help_offer(
            skill_or_resource='Vehicle', category='TRANSPORT',
        )
        res = self.standard_client.get('/help-offers/')
        self.assertEqual(res.status_code, status.HTTP_200_OK)
        self.assertEqual(len(res.data), 2)

    def test_list_help_offers_unauthenticated(self):
        """Unauthenticated offer list returns 401."""
        res = self.anon.get('/help-offers/')
        self.assertEqual(res.status_code, status.HTTP_401_UNAUTHORIZED)

    def test_list_help_offers_filter_by_hub(self):
        """Filtering by hub_id returns only offers for that hub."""
        other_hub, _ = Hub.objects.get_or_create(name='Ankara', defaults={'slug': 'ankara'})
        self._create_help_offer()
        self._create_help_offer(hub=other_hub.pk, skill_or_resource='Shelter space')
        res = self.standard_client.get(f'/help-offers/?hub_id={self.hub.pk}')
        self.assertEqual(len(res.data), 1)
        self.assertEqual(res.data[0]['skill_or_resource'], 'First Aid')

    def test_list_help_offers_filter_by_category(self):
        """Filtering by category returns only matching offers."""
        self._create_help_offer()
        self._create_help_offer(
            category='TRANSPORT', skill_or_resource='Vehicle',
        )
        res = self.standard_client.get('/help-offers/?category=TRANSPORT')
        self.assertEqual(len(res.data), 1)
        self.assertEqual(res.data[0]['skill_or_resource'], 'Vehicle')

    # -- Delete --

    def test_delete_help_offer_by_author(self):
        """Author can delete their own offer."""
        pk = self._create_help_offer().data['id']
        res = self.standard_client.delete(f'/help-offers/{pk}/')
        self.assertEqual(res.status_code, status.HTTP_204_NO_CONTENT)
        self.assertFalse(HelpOffer.objects.filter(pk=pk).exists())

    def test_delete_help_offer_by_non_author(self):
        """Non-author gets 403 when trying to delete an offer."""
        pk = self._create_help_offer().data['id']
        res = self.expert_client.delete(f'/help-offers/{pk}/')
        self.assertEqual(res.status_code, status.HTTP_403_FORBIDDEN)

    def test_delete_help_offer_unauthenticated(self):
        """Unauthenticated offer delete returns 401."""
        pk = self._create_help_offer().data['id']
        res = self.anon.delete(f'/help-offers/{pk}/')
        self.assertEqual(res.status_code, status.HTTP_401_UNAUTHORIZED)


# ── FCM Notification Tests ───────────────────────────────────────────────────

class HelpRequestNotificationTests(HelpTestBase):
    """
    Tests for send_help_request_notification (issue #124).

    Firebase is mocked so tests run without real credentials.
    Each test patches firebase_admin._apps to simulate an initialised app,
    and patches messaging.send_each_for_multicast to capture FCM calls.
    """

    def _setup_expert_with_token(self, email, token, hub=None):
        """Create an expert user with an FCM token."""
        user = User.objects.create_user(
            email=email, full_name=email.split('@')[0],
            password='Pass1234', hub=hub or self.hub, role=User.Role.EXPERT,
        )
        user.fcm_token = token
        user.save(update_fields=['fcm_token'])
        return user

    @patch('help_requests.notifications.messaging.send_each_for_multicast')
    @patch('help_requests.notifications.firebase_admin._apps', {'default': True})
    def test_notification_sent_to_hub_experts(self, mock_send):
        """Experts in the same hub receive a notification when a help request is created."""
        mock_send.return_value = MagicMock(failure_count=0)
        self._setup_expert_with_token('exp1@test.com', 'token-1')
        self._setup_expert_with_token('exp2@test.com', 'token-2')

        res = self._create_help_request()
        self.assertEqual(res.status_code, status.HTTP_201_CREATED)

        mock_send.assert_called_once()
        message = mock_send.call_args[0][0]
        self.assertCountEqual(message.tokens, ['token-1', 'token-2'])

    @patch('help_requests.notifications.messaging.send')
    @patch('help_requests.notifications.messaging.send_each_for_multicast')
    @patch('help_requests.notifications.firebase_admin._apps', {'default': True})
    def test_author_excluded_from_notification(self, mock_multicast, mock_send):
        """The request author does not receive a notification, even if they are an expert."""
        mock_multicast.return_value = MagicMock(failure_count=0)
        # Make the standard_user an expert with a token.
        self.standard_user.role = User.Role.EXPERT
        self.standard_user.fcm_token = 'author-token'
        self.standard_user.save(update_fields=['role', 'fcm_token'])

        other = self._setup_expert_with_token('other@test.com', 'other-token')

        res = self._create_help_request()
        self.assertEqual(res.status_code, status.HTTP_201_CREATED)

        # Fallback path: no matching expertise → other expert notified via multicast,
        # requester (author) notified via messaging.send (not via multicast).
        mock_multicast.assert_called_once()
        tokens = mock_multicast.call_args[0][0].tokens
        self.assertNotIn('author-token', tokens)
        self.assertIn('other-token', tokens)

    @patch('help_requests.notifications.messaging.send_each_for_multicast')
    @patch('help_requests.notifications.firebase_admin._apps', {'default': True})
    def test_experts_in_other_hub_not_notified(self, mock_send):
        """Experts in a different hub do not receive a notification."""
        mock_send.return_value = MagicMock(failure_count=0)
        other_hub, _ = Hub.objects.get_or_create(name='Ankara', defaults={'slug': 'ankara'})
        self._setup_expert_with_token('same@test.com', 'same-hub-token', hub=self.hub)
        self._setup_expert_with_token('diff@test.com', 'diff-hub-token', hub=other_hub)

        self._create_help_request()

        mock_send.assert_called_once()
        tokens = mock_send.call_args[0][0].tokens
        self.assertIn('same-hub-token', tokens)
        self.assertNotIn('diff-hub-token', tokens)

    @patch('help_requests.notifications.messaging.send_each_for_multicast')
    @patch('help_requests.notifications.firebase_admin._apps', {'default': True})
    def test_no_notification_without_hub(self, mock_send):
        """Help requests without a hub skip notifications entirely."""
        self._setup_expert_with_token('exp@test.com', 'token-1')

        res = self._create_help_request(hub=None)
        self.assertEqual(res.status_code, status.HTTP_201_CREATED)

        mock_send.assert_not_called()

    @patch('help_requests.notifications.messaging.send_each_for_multicast')
    @patch('help_requests.notifications.firebase_admin._apps', {'default': True})
    def test_standard_users_not_notified(self, mock_send):
        """Standard (non-expert) users with FCM tokens do not receive notifications."""
        mock_send.return_value = MagicMock(failure_count=0)
        # standard_user already exists in setUp with role=STANDARD
        self.standard_user.fcm_token = 'standard-token'
        self.standard_user.save(update_fields=['fcm_token'])

        expert = self._setup_expert_with_token('exp@test.com', 'expert-token')

        # Create as expert so standard_user isn't the author.
        res = self._create_help_request(client=self.expert_client)
        self.assertEqual(res.status_code, status.HTTP_201_CREATED)

        mock_send.assert_called_once()
        tokens = mock_send.call_args[0][0].tokens
        self.assertNotIn('standard-token', tokens)
        self.assertIn('expert-token', tokens)

    @patch('help_requests.notifications.messaging.send_each_for_multicast')
    @patch('help_requests.notifications.firebase_admin._apps', {'default': True})
    def test_no_fcm_call_when_no_expert_tokens(self, mock_send):
        """If no experts have FCM tokens, send_each_for_multicast is not called."""
        # expert_user from setUp has no fcm_token by default.
        res = self._create_help_request()
        self.assertEqual(res.status_code, status.HTTP_201_CREATED)

        mock_send.assert_not_called()

    @patch('help_requests.notifications.messaging.send_each_for_multicast')
    @patch('help_requests.notifications.firebase_admin._apps', {'default': True})
    def test_notification_payload_contains_request_details(self, mock_send):
        """Notification data payload includes request_id, title, category, and urgency."""
        mock_send.return_value = MagicMock(failure_count=0)
        self._setup_expert_with_token('exp@test.com', 'token-1')

        res = self._create_help_request(
            title='Injured person', category='MEDICAL', urgency='HIGH',
        )
        request_id = res.data['id']

        data = mock_send.call_args[0][0].data
        self.assertEqual(data['type'], 'help_request')
        self.assertEqual(data['request_id'], str(request_id))
        self.assertIn('Injured person', data['title'])
        self.assertIn('Medical', data['body'])
        self.assertIn('High', data['body'])

    @patch('help_requests.notifications.messaging.send_each_for_multicast')
    @patch('help_requests.notifications.firebase_admin._apps', {'default': True})
    def test_fcm_failure_does_not_break_create(self, mock_send):
        """If FCM raises an exception, the help request is still created successfully."""
        mock_send.side_effect = Exception('FCM unavailable')
        self._setup_expert_with_token('exp@test.com', 'token-1')

        res = self._create_help_request()
        self.assertEqual(res.status_code, status.HTTP_201_CREATED)
        self.assertTrue(HelpRequest.objects.filter(pk=res.data['id']).exists())

    @patch('help_requests.notifications.messaging.send_each_for_multicast')
    @patch('help_requests.notifications.firebase_admin._apps', {'default': True})
    def test_only_matching_expert_notified_primary(self, mock_send):
        """Only experts whose approved expertise matches the request category are notified."""
        mock_send.return_value = MagicMock(failure_count=0)

        medical_cat = ExpertiseCategory.objects.filter(help_request_category='MEDICAL').first()
        shelter_cat = ExpertiseCategory.objects.filter(help_request_category='SHELTER').first()

        medical_expert = self._setup_expert_with_token('med@test.com', 'medical-token')
        ExpertiseField.objects.create(user=medical_expert, category=medical_cat, is_approved=True)

        shelter_expert = self._setup_expert_with_token('shelter@test.com', 'shelter-token')
        ExpertiseField.objects.create(user=shelter_expert, category=shelter_cat, is_approved=True)

        self._create_help_request(category='MEDICAL')

        mock_send.assert_called_once()
        tokens = mock_send.call_args[0][0].tokens
        self.assertIn('medical-token', tokens)
        self.assertNotIn('shelter-token', tokens)

    @patch('help_requests.notifications.messaging.send_each_for_multicast')
    @patch('help_requests.notifications.firebase_admin._apps', {'default': True})
    def test_fallback_notifies_all_hub_experts_when_no_match(self, mock_send):
        """When no matching expert exists, all hub experts are notified as fallback."""
        mock_send.return_value = MagicMock(failure_count=0)

        shelter_cat = ExpertiseCategory.objects.filter(help_request_category='SHELTER').first()
        shelter_expert = self._setup_expert_with_token('shelter@test.com', 'shelter-token')
        ExpertiseField.objects.create(user=shelter_expert, category=shelter_cat, is_approved=True)

        # MEDICAL request — shelter expert has no MEDICAL expertise → fallback
        self._create_help_request(category='MEDICAL')

        mock_send.assert_called_once()
        tokens = mock_send.call_args[0][0].tokens
        self.assertIn('shelter-token', tokens)

    @patch('help_requests.notifications.messaging.send')
    @patch('help_requests.notifications.messaging.send_each_for_multicast')
    @patch('help_requests.notifications.firebase_admin._apps', {'default': True})
    def test_requester_notified_when_no_matching_expert(self, mock_multicast, mock_send):
        """Requester receives an FCM notification when no matching expert is found."""
        mock_multicast.return_value = MagicMock(failure_count=0)

        # Give the requester an FCM token.
        self.standard_user.fcm_token = 'requester-token'
        self.standard_user.save(update_fields=['fcm_token'])

        # A shelter expert exists — but the request is MEDICAL → fallback triggered.
        shelter_cat = ExpertiseCategory.objects.filter(help_request_category='SHELTER').first()
        shelter_expert = self._setup_expert_with_token('shelter@test.com', 'shelter-token')
        ExpertiseField.objects.create(user=shelter_expert, category=shelter_cat, is_approved=True)

        self._create_help_request(category='MEDICAL')

        mock_send.assert_called_once()
        call_message = mock_send.call_args[0][0]
        self.assertEqual(call_message.token, 'requester-token')
        self.assertEqual(call_message.data['type'], 'no_expert_available')

    def test_other_category_valid_for_help_request(self):
        """Help request can be created with category OTHER."""
        res = self._create_help_request(category='OTHER')
        self.assertEqual(res.status_code, status.HTTP_201_CREATED)
        self.assertEqual(res.data['category'], 'OTHER')


# ── Expertise Match Filter Tests ──────────────────────────────────────────────

class ExpertiseMatchFilterTests(HelpTestBase):
    """Tests for ?expertise_match=true on GET /help-requests/."""

    def setUp(self):
        super().setUp()
        self.medical_cat = ExpertiseCategory.objects.filter(help_request_category='MEDICAL').first()
        ExpertiseField.objects.create(
            user=self.expert_user, category=self.medical_cat, is_approved=True,
        )
        HelpRequest.objects.create(
            author=self.standard_user, hub=self.hub, category='MEDICAL',
            urgency='LOW', title='Medical request', description='test',
        )
        HelpRequest.objects.create(
            author=self.standard_user, hub=self.hub, category='FOOD',
            urgency='LOW', title='Food request', description='test',
        )

    def test_expertise_match_shows_only_matching_requests(self):
        """Expert with MEDICAL expertise only sees MEDICAL requests when filtering."""
        res = self.expert_client.get('/help-requests/?expertise_match=true')
        self.assertEqual(res.status_code, 200)
        categories = [r['category'] for r in res.data]
        self.assertIn('MEDICAL', categories)
        self.assertNotIn('FOOD', categories)

    def test_no_filter_shows_all_requests(self):
        """Without expertise_match, expert sees all requests."""
        res = self.expert_client.get('/help-requests/')
        categories = [r['category'] for r in res.data]
        self.assertIn('MEDICAL', categories)
        self.assertIn('FOOD', categories)

    def test_standard_user_unaffected_by_expertise_match(self):
        """Standard users see all requests even with expertise_match=true."""
        res = self.standard_client.get('/help-requests/?expertise_match=true')
        categories = [r['category'] for r in res.data]
        self.assertIn('MEDICAL', categories)
        self.assertIn('FOOD', categories)

    def test_expert_with_no_expertise_fields_sees_empty_list(self):
        """Expert with no approved expertise fields gets an empty list when filtering."""
        other_expert = User.objects.create_user(
            email='noexpertise@example.com', full_name='No Expertise',
            password='Pass1234', hub=self.hub, role=User.Role.EXPERT,
        )
        token = RefreshToken.for_user(other_expert).access_token
        client = APIClient()
        client.credentials(HTTP_AUTHORIZATION=f'Bearer {token}')

        res = client.get('/help-requests/?expertise_match=true')
        self.assertEqual(res.status_code, 200)
        self.assertEqual(len(res.data), 0)


# ── Image Upload Tests ────────────────────────────────────────────────────────

class ImageUploadTests(HelpTestBase):
    """
    Tests for POST /help-requests/upload/.

    Uses SimpleUploadedFile to create in-memory files and mocks
    default_storage.save to avoid touching the filesystem.
    """

    UPLOAD_URL = '/help-requests/upload/'

    def _make_image(self, name='photo.jpg', content_type='image/jpeg', size=None):
        """Return a SimpleUploadedFile representing a small valid image."""
        data = b'\xff\xd8\xff' + b'\x00' * (size - 3 if size and size > 3 else 1)
        return SimpleUploadedFile(name, data, content_type=content_type)

    @patch('help_requests.views.default_storage')
    def test_upload_single_image(self, mock_storage):
        """201 with a list of one URL for a valid single-file upload."""
        mock_storage.save.return_value = 'uploads/abc.jpg'
        img = self._make_image()
        res = self.standard_client.post(
            self.UPLOAD_URL, {'images': img}, format='multipart',
        )
        self.assertEqual(res.status_code, status.HTTP_201_CREATED)
        self.assertIn('urls', res.data)
        self.assertEqual(len(res.data['urls']), 1)

    @patch('help_requests.views.default_storage')
    def test_upload_multiple_images(self, mock_storage):
        """201 with a list of two URLs when two valid files are uploaded."""
        mock_storage.save.side_effect = ['uploads/a.jpg', 'uploads/b.png']
        img1 = self._make_image('a.jpg', 'image/jpeg')
        img2 = self._make_image('b.png', 'image/png')
        res = self.standard_client.post(
            self.UPLOAD_URL, {'images': [img1, img2]}, format='multipart',
        )
        self.assertEqual(res.status_code, status.HTTP_201_CREATED)
        self.assertEqual(len(res.data['urls']), 2)

    def test_upload_no_files_returns_400(self):
        """400 when no files are provided."""
        res = self.standard_client.post(self.UPLOAD_URL, {}, format='multipart')
        self.assertEqual(res.status_code, status.HTTP_400_BAD_REQUEST)

    def test_upload_invalid_file_type_returns_400(self):
        """400 when an unsupported MIME type is uploaded."""
        txt = SimpleUploadedFile('doc.txt', b'hello', content_type='text/plain')
        res = self.standard_client.post(
            self.UPLOAD_URL, {'images': txt}, format='multipart',
        )
        self.assertEqual(res.status_code, status.HTTP_400_BAD_REQUEST)
        self.assertIn('Unsupported file type', res.data['detail'])

    def test_upload_file_too_large_returns_400(self):
        """400 when a file exceeds the 5 MB limit."""
        big = SimpleUploadedFile(
            'big.jpg', b'\xff\xd8\xff' + b'\x00' * (5 * 1024 * 1024),
            content_type='image/jpeg',
        )
        res = self.standard_client.post(
            self.UPLOAD_URL, {'images': big}, format='multipart',
        )
        self.assertEqual(res.status_code, status.HTTP_400_BAD_REQUEST)
        self.assertIn('5 MB limit', res.data['detail'])

    def test_upload_unauthenticated_returns_401(self):
        """401 when no auth token is provided."""
        img = self._make_image()
        res = self.anon.post(self.UPLOAD_URL, {'images': img}, format='multipart')
        self.assertEqual(res.status_code, status.HTTP_401_UNAUTHORIZED)


# ── Comment Delete Tests ──────────────────────────────────────────────────────

class HelpCommentDeleteTests(HelpTestBase):
    """
    Tests for DELETE /help-requests/comments/{pk}/.

    Verifies author-only enforcement, comment_count decrement, and error cases.
    """

    def _create_comment(self, client=None, request_pk=None, content='Test comment'):
        """Helper to create a comment and return its data."""
        client = client or self.standard_client
        res = client.post(
            f'/help-requests/{request_pk}/comments/',
            {'content': content},
            format='json',
        )
        self.assertEqual(res.status_code, status.HTTP_201_CREATED)
        return res.data

    def test_author_can_delete_own_comment(self):
        """Comment author can delete their own comment — returns 204."""
        request_pk = self._create_help_request().data['id']
        comment = self._create_comment(request_pk=request_pk)

        res = self.standard_client.delete(f'/help-requests/comments/{comment["id"]}/')
        self.assertEqual(res.status_code, status.HTTP_204_NO_CONTENT)
        self.assertFalse(HelpComment.objects.filter(pk=comment['id']).exists())

    def test_delete_decrements_comment_count(self):
        """Deleting a comment decrements comment_count on the parent request."""
        request_pk = self._create_help_request().data['id']
        comment = self._create_comment(request_pk=request_pk)
        self.assertEqual(HelpRequest.objects.get(pk=request_pk).comment_count, 1)

        self.standard_client.delete(f'/help-requests/comments/{comment["id"]}/')
        self.assertEqual(HelpRequest.objects.get(pk=request_pk).comment_count, 0)

    def test_non_author_cannot_delete_comment(self):
        """A user who did not write the comment gets 403."""
        request_pk = self._create_help_request().data['id']
        comment = self._create_comment(request_pk=request_pk)

        res = self.expert_client.delete(f'/help-requests/comments/{comment["id"]}/')
        self.assertEqual(res.status_code, status.HTTP_403_FORBIDDEN)
        self.assertTrue(HelpComment.objects.filter(pk=comment['id']).exists())

    def test_unauthenticated_cannot_delete_comment(self):
        """Unauthenticated request returns 401."""
        request_pk = self._create_help_request().data['id']
        comment = self._create_comment(request_pk=request_pk)

        res = self.anon.delete(f'/help-requests/comments/{comment["id"]}/')
        self.assertEqual(res.status_code, status.HTTP_401_UNAUTHORIZED)

    def test_delete_nonexistent_comment_returns_404(self):
        """Deleting a comment that does not exist returns 404."""
        res = self.standard_client.delete('/help-requests/comments/99999/')
        self.assertEqual(res.status_code, status.HTTP_404_NOT_FOUND)
