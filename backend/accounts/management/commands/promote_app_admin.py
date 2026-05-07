"""
Management command for bootstrapping the first application admin.

Usage:
    python manage.py promote_app_admin user@example.com
    python manage.py promote_app_admin user@example.com --role MODERATOR
    python manage.py promote_app_admin user@example.com --role NONE

Sets the application-level `staff_role`. Does not touch Django's `is_staff` or
`is_superuser` flags.
"""

from django.core.management.base import BaseCommand, CommandError

from accounts.audit import record_staff_action
from accounts.models import StaffAuditLog, User


class Command(BaseCommand):
    help = 'Set a user\'s application staff role (defaults to ADMIN).'

    def add_arguments(self, parser):
        parser.add_argument('email', help='Email address of the existing user')
        parser.add_argument(
            '--role',
            default=User.StaffRole.ADMIN,
            choices=[c[0] for c in User.StaffRole.choices],
            help='Staff role to assign (default: ADMIN)',
        )

    def handle(self, *args, **options):
        email = options['email'].strip().lower()
        role = options['role']

        try:
            user = User.objects.get(email__iexact=email)
        except User.DoesNotExist as exc:  # pragma: no cover — user-facing error path
            raise CommandError(f'No user with email {email!r}.') from exc

        if user.staff_role == role:
            self.stdout.write(self.style.WARNING(
                f'{user.email} is already {role}; nothing to do.'
            ))
            return

        previous = {'staff_role': user.staff_role}
        user.staff_role = role
        user.save(update_fields=['staff_role'])
        record_staff_action(
            actor=None,
            action=StaffAuditLog.Action.STAFF_ROLE_CHANGED,
            target_type=StaffAuditLog.TargetType.USER,
            target_id=user.pk,
            target_user=user,
            previous_state=previous,
            new_state={'staff_role': user.staff_role},
            reason='promote_app_admin management command',
        )
        self.stdout.write(self.style.SUCCESS(
            f'Set staff_role={role} for {user.email}.'
        ))
