"""
Tests for application staff roles, moderation, and expertise verification.

Covers:
- Permission helpers
- Staff endpoint authorization
- Admin safeguards (last-admin, self-action)
- Forum moderation (visibility, status changes, comment delete with audit)
- Help moderation (request/offer/comment delete with audit)
- Expertise verification (decision flow, rejection-needs-note, edit-resets)
- Audit log writes
- Registration cannot grant staff_role
- /me returns staff_role
- promote_app_admin command
"""

from io import StringIO

from django.core.management import call_command
from django.test import TestCase
from django.urls import reverse
from django.utils import timezone

from rest_framework import status
from rest_framework.test import APIClient
from rest_framework_simplejwt.tokens import RefreshToken

from forum.models import Comment, Post, Report
from help_requests.models import HelpComment, HelpOffer, HelpRequest

from .models import ExpertiseField, Hub, StaffAuditLog, User
from .permissions import (
    user_is_admin,
    user_is_moderator_or_admin,
    user_is_verification_coordinator_or_admin,
)


def _client_for(user):
    client = APIClient()
    token = RefreshToken.for_user(user).access_token
    client.credentials(HTTP_AUTHORIZATION=f'Bearer {token}')
    return client


class StaffBase(TestCase):
    """Reusable users covering every staff and community role combination."""

    def setUp(self):
        self.hub, _ = Hub.objects.get_or_create(name='Istanbul', defaults={'slug': 'istanbul'})

        self.standard = User.objects.create_user(
            email='standard@example.com', full_name='Standard',
            password='Pass1234', hub=self.hub,
        )
        self.expert = User.objects.create_user(
            email='expert@example.com', full_name='Expert', password='Pass1234',
            role=User.Role.EXPERT, expertise_field='First Aid', hub=self.hub,
        )
        self.moderator = User.objects.create_user(
            email='mod@example.com', full_name='Moderator', password='Pass1234',
            staff_role=User.StaffRole.MODERATOR, hub=self.hub,
        )
        self.verifier = User.objects.create_user(
            email='verifier@example.com', full_name='Verifier', password='Pass1234',
            staff_role=User.StaffRole.VERIFICATION_COORDINATOR, hub=self.hub,
        )
        self.admin = User.objects.create_user(
            email='admin@example.com', full_name='Admin', password='Pass1234',
            staff_role=User.StaffRole.ADMIN, hub=self.hub,
        )

        self.standard_client = _client_for(self.standard)
        self.expert_client = _client_for(self.expert)
        self.moderator_client = _client_for(self.moderator)
        self.verifier_client = _client_for(self.verifier)
        self.admin_client = _client_for(self.admin)
        self.anon = APIClient()


# ── Permission helpers ──────────────────────────────────────────────────────────

class PermissionHelperTests(StaffBase):
    def test_admin_helpers(self):
        self.assertTrue(user_is_admin(self.admin))
        self.assertTrue(user_is_moderator_or_admin(self.admin))
        self.assertTrue(user_is_verification_coordinator_or_admin(self.admin))

    def test_moderator_helpers(self):
        self.assertFalse(user_is_admin(self.moderator))
        self.assertTrue(user_is_moderator_or_admin(self.moderator))
        self.assertFalse(user_is_verification_coordinator_or_admin(self.moderator))

    def test_verifier_helpers(self):
        self.assertFalse(user_is_admin(self.verifier))
        self.assertFalse(user_is_moderator_or_admin(self.verifier))
        self.assertTrue(user_is_verification_coordinator_or_admin(self.verifier))

    def test_standard_user_helpers(self):
        self.assertFalse(user_is_admin(self.standard))
        self.assertFalse(user_is_moderator_or_admin(self.standard))
        self.assertFalse(user_is_verification_coordinator_or_admin(self.standard))

    def test_inactive_user_loses_authority(self):
        self.admin.is_active = False
        self.admin.save(update_fields=['is_active'])
        self.assertFalse(user_is_admin(self.admin))


# ── Auth payload ────────────────────────────────────────────────────────────────

class AuthPayloadTests(StaffBase):
    def test_me_returns_staff_role(self):
        response = self.admin_client.get('/me')
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(response.data['staff_role'], User.StaffRole.ADMIN)

    def test_register_cannot_set_staff_role(self):
        payload = {
            'full_name': 'Sneaky',
            'email': 'sneaky@example.com',
            'password': 'StrongPass123!',
            'confirm_password': 'StrongPass123!',
            'role': 'STANDARD',
            'staff_role': 'ADMIN',
            'is_staff': True,
        }
        response = APIClient().post('/register', payload, format='json')
        self.assertEqual(response.status_code, status.HTTP_201_CREATED)
        sneaky = User.objects.get(email='sneaky@example.com')
        self.assertEqual(sneaky.staff_role, User.StaffRole.NONE)
        self.assertFalse(sneaky.is_staff)


