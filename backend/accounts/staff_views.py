"""
Staff endpoints for admin and verification coordinator workflows.

Routes are mounted under `/staff/...` from `accounts/urls.py`. They are kept
in this module to avoid bloating `views.py` and to keep the staff surface
discoverable.
"""

from django.db import transaction
from django.db.models import Q
from django.shortcuts import get_object_or_404
from django.utils import timezone

from rest_framework import status
from rest_framework.response import Response
from rest_framework.views import APIView

from .audit import record_staff_action
from .models import ExpertiseField, Hub, StaffAuditLog, User
from .permissions import (
    IsAdminStaffRole,
    IsVerificationCoordinatorOrAdmin,
)
from .serializers import (
    AccountStatusUpdateSerializer,
    ExpertiseVerificationDecisionSerializer,
    ExpertiseFieldSerializer,
    HubSerializer,
    HubWriteSerializer,
    StaffAuditLogSerializer,
    StaffRoleUpdateSerializer,
    StaffUserListSerializer,
    UserSerializer,
)


# ── Admin: user management ───────────────────────────────────────────────────

def _active_admin_count(exclude_user_id=None) -> int:
    qs = User.objects.filter(staff_role=User.StaffRole.ADMIN, is_active=True)
    if exclude_user_id is not None:
        qs = qs.exclude(pk=exclude_user_id)
    return qs.count()


class StaffUserListView(APIView):
    """GET /staff/users/ — admin-only user listing with search and filters."""

    permission_classes = [IsAdminStaffRole]

    def get(self, request):
        qs = User.objects.all().select_related('hub')

        search = request.query_params.get('search')
        if search:
            qs = qs.filter(Q(email__icontains=search) | Q(full_name__icontains=search))

        for param, field in (
            ('role', 'role'),
            ('staff_role', 'staff_role'),
        ):
            value = request.query_params.get(param)
            if value:
                qs = qs.filter(**{field: value.upper()})

        is_active = request.query_params.get('is_active')
        if is_active is not None:
            qs = qs.filter(is_active=str(is_active).lower() in ('1', 'true', 'yes'))

        hub_id = request.query_params.get('hub')
        if hub_id:
            qs = qs.filter(hub_id=hub_id)

        return Response(StaffUserListSerializer(qs, many=True).data)


class StaffUserStaffRoleView(APIView):
    """PATCH /staff/users/<id>/staff-role/ — admin assigns or revokes staff roles."""

    permission_classes = [IsAdminStaffRole]

    def patch(self, request, pk):
        target = get_object_or_404(User, pk=pk)

        if target.pk == request.user.pk:
            return Response(
                {'detail': 'You cannot change your own staff role.'},
                status=status.HTTP_400_BAD_REQUEST,
            )

        serializer = StaffRoleUpdateSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        new_role = serializer.validated_data['staff_role']
        reason = serializer.validated_data.get('reason', '') or ''

        if new_role == target.staff_role:
            return Response(StaffUserListSerializer(target).data)

        # Cannot remove the last active admin.
        if (
            target.staff_role == User.StaffRole.ADMIN
            and new_role != User.StaffRole.ADMIN
            and _active_admin_count(exclude_user_id=target.pk) == 0
        ):
            return Response(
                {'detail': 'At least one active admin must remain.'},
                status=status.HTTP_400_BAD_REQUEST,
            )

        previous = {'staff_role': target.staff_role}
        with transaction.atomic():
            target.staff_role = new_role
            target.save(update_fields=['staff_role'])
            record_staff_action(
                actor=request.user,
                action=StaffAuditLog.Action.STAFF_ROLE_CHANGED,
                target_type=StaffAuditLog.TargetType.USER,
                target_id=target.pk,
                target_user=target,
                previous_state=previous,
                new_state={'staff_role': target.staff_role},
                reason=reason,
            )

        return Response(StaffUserListSerializer(target).data)


