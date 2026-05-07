"""
Audit logging utility.

`record_staff_action` writes a single immutable `StaffAuditLog` row in the
caller's transaction. Views are responsible for wrapping the action and the
log write together with `transaction.atomic()`.
"""

from .models import StaffAuditLog


def record_staff_action(
    *,
    actor,
    action: str,
    target_type: str,
    target_id=None,
    target_user=None,
    previous_state: dict | None = None,
    new_state: dict | None = None,
    reason: str = '',
) -> StaffAuditLog:
    """Persist a staff action. The caller is responsible for transactionality."""
    return StaffAuditLog.objects.create(
        actor=actor if actor is not None and getattr(actor, 'is_authenticated', False) else None,
        target_user=target_user,
        target_type=target_type,
        target_id='' if target_id is None else str(target_id),
        action=action,
        previous_state=previous_state or {},
        new_state=new_state or {},
        reason=reason or '',
    )