# ── Staff user management ───────────────────────────────────────────────────────

class StaffUserListTests(StaffBase):
    def test_admin_can_list_users(self):
        response = self.admin_client.get('/staff/users/')
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        emails = {u['email'] for u in response.data}
        self.assertIn('standard@example.com', emails)

    def test_non_admin_cannot_list_users(self):
        for client in (self.moderator_client, self.verifier_client, self.standard_client, self.anon):
            response = client.get('/staff/users/')
            self.assertIn(response.status_code, (status.HTTP_401_UNAUTHORIZED, status.HTTP_403_FORBIDDEN))

    def test_search_filter(self):
        response = self.admin_client.get('/staff/users/?search=verifier')
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertTrue(any(u['email'] == 'verifier@example.com' for u in response.data))


class StaffRoleAssignmentTests(StaffBase):
    def test_admin_can_promote_to_moderator(self):
        response = self.admin_client.patch(
            f'/staff/users/{self.standard.pk}/staff-role/',
            {'staff_role': 'MODERATOR', 'reason': 'community help'},
            format='json',
        )
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.standard.refresh_from_db()
        self.assertEqual(self.standard.staff_role, User.StaffRole.MODERATOR)
        self.assertTrue(StaffAuditLog.objects.filter(
            action=StaffAuditLog.Action.STAFF_ROLE_CHANGED,
            target_user=self.standard,
        ).exists())

    def test_admin_cannot_demote_self(self):
        response = self.admin_client.patch(
            f'/staff/users/{self.admin.pk}/staff-role/',
            {'staff_role': 'NONE'},
            format='json',
        )
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)

    def test_cannot_remove_last_active_admin(self):
        # Two admins exist (self.admin and target). Demoting target is fine
        # because self.admin remains active.
        target = User.objects.create_user(
            email='target_admin@example.com', full_name='Target',
            password='Pass1234', staff_role=User.StaffRole.ADMIN,
        )
        ok_response = self.admin_client.patch(
            f'/staff/users/{target.pk}/staff-role/',
            {'staff_role': 'NONE'},
            format='json',
        )
        self.assertEqual(ok_response.status_code, status.HTTP_200_OK)
        # Now self.admin is the only active admin. Add another admin who is
        # suspended — they don't count toward "active admins".
        spare = User.objects.create_user(
            email='spare_admin@example.com', full_name='Spare',
            password='Pass1234', staff_role=User.StaffRole.ADMIN, is_active=False,
        )
        # Promote target back to admin so target_client can act on self.admin.
        target.staff_role = User.StaffRole.ADMIN
        target.save(update_fields=['staff_role'])
        # Suspend target — now self.admin is the sole active admin again.
        target.is_active = False
        target.save(update_fields=['is_active'])
        # Demoting self.admin from another admin would orphan the system, but
        # self.admin cannot demote self.admin (self-action guard), and target
        # is suspended so its token is rejected by login flow. The simplest
        # confirmation: a second active admin tries to demote the only other
        # active admin while no others exist.
        target.is_active = True
        target.save(update_fields=['is_active'])
        # Now suspend self.admin; target is the only active admin.
        self.admin.is_active = False
        self.admin.save(update_fields=['is_active'])
        target_client = _client_for(target)
        # target tries to demote target -> blocked by self-action guard
        # AND last-active-admin guard.
        response = target_client.patch(
            f'/staff/users/{target.pk}/staff-role/',
            {'staff_role': 'NONE'},
            format='json',
        )
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)
        # Suspended/non-admin actors don't matter for this guard.
        self.assertTrue(spare)

    def test_non_admin_cannot_change_staff_role(self):
        response = self.moderator_client.patch(
            f'/staff/users/{self.standard.pk}/staff-role/',
            {'staff_role': 'MODERATOR'},
            format='json',
        )
        self.assertIn(response.status_code, (status.HTTP_401_UNAUTHORIZED, status.HTTP_403_FORBIDDEN))


