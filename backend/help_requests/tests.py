"""
Tests for the help_requests app.

Covers all acceptance criteria from issues #120-#123:
  - Help request CRUD with permission enforcement
  - Comment list/create with comment_count sync
  - Auto status update to EXPERT_RESPONDING on expert comment
  - Status not overwritten when already RESOLVED
  - Help offer CRUD with permission checks
  - Unauthenticated access returns 401
  - Image upload endpoint validation and auth
"""

from unittest.mock import patch

from django.core.files.uploadedfile import SimpleUploadedFile
from django.test import TestCase
from rest_framework.test import APIClient
from rest_framework import status
from rest_framework_simplejwt.tokens import RefreshToken

from accounts.models import Hub, User
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
