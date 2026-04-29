from django.contrib.auth.models import AbstractBaseUser, BaseUserManager, PermissionsMixin
from django.db import models


class Hub(models.Model):
    """A city / neighbourhood hub that users and posts belong to."""
    name = models.CharField(max_length=120, unique=True)
    slug = models.SlugField(max_length=120, unique=True)
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        ordering = ['name']

    def __str__(self):
        return self.name


class UserManager(BaseUserManager):
    """Custom manager that uses email as the unique identifier."""

    def create_user(self, email, full_name, password=None, **extra_fields):
        if not email:
            raise ValueError('Email is required.')
        if not full_name:
            raise ValueError('Full name is required.')
        email = self.normalize_email(email)
        user = self.model(email=email, full_name=full_name, **extra_fields)
        user.set_password(password)
        user.save(using=self._db)
        return user

    def create_superuser(self, email, full_name, password=None, **extra_fields):
        """Creates a Django admin superuser (unrelated to app-level roles)."""
        extra_fields.setdefault('is_staff', True)
        extra_fields.setdefault('is_superuser', True)
        return self.create_user(email, full_name, password, **extra_fields)


class User(AbstractBaseUser, PermissionsMixin):
    """
    Single user model covering both STANDARD and EXPERT users.
    Role is stored as a field; expertise is only meaningful for EXPERT users.

    Application staff authority is independent of the community role and lives
    on `staff_role`. It is intentionally decoupled from Django's `is_staff` /
    `is_superuser` flags, which continue to control Django admin access only.
    """

    class Role(models.TextChoices):
        STANDARD = 'STANDARD', 'Standard'
        EXPERT = 'EXPERT', 'Expert'

    class StaffRole(models.TextChoices):
        NONE = 'NONE', 'None'
        MODERATOR = 'MODERATOR', 'Moderator'
        VERIFICATION_COORDINATOR = 'VERIFICATION_COORDINATOR', 'Verification Coordinator'
        ADMIN = 'ADMIN', 'Admin'

    # Core identity fields
    email = models.EmailField(unique=True)
    full_name = models.CharField(max_length=255)

    # Role & profile fields
    role = models.CharField(
        max_length=10,
        choices=Role.choices,
        default=Role.STANDARD,
    )
    staff_role = models.CharField(
        max_length=32,
        choices=StaffRole.choices,
        default=StaffRole.NONE,
    )
    hub = models.ForeignKey(
        Hub,
        on_delete=models.SET_NULL,
        null=True,
        blank=True,
        related_name='members',
    )
    neighborhood_address = models.CharField(max_length=255, blank=True, null=True)
    expertise_field = models.CharField(max_length=255, blank=True, null=True)

    # Push notifications
    fcm_token = models.TextField(blank=True, null=True)

    # Timestamps
    created_at = models.DateTimeField(auto_now_add=True)

    # Django admin / permission fields
    is_active = models.BooleanField(default=True)
    is_staff = models.BooleanField(default=False)

    objects = UserManager()

    USERNAME_FIELD = 'email'
    REQUIRED_FIELDS = ['full_name']

    class Meta:
        verbose_name = 'User'
        verbose_name_plural = 'Users'
        ordering = ['-created_at']

    def __str__(self):
        return f'{self.full_name} <{self.email}> [{self.role}/{self.staff_role}]'


class Profile(models.Model):
    """Extended profile attributes linked to a single user."""

    class AvailabilityStatus(models.TextChoices):
        SAFE = 'SAFE', 'Safe'
        NEEDS_HELP = 'NEEDS_HELP', 'Needs Help'
        AVAILABLE_TO_HELP = 'AVAILABLE_TO_HELP', 'Available to Help'

    user = models.OneToOneField('User', on_delete=models.CASCADE, related_name='profile')

    # Contact fields
    phone_number = models.CharField(max_length=20, blank=True, null=True, unique=True)
    emergency_contact_phone = models.CharField(max_length=20, blank=True, null=True)

    # Medical / accessibility fields
    blood_type = models.CharField(max_length=10, blank=True, null=True)
    special_needs = models.TextField(blank=True, null=True)
    has_disability = models.BooleanField(default=False)

    # Emergency availability status
    availability_status = models.CharField(
        max_length=20,
        choices=AvailabilityStatus.choices,
        default=AvailabilityStatus.SAFE,
    )

    # General profile fields (kept from original)
    bio = models.TextField(blank=True, null=True)
    preferred_language = models.CharField(max_length=100, blank=True, null=True)
    emergency_contact = models.CharField(max_length=255, blank=True, null=True)

    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)

    def __str__(self):
        return f'Profile for {self.user.email}'


class Resource(models.Model):
    """A resource owned by a user (available during emergencies)."""

    user = models.ForeignKey('User', on_delete=models.CASCADE, related_name='resources')
    name = models.CharField(max_length=255)
    category = models.CharField(max_length=255)
    quantity = models.PositiveIntegerField(default=1)
    condition = models.BooleanField(default=True, help_text='Whether the resource is functional')

    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)

    def __str__(self):
        return f'{self.name} (x{self.quantity}) — {self.user.email}'