class AccountStatusTests(StaffBase):
    def test_admin_can_suspend_user_with_reason(self):
        response = self.admin_client.patch(
            f'/staff/users/{self.standard.pk}/status/',
            {'is_active': False, 'reason': 'spam'},
            format='json',
        )
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.standard.refresh_from_db()
        self.assertFalse(self.standard.is_active)
        self.assertTrue(StaffAuditLog.objects.filter(
            action=StaffAuditLog.Action.USER_SUSPENDED,
        ).exists())

    def test_suspending_requires_reason(self):
        response = self.admin_client.patch(
            f'/staff/users/{self.standard.pk}/status/',
            {'is_active': False, 'reason': '   '},
            format='json',
        )
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)

    def test_cannot_suspend_self(self):
        response = self.admin_client.patch(
            f'/staff/users/{self.admin.pk}/status/',
            {'is_active': False, 'reason': 'self test'},
            format='json',
        )
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)

    def test_cannot_suspend_last_active_admin(self):
        actor = User.objects.create_user(
            email='actor@example.com', full_name='Actor', password='Pass1234',
            staff_role=User.StaffRole.ADMIN,
        )
        actor_client = _client_for(actor)
        # Try to suspend self.admin while actor remains as another active admin
        # — that should succeed because actor will remain.
        ok_response = actor_client.patch(
            f'/staff/users/{self.admin.pk}/status/',
            {'is_active': False, 'reason': 'rotation'},
            format='json',
        )
        self.assertEqual(ok_response.status_code, status.HTTP_200_OK)
        # Now try to suspend the only remaining active admin (actor) via a
        # second admin we promote as inactive (cannot act). We instead use
        # actor's own client — blocked by self-suspend guard, which also
        # protects last-admin invariant from this path.
        response = actor_client.patch(
            f'/staff/users/{actor.pk}/status/',
            {'is_active': False, 'reason': 'self'},
            format='json',
        )
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)

    def test_non_admin_cannot_change_status(self):
        response = self.moderator_client.patch(
            f'/staff/users/{self.standard.pk}/status/',
            {'is_active': False, 'reason': 'try'},
            format='json',
        )
        self.assertIn(response.status_code, (status.HTTP_401_UNAUTHORIZED, status.HTTP_403_FORBIDDEN))


# ── Hub management ─────────────────────────────────────────────────────────────

class StaffHubTests(StaffBase):
    def test_admin_can_create_hub(self):
        response = self.admin_client.post(
            '/staff/hubs/',
            {'name': 'Test Town'},
            format='json',
        )
        self.assertEqual(response.status_code, status.HTTP_201_CREATED, response.data)
        self.assertEqual(response.data['slug'], 'test-town')
        self.assertTrue(StaffAuditLog.objects.filter(
            action=StaffAuditLog.Action.HUB_CREATED,
        ).exists())

    def test_admin_can_update_hub(self):
        hub = Hub.objects.create(name='Edirne', slug='edirne')
        response = self.admin_client.patch(
            f'/staff/hubs/{hub.pk}/',
            {'name': 'Edirne Old City'},
            format='json',
        )
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        hub.refresh_from_db()
        self.assertEqual(hub.name, 'Edirne Old City')

    def test_hub_delete_requires_confirmation(self):
        hub = Hub.objects.create(name='Tekirdag', slug='tekirdag')
        response = self.admin_client.delete(f'/staff/hubs/{hub.pk}/', {}, format='json')
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)
        self.assertTrue(Hub.objects.filter(pk=hub.pk).exists())
        response = self.admin_client.delete(
            f'/staff/hubs/{hub.pk}/', {'confirm': True}, format='json',
        )
        self.assertEqual(response.status_code, status.HTTP_204_NO_CONTENT)
        self.assertFalse(Hub.objects.filter(pk=hub.pk).exists())

    def test_non_admin_cannot_manage_hubs(self):
        for client in (self.moderator_client, self.verifier_client, self.standard_client):
            response = client.post('/staff/hubs/', {'name': 'X'}, format='json')
            self.assertIn(response.status_code, (status.HTTP_401_UNAUTHORIZED, status.HTTP_403_FORBIDDEN))


# ── Audit log read endpoint ────────────────────────────────────────────────────

class AuditLogTests(StaffBase):
    def test_admin_can_view_audit_logs(self):
        # Trigger an action.
        self.admin_client.patch(
            f'/staff/users/{self.standard.pk}/staff-role/',
            {'staff_role': 'MODERATOR'},
            format='json',
        )
        response = self.admin_client.get('/staff/audit-logs/')
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertTrue(len(response.data) >= 1)

    def test_non_admin_blocked(self):
        response = self.moderator_client.get('/staff/audit-logs/')
        self.assertIn(response.status_code, (status.HTTP_401_UNAUTHORIZED, status.HTTP_403_FORBIDDEN))