class StaffUserStatusView(APIView):
    """PATCH /staff/users/<id>/status/ — admin suspends or reactivates an account."""

    permission_classes = [IsAdminStaffRole]

    def patch(self, request, pk):
        target = get_object_or_404(User, pk=pk)

        if target.pk == request.user.pk:
            return Response(
                {'detail': 'You cannot change your own account status.'},
                status=status.HTTP_400_BAD_REQUEST,
            )

        serializer = AccountStatusUpdateSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        new_active = serializer.validated_data['is_active']
        reason = serializer.validated_data['reason']

        if new_active == target.is_active:
            return Response(StaffUserListSerializer(target).data)

        # Suspending the last active admin is rejected.
        if (
            not new_active
            and target.staff_role == User.StaffRole.ADMIN
            and _active_admin_count(exclude_user_id=target.pk) == 0
        ):
            return Response(
                {'detail': 'Cannot suspend the last active admin.'},
                status=status.HTTP_400_BAD_REQUEST,
            )

        previous = {'is_active': target.is_active}
        with transaction.atomic():
            target.is_active = new_active
            target.save(update_fields=['is_active'])
            record_staff_action(
                actor=request.user,
                action=(
                    StaffAuditLog.Action.USER_REACTIVATED
                    if new_active
                    else StaffAuditLog.Action.USER_SUSPENDED
                ),
                target_type=StaffAuditLog.TargetType.USER,
                target_id=target.pk,
                target_user=target,
                previous_state=previous,
                new_state={'is_active': target.is_active},
                reason=reason,
            )

        return Response(StaffUserListSerializer(target).data)


# ── Admin: hub management ────────────────────────────────────────────────────

class StaffHubListCreateView(APIView):
    """
    GET  /staff/hubs/ — list all hubs (admin-only is fine: same data is public
                        via /hubs/, but we keep the staff route for symmetry).
    POST /staff/hubs/ — create a new hub.
    """

    permission_classes = [IsAdminStaffRole]

    def get(self, request):
        return Response(HubSerializer(Hub.objects.all(), many=True).data)

    def post(self, request):
        serializer = HubWriteSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        with transaction.atomic():
            hub = serializer.save()
            record_staff_action(
                actor=request.user,
                action=StaffAuditLog.Action.HUB_CREATED,
                target_type=StaffAuditLog.TargetType.HUB,
                target_id=hub.pk,
                new_state={'name': hub.name, 'slug': hub.slug},
            )
        return Response(HubSerializer(hub).data, status=status.HTTP_201_CREATED)


class StaffHubDetailView(APIView):
    """
    PATCH  /staff/hubs/<id>/ — update a hub.
    DELETE /staff/hubs/<id>/ — delete a hub. Cascades to posts/help content per
                               existing FK behaviour; require an explicit
                               `confirm: true` flag to avoid accidental deletes.
    """

    permission_classes = [IsAdminStaffRole]

    def patch(self, request, pk):
        hub = get_object_or_404(Hub, pk=pk)
        previous = {'name': hub.name, 'slug': hub.slug}
        serializer = HubWriteSerializer(hub, data=request.data, partial=True)
        serializer.is_valid(raise_exception=True)
        with transaction.atomic():
            hub = serializer.save()
            record_staff_action(
                actor=request.user,
                action=StaffAuditLog.Action.HUB_UPDATED,
                target_type=StaffAuditLog.TargetType.HUB,
                target_id=hub.pk,
                previous_state=previous,
                new_state={'name': hub.name, 'slug': hub.slug},
            )
        return Response(HubSerializer(hub).data)

    def delete(self, request, pk):
        hub = get_object_or_404(Hub, pk=pk)
        if not request.data.get('confirm') is True:
            return Response(
                {
                    'detail': (
                        'Deleting a hub cascades to its posts, help requests, '
                        'and help offers. Pass {"confirm": true} to proceed.'
                    ),
                },
                status=status.HTTP_400_BAD_REQUEST,
            )
        snapshot = {'id': hub.pk, 'name': hub.name, 'slug': hub.slug}
        with transaction.atomic():
            hub.delete()
            record_staff_action(
                actor=request.user,
                action=StaffAuditLog.Action.HUB_DELETED,
                target_type=StaffAuditLog.TargetType.HUB,
                target_id=snapshot['id'],
                previous_state=snapshot,
            )
        return Response({'detail': 'Hub deleted.'}, status=status.HTTP_204_NO_CONTENT)


# ── Admin: audit log read endpoint ───────────────────────────────────────────