class ExpertiseField(models.Model):
    """An area of expertise for EXPERT users only."""

    class CertificationLevel(models.TextChoices):
        BEGINNER = 'BEGINNER', 'Beginner'
        ADVANCED = 'ADVANCED', 'Advanced'

    class VerificationStatus(models.TextChoices):
        PENDING = 'PENDING', 'Pending'
        APPROVED = 'APPROVED', 'Approved'
        REJECTED = 'REJECTED', 'Rejected'

    user = models.ForeignKey('User', on_delete=models.CASCADE, related_name='expertise_fields')
    field = models.CharField(max_length=255, help_text='e.g. First Aid, Search and Rescue')
    certification_level = models.CharField(
        max_length=10,
        choices=CertificationLevel.choices,
        default=CertificationLevel.BEGINNER,
    )
    certification_document_url = models.CharField(max_length=500, blank=True, null=True)

    # Verification workflow handled by VERIFICATION_COORDINATOR / ADMIN staff.
    verification_status = models.CharField(
        max_length=10,
        choices=VerificationStatus.choices,
        default=VerificationStatus.PENDING,
    )
    reviewed_by = models.ForeignKey(
        'User',
        on_delete=models.SET_NULL,
        null=True,
        blank=True,
        related_name='reviewed_expertise_fields',
    )
    reviewed_at = models.DateTimeField(null=True, blank=True)
    verification_note = models.TextField(blank=True, default='')

    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)

    def __str__(self):
        return f'{self.field} ({self.certification_level}) — {self.user.email}'


class StaffAuditLog(models.Model):
    """
    Append-only record of every application staff action.

    Stored in the same database transaction as the action itself so the log
    always reflects what actually happened. There is no API for editing or
    deleting these rows.
    """

    class TargetType(models.TextChoices):
        USER = 'USER', 'User'
        POST = 'POST', 'Forum Post'
        COMMENT = 'COMMENT', 'Forum Comment'
        HELP_REQUEST = 'HELP_REQUEST', 'Help Request'
        HELP_OFFER = 'HELP_OFFER', 'Help Offer'
        HELP_COMMENT = 'HELP_COMMENT', 'Help Comment'
        EXPERTISE_FIELD = 'EXPERTISE_FIELD', 'Expertise Field'
        HUB = 'HUB', 'Hub'

    class Action(models.TextChoices):
        STAFF_ROLE_CHANGED = 'STAFF_ROLE_CHANGED', 'Staff role changed'
        USER_SUSPENDED = 'USER_SUSPENDED', 'User suspended'
        USER_REACTIVATED = 'USER_REACTIVATED', 'User reactivated'
        FORUM_POST_HIDDEN = 'FORUM_POST_HIDDEN', 'Forum post hidden'
        FORUM_POST_RESTORED = 'FORUM_POST_RESTORED', 'Forum post restored'
        FORUM_POST_REMOVED = 'FORUM_POST_REMOVED', 'Forum post removed'
        FORUM_COMMENT_DELETED = 'FORUM_COMMENT_DELETED', 'Forum comment deleted'
        HELP_REQUEST_DELETED = 'HELP_REQUEST_DELETED', 'Help request deleted'
        HELP_OFFER_DELETED = 'HELP_OFFER_DELETED', 'Help offer deleted'
        HELP_COMMENT_DELETED = 'HELP_COMMENT_DELETED', 'Help comment deleted'
        EXPERTISE_APPROVED = 'EXPERTISE_APPROVED', 'Expertise approved'
        EXPERTISE_REJECTED = 'EXPERTISE_REJECTED', 'Expertise rejected'
        EXPERTISE_REOPENED = 'EXPERTISE_REOPENED', 'Expertise reopened'
        HUB_CREATED = 'HUB_CREATED', 'Hub created'
        HUB_UPDATED = 'HUB_UPDATED', 'Hub updated'
        HUB_DELETED = 'HUB_DELETED', 'Hub deleted'

    actor = models.ForeignKey(
        'User',
        on_delete=models.SET_NULL,
        null=True,
        related_name='staff_audit_actions',
    )
    target_user = models.ForeignKey(
        'User',
        on_delete=models.SET_NULL,
        null=True,
        blank=True,
        related_name='staff_audit_targets',
    )
    target_type = models.CharField(max_length=32, choices=TargetType.choices)
    target_id = models.CharField(max_length=64, blank=True, default='')
    action = models.CharField(max_length=64, choices=Action.choices)
    previous_state = models.JSONField(default=dict, blank=True)
    new_state = models.JSONField(default=dict, blank=True)
    reason = models.TextField(blank=True, default='')
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        ordering = ['-created_at']
        indexes = [
            models.Index(fields=['target_type', 'target_id']),
            models.Index(fields=['action', '-created_at']),
        ]

    def __str__(self):
        actor_label = self.actor.email if self.actor_id else 'system'
        return f'{actor_label} {self.action} {self.target_type}:{self.target_id}'


# Automatically create Profile when User is created
from django.db.models.signals import post_save
from django.dispatch import receiver


@receiver(post_save, sender=User)
def create_user_profile(sender, instance, created, **kwargs):
    if created:
        Profile.objects.create(user=instance)


@receiver(post_save, sender=User)
def save_user_profile(sender, instance, created, **kwargs):
    if not created:
        try:
            instance.profile.save()
        except Profile.DoesNotExist:
            Profile.objects.create(user=instance)