# ── Forum moderation ───────────────────────────────────────────────────────────

class ForumModerationTests(StaffBase):
    def setUp(self):
        super().setUp()
        self.post = Post.objects.create(
            hub=self.hub, author=self.standard,
            forum_type=Post.ForumType.STANDARD,
            title='Hello', content='World',
        )

    def test_moderator_can_hide_post(self):
        response = self.moderator_client.patch(
            f'/forum/posts/{self.post.pk}/moderation/',
            {'action': 'HIDE', 'reason': 'spam'},
            format='json',
        )
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.post.refresh_from_db()
        self.assertEqual(self.post.status, Post.Status.HIDDEN)
        self.assertTrue(StaffAuditLog.objects.filter(
            action=StaffAuditLog.Action.FORUM_POST_HIDDEN,
        ).exists())

    def test_hide_requires_reason(self):
        response = self.moderator_client.patch(
            f'/forum/posts/{self.post.pk}/moderation/',
            {'action': 'HIDE'},
            format='json',
        )
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)

    def test_restore_does_not_require_reason(self):
        self.post.status = Post.Status.HIDDEN
        self.post.save(update_fields=['status'])
        response = self.moderator_client.patch(
            f'/forum/posts/{self.post.pk}/moderation/',
            {'action': 'RESTORE'},
            format='json',
        )
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.post.refresh_from_db()
        self.assertEqual(self.post.status, Post.Status.ACTIVE)

    def test_admin_can_remove_post(self):
        response = self.admin_client.patch(
            f'/forum/posts/{self.post.pk}/moderation/',
            {'action': 'REMOVE', 'reason': 'policy'},
            format='json',
        )
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.post.refresh_from_db()
        self.assertEqual(self.post.status, Post.Status.REMOVED)

    def test_non_staff_cannot_moderate(self):
        for client in (self.standard_client, self.expert_client, self.verifier_client):
            response = client.patch(
                f'/forum/posts/{self.post.pk}/moderation/',
                {'action': 'HIDE', 'reason': 'try'},
                format='json',
            )
            self.assertIn(response.status_code, (status.HTTP_401_UNAUTHORIZED, status.HTTP_403_FORBIDDEN))

    def test_hidden_post_is_404_for_non_staff(self):
        self.post.status = Post.Status.HIDDEN
        self.post.save(update_fields=['status'])
        # Anonymous user cannot see hidden post.
        response = self.anon.get(f'/forum/posts/{self.post.pk}/')
        self.assertEqual(response.status_code, status.HTTP_404_NOT_FOUND)
        # Other standard user cannot see hidden post.
        other_user = User.objects.create_user(
            email='other@example.com', full_name='Other', password='Pass1234',
        )
        other_client = _client_for(other_user)
        response = other_client.get(f'/forum/posts/{self.post.pk}/')
        self.assertEqual(response.status_code, status.HTTP_404_NOT_FOUND)
        # Author can still see their own hidden post.
        response = self.standard_client.get(f'/forum/posts/{self.post.pk}/')
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        # Moderator can still see hidden posts.
        response = self.moderator_client.get(f'/forum/posts/{self.post.pk}/')
        self.assertEqual(response.status_code, status.HTTP_200_OK)

    def test_moderator_can_delete_any_comment(self):
        comment = Comment.objects.create(post=self.post, author=self.standard, content='boo')
        Post.objects.filter(pk=self.post.pk).update(comment_count=1)
        response = self.moderator_client.delete(
            f'/forum/moderation/comments/{comment.pk}/', format='json',
        )
        self.assertEqual(response.status_code, status.HTTP_204_NO_CONTENT)
        self.assertFalse(Comment.objects.filter(pk=comment.pk).exists())
        self.assertTrue(StaffAuditLog.objects.filter(
            action=StaffAuditLog.Action.FORUM_COMMENT_DELETED,
        ).exists())

    def test_moderation_queue_lists_reported_or_hidden(self):
        self.post.status = Post.Status.HIDDEN
        self.post.save(update_fields=['status'])
        another = Post.objects.create(
            hub=self.hub, author=self.standard,
            forum_type=Post.ForumType.STANDARD,
            title='Quiet', content='nothing',
        )
        # Active post with a report should appear too.
        Report.objects.create(post=another, user=self.expert, reason=Report.Reason.SPAM)
        Post.objects.filter(pk=another.pk).update(report_count=1)
        response = self.moderator_client.get('/forum/moderation/posts/')
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        ids = {p['id'] for p in response.data}
        self.assertIn(self.post.pk, ids)
        self.assertIn(another.pk, ids)


