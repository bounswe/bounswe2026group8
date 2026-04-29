"""
Adds application staff roles, expertise verification workflow, and the
StaffAuditLog model. See docs/admin-moderator-implementation-plan.md.
"""

from django.conf import settings
from django.db import migrations, models
import django.db.models.deletion


class Migration(migrations.Migration):

    dependencies = [
        ('accounts', '0005_change_cert_url_to_charfield'),
    ]

    operations = [
        migrations.AddField(
            model_name='user',
            name='staff_role',
            field=models.CharField(
                choices=[
                    ('NONE', 'None'),
                    ('MODERATOR', 'Moderator'),
                    ('VERIFICATION_COORDINATOR', 'Verification Coordinator'),
                    ('ADMIN', 'Admin'),
                ],
                default='NONE',
                max_length=32,
            ),
        ),
        migrations.AddField(
            model_name='expertisefield',
            name='verification_status',
            field=models.CharField(
                choices=[
                    ('PENDING', 'Pending'),
                    ('APPROVED', 'Approved'),
                    ('REJECTED', 'Rejected'),
                ],
                default='PENDING',
                max_length=10,
            ),
        ),
        migrations.AddField(
            model_name='expertisefield',
            name='reviewed_by',
            field=models.ForeignKey(
                blank=True,
                null=True,
                on_delete=django.db.models.deletion.SET_NULL,
                related_name='reviewed_expertise_fields',
                to=settings.AUTH_USER_MODEL,
            ),
        ),
        migrations.AddField(
            model_name='expertisefield',
            name='reviewed_at',
            field=models.DateTimeField(blank=True, null=True),
        ),
        migrations.AddField(
            model_name='expertisefield',
            name='verification_note',
            field=models.TextField(blank=True, default=''),
        ),
        migrations.CreateModel(
            name='StaffAuditLog',
            fields=[
                ('id', models.BigAutoField(auto_created=True, primary_key=True, serialize=False, verbose_name='ID')),
                ('target_type', models.CharField(
                    choices=[
                        ('USER', 'User'),
                        ('POST', 'Forum Post'),
                        ('COMMENT', 'Forum Comment'),
                        ('HELP_REQUEST', 'Help Request'),
                        ('HELP_OFFER', 'Help Offer'),
                        ('HELP_COMMENT', 'Help Comment'),
                        ('EXPERTISE_FIELD', 'Expertise Field'),
                        ('HUB', 'Hub'),
                    ],
                    max_length=32,
                )),
                ('target_id', models.CharField(blank=True, default='', max_length=64)),
                ('action', models.CharField(
                    choices=[
                        ('STAFF_ROLE_CHANGED', 'Staff role changed'),
                        ('USER_SUSPENDED', 'User suspended'),
                        ('USER_REACTIVATED', 'User reactivated'),
                        ('FORUM_POST_HIDDEN', 'Forum post hidden'),
                        ('FORUM_POST_RESTORED', 'Forum post restored'),
                        ('FORUM_POST_REMOVED', 'Forum post removed'),
                        ('FORUM_COMMENT_DELETED', 'Forum comment deleted'),
                        ('HELP_REQUEST_DELETED', 'Help request deleted'),
                        ('HELP_OFFER_DELETED', 'Help offer deleted'),
                        ('HELP_COMMENT_DELETED', 'Help comment deleted'),
                        ('EXPERTISE_APPROVED', 'Expertise approved'),
                        ('EXPERTISE_REJECTED', 'Expertise rejected'),
                        ('EXPERTISE_REOPENED', 'Expertise reopened'),
                        ('HUB_CREATED', 'Hub created'),
                        ('HUB_UPDATED', 'Hub updated'),
                        ('HUB_DELETED', 'Hub deleted'),
                    ],
                    max_length=64,
                )),
                ('previous_state', models.JSONField(blank=True, default=dict)),
                ('new_state', models.JSONField(blank=True, default=dict)),
                ('reason', models.TextField(blank=True, default='')),
                ('created_at', models.DateTimeField(auto_now_add=True)),
                ('actor', models.ForeignKey(
                    null=True,
                    on_delete=django.db.models.deletion.SET_NULL,
                    related_name='staff_audit_actions',
                    to=settings.AUTH_USER_MODEL,
                )),
                ('target_user', models.ForeignKey(
                    blank=True,
                    null=True,
                    on_delete=django.db.models.deletion.SET_NULL,
                    related_name='staff_audit_targets',
                    to=settings.AUTH_USER_MODEL,
                )),
            ],
            options={
                'ordering': ['-created_at'],
            },
        ),
        migrations.AddIndex(
            model_name='staffauditlog',
            index=models.Index(fields=['target_type', 'target_id'], name='staff_audit_target_idx'),
        ),
        migrations.AddIndex(
            model_name='staffauditlog',
            index=models.Index(fields=['action', '-created_at'], name='staff_audit_action_ts_idx'),
        ),
    ]