class StaffAuditLogListView(APIView):
    """GET /staff/audit-logs/ — admin-only chronological audit feed."""

    permission_classes = [IsAdminStaffRole]

    def get(self, request):
        qs = StaffAuditLog.objects.all().select_related('actor', 'target_user')

        for key in ('action', 'target_type'):
            value = request.query_params.get(key)
            if value:
                qs = qs.filter(**{key: value.upper()})

        actor_id = request.query_params.get('actor')
        if actor_id:
            qs = qs.filter(actor_id=actor_id)

        target_user = request.query_params.get('target_user')
        if target_user:
            qs = qs.filter(target_user_id=target_user)

        try:
            limit = int(request.query_params.get('limit', '100'))
        except (TypeError, ValueError):
            limit = 100
        limit = max(1, min(limit, 500))

        return Response(StaffAuditLogSerializer(qs[:limit], many=True).data)


# ── Verification coordinator: expertise queue and decisions ──────────────────

class ExpertiseVerificationListView(APIView):
    """
    GET /staff/expertise-verifications/ — list expertise records for review.
    Defaults to PENDING records; supports filters for status, certification
    level, and target user.
    """

    permission_classes = [IsVerificationCoordinatorOrAdmin]

    def get(self, request):
        qs = ExpertiseField.objects.all().select_related('user', 'reviewed_by')

        status_param = request.query_params.get('status', ExpertiseField.VerificationStatus.PENDING)
        if status_param and status_param.upper() != 'ALL':
            qs = qs.filter(verification_status=status_param.upper())

        cert_level = request.query_params.get('certification_level')
        if cert_level:
            qs = qs.filter(certification_level=cert_level.upper())

        user_id = request.query_params.get('user')
        if user_id:
            qs = qs.filter(user_id=user_id)

        items = []
        for ef in qs:
            row = ExpertiseFieldSerializer(ef).data
            row['user'] = UserSerializer(ef.user).data
            items.append(row)
        return Response(items)


class ExpertiseVerificationDecisionView(APIView):
    """PATCH /staff/expertise-verifications/<id>/decision/."""

    permission_classes = [IsVerificationCoordinatorOrAdmin]

    def patch(self, request, pk):
        expertise = get_object_or_404(ExpertiseField, pk=pk)
        serializer = ExpertiseVerificationDecisionSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        decision = serializer.validated_data['status']
        note = serializer.validated_data.get('note', '') or ''

        previous = {
            'verification_status': expertise.verification_status,
            'verification_note': expertise.verification_note,
            'reviewed_by_id': expertise.reviewed_by_id,
            'reviewed_at': expertise.reviewed_at.isoformat() if expertise.reviewed_at else None,
        }

        action_map = {
            ExpertiseField.VerificationStatus.APPROVED: StaffAuditLog.Action.EXPERTISE_APPROVED,
            ExpertiseField.VerificationStatus.REJECTED: StaffAuditLog.Action.EXPERTISE_REJECTED,
            ExpertiseField.VerificationStatus.PENDING: StaffAuditLog.Action.EXPERTISE_REOPENED,
        }

        with transaction.atomic():
            expertise.verification_status = decision
            expertise.verification_note = note
            if decision == ExpertiseField.VerificationStatus.PENDING:
                # Reopen clears prior reviewer; full history lives in the audit log.
                expertise.reviewed_by = None
                expertise.reviewed_at = None
            else:
                expertise.reviewed_by = request.user
                expertise.reviewed_at = timezone.now()
            expertise.save(update_fields=[
                'verification_status',
                'verification_note',
                'reviewed_by',
                'reviewed_at',
                'updated_at',
            ])
            record_staff_action(
                actor=request.user,
                action=action_map[decision],
                target_type=StaffAuditLog.TargetType.EXPERTISE_FIELD,
                target_id=expertise.pk,
                target_user=expertise.user,
                previous_state=previous,
                new_state={
                    'verification_status': expertise.verification_status,
                    'verification_note': expertise.verification_note,
                    'reviewed_by_id': expertise.reviewed_by_id,
                    'reviewed_at': expertise.reviewed_at.isoformat() if expertise.reviewed_at else None,
                },
                reason=note,
            )

        return Response(ExpertiseFieldSerializer(expertise).data)