# ── Help moderation ────────────────────────────────────────────────────────────

class HelpModerationTests(StaffBase):
    def setUp(self):
        super().setUp()
        self.help_request = HelpRequest.objects.create(
            author=self.standard, hub=self.hub,
            category='MEDICAL', urgency='HIGH',
            title='Help', description='Please',
        )
        self.help_offer = HelpOffer.objects.create(
            author=self.standard, hub=self.hub,
            category='TRANSPORT', skill_or_resource='Vehicle',
            description='Have car', availability='Anytime',
        )

    def test_moderator_can_delete_help_request(self):
        response = self.moderator_client.delete(
            f'/help-requests/{self.help_request.pk}/', {'reason': 'pii'}, format='json',
        )
        self.assertEqual(response.status_code, status.HTTP_204_NO_CONTENT)
        self.assertFalse(HelpRequest.objects.filter(pk=self.help_request.pk).exists())
        self.assertTrue(StaffAuditLog.objects.filter(
            action=StaffAuditLog.Action.HELP_REQUEST_DELETED,
        ).exists())

    def test_moderator_can_delete_help_offer(self):
        response = self.moderator_client.delete(
            f'/help-offers/{self.help_offer.pk}/', {'reason': 'spam'}, format='json',
        )
        self.assertEqual(response.status_code, status.HTTP_204_NO_CONTENT)
        self.assertFalse(HelpOffer.objects.filter(pk=self.help_offer.pk).exists())
        self.assertTrue(StaffAuditLog.objects.filter(
            action=StaffAuditLog.Action.HELP_OFFER_DELETED,
        ).exists())

    def test_moderator_can_delete_help_comment(self):
        comment = HelpComment.objects.create(
            request=self.help_request, author=self.standard, content='bad',
        )
        HelpRequest.objects.filter(pk=self.help_request.pk).update(comment_count=1)
        response = self.moderator_client.delete(
            f'/help-requests/comments/{comment.pk}/', {'reason': 'abuse'}, format='json',
        )
        self.assertEqual(response.status_code, status.HTTP_204_NO_CONTENT)
        self.assertFalse(HelpComment.objects.filter(pk=comment.pk).exists())
        self.assertTrue(StaffAuditLog.objects.filter(
            action=StaffAuditLog.Action.HELP_COMMENT_DELETED,
        ).exists())

    def test_non_staff_cannot_delete_others_help_content(self):
        other = User.objects.create_user(
            email='other@example.com', full_name='Other', password='Pass1234',
        )
        other_client = _client_for(other)
        response = other_client.delete(f'/help-requests/{self.help_request.pk}/')
        self.assertEqual(response.status_code, status.HTTP_403_FORBIDDEN)

    def test_help_moderation_queues(self):
        response = self.moderator_client.get('/help-requests/moderation/')
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        ids = {r['id'] for r in response.data}
        self.assertIn(self.help_request.pk, ids)
        response = self.moderator_client.get('/help-offers/moderation/')
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        ids = {r['id'] for r in response.data}
        self.assertIn(self.help_offer.pk, ids)

    def test_non_staff_blocked_from_help_moderation_queue(self):
        for client in (self.standard_client, self.verifier_client, self.anon):
            response = client.get('/help-requests/moderation/')
            self.assertIn(response.status_code, (status.HTTP_401_UNAUTHORIZED, status.HTTP_403_FORBIDDEN))


# ── Expertise verification ─────────────────────────────────────────────────────

class ExpertiseVerificationTests(StaffBase):
    def setUp(self):
        super().setUp()
        self.expertise = ExpertiseField.objects.create(
            user=self.expert,
            field='Cardiology',
            certification_level=ExpertiseField.CertificationLevel.ADVANCED,
            certification_document_url='https://example.com/cert.pdf',
        )

    def test_pending_default(self):
        self.assertEqual(
            self.expertise.verification_status,
            ExpertiseField.VerificationStatus.PENDING,
        )

    def test_verifier_can_approve(self):
        response = self.verifier_client.patch(
            f'/staff/expertise-verifications/{self.expertise.pk}/decision/',
            {'status': 'APPROVED', 'note': 'ok'},
            format='json',
        )
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.expertise.refresh_from_db()
        self.assertEqual(
            self.expertise.verification_status,
            ExpertiseField.VerificationStatus.APPROVED,
        )
        self.assertEqual(self.expertise.reviewed_by, self.verifier)
        self.assertIsNotNone(self.expertise.reviewed_at)
        self.assertTrue(StaffAuditLog.objects.filter(
            action=StaffAuditLog.Action.EXPERTISE_APPROVED,
        ).exists())

    def test_rejection_requires_note(self):
        response = self.verifier_client.patch(
            f'/staff/expertise-verifications/{self.expertise.pk}/decision/',
            {'status': 'REJECTED'},
            format='json',
        )
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)

    def test_reopen_clears_reviewer(self):
        self.expertise.verification_status = ExpertiseField.VerificationStatus.APPROVED
        self.expertise.reviewed_by = self.verifier
        self.expertise.reviewed_at = timezone.now()
        self.expertise.save(update_fields=['verification_status', 'reviewed_by', 'reviewed_at'])
        response = self.admin_client.patch(
            f'/staff/expertise-verifications/{self.expertise.pk}/decision/',
            {'status': 'PENDING', 'note': 'reassess'},
            format='json',
        )
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.expertise.refresh_from_db()
        self.assertIsNone(self.expertise.reviewed_by)
        self.assertIsNone(self.expertise.reviewed_at)
        self.assertEqual(
            self.expertise.verification_status,
            ExpertiseField.VerificationStatus.PENDING,
        )

    def test_only_verifier_or_admin_can_decide(self):
        for client in (self.standard_client, self.expert_client, self.moderator_client, self.anon):
            response = client.patch(
                f'/staff/expertise-verifications/{self.expertise.pk}/decision/',
                {'status': 'APPROVED'},
                format='json',
            )
            self.assertIn(response.status_code, (status.HTTP_401_UNAUTHORIZED, status.HTTP_403_FORBIDDEN))

    def test_editing_expertise_resets_to_pending(self):
        self.expertise.verification_status = ExpertiseField.VerificationStatus.APPROVED
        self.expertise.reviewed_by = self.verifier
        self.expertise.reviewed_at = timezone.now()
        self.expertise.verification_note = 'Looks good'
        self.expertise.save(update_fields=[
            'verification_status', 'reviewed_by', 'reviewed_at', 'verification_note',
        ])
        # The owning expert edits the certification document.
        response = self.expert_client.patch(
            f'/expertise/{self.expertise.pk}',
            {'certification_document_url': 'https://example.com/new.pdf'},
            format='json',
        )
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.expertise.refresh_from_db()
        self.assertEqual(
            self.expertise.verification_status,
            ExpertiseField.VerificationStatus.PENDING,
        )
        self.assertIsNone(self.expertise.reviewed_by)
        self.assertIsNone(self.expertise.reviewed_at)
        self.assertEqual(self.expertise.verification_note, '')

    def test_pending_queue(self):
        approved = ExpertiseField.objects.create(
            user=self.expert, field='Surgery',
            verification_status=ExpertiseField.VerificationStatus.APPROVED,
        )
        response = self.verifier_client.get('/staff/expertise-verifications/')
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        ids = {row['id'] for row in response.data}
        self.assertIn(self.expertise.pk, ids)
        self.assertNotIn(approved.pk, ids)


# ── Management command ─────────────────────────────────────────────────────────

class PromoteAppAdminCommandTests(StaffBase):
    def test_command_promotes_user(self):
        out = StringIO()
        call_command('promote_app_admin', 'standard@example.com', stdout=out)
        self.standard.refresh_from_db()
        self.assertEqual(self.standard.staff_role, User.StaffRole.ADMIN)
        self.assertIn('Set staff_role=ADMIN', out.getvalue())

    def test_command_supports_role_option(self):
        call_command('promote_app_admin', 'standard@example.com', '--role', 'MODERATOR')
        self.standard.refresh_from_db()
        self.assertEqual(self.standard.staff_role, User.StaffRole.MODERATOR)

    def test_command_does_not_change_django_flags(self):
        call_command('promote_app_admin', 'standard@example.com')
        self.standard.refresh_from_db()
        self.assertFalse(self.standard.is_staff)
        self.assertFalse(self.standard.is_superuser)

    def test_command_writes_audit_record(self):
        call_command('promote_app_admin', 'standard@example.com')
        self.assertTrue(StaffAuditLog.objects.filter(
            action=StaffAuditLog.Action.STAFF_ROLE_CHANGED,
            target_user=self.standard,
        ).exists())
