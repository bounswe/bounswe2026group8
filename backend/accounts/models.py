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
    """

    class Role(models.TextChoices):
        STANDARD = 'STANDARD', 'Standard'
        EXPERT = 'EXPERT', 'Expert'

    # Core identity fields
    email = models.EmailField(unique=True)
    full_name = models.CharField(max_length=255)

    # Role & profile fields
    role = models.CharField(
        max_length=10,
        choices=Role.choices,
        default=Role.STANDARD,
    )
    hub = models.ForeignKey(
        Hub,
        on_delete=models.SET_NULL,
        null=True,
        blank=True,
        related_name='members',
    )
    neighborhood_address = models.CharField(max_length=255, blank=True, null=True)

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
        return f'{self.full_name} <{self.email}> [{self.role}]'


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


class HelpRequestCategory(models.TextChoices):
    """
    Mirrors help_requests.models.Category — defined here to avoid a circular import
    (help_requests imports accounts for User FK).
    """
    MEDICAL   = 'MEDICAL',   'Medical'
    FOOD      = 'FOOD',      'Food'
    SHELTER   = 'SHELTER',   'Shelter'
    TRANSPORT = 'TRANSPORT', 'Transport'
    OTHER     = 'OTHER',     'Other'


class ExpertiseCategory(models.Model):
    """
    Predefined expertise area managed by admins.
    Grouped under a help-request category for notification targeting.
    """
    name = models.CharField(max_length=100, unique=True)
    help_request_category = models.CharField(
        max_length=20,
        choices=HelpRequestCategory.choices,
    )
    is_active = models.BooleanField(default=True)

    class Meta:
        verbose_name_plural = 'expertise categories'
        ordering = ['help_request_category', 'name']

    def __str__(self):
        return f'{self.name} ({self.help_request_category})'


class ExpertiseField(models.Model):
    """An area of expertise for EXPERT users only."""

    class CertificationLevel(models.TextChoices):
        BEGINNER = 'BEGINNER', 'Beginner'
        ADVANCED = 'ADVANCED', 'Advanced'

    user = models.ForeignKey('User', on_delete=models.CASCADE, related_name='expertise_fields')
    category = models.ForeignKey(
        ExpertiseCategory,
        on_delete=models.PROTECT,
        related_name='expertise_fields',
    )
    is_approved = models.BooleanField(default=True)
    certification_level = models.CharField(
        max_length=10,
        choices=CertificationLevel.choices,
        default=CertificationLevel.BEGINNER,
    )
    certification_document_url = models.CharField(max_length=500, blank=True, null=True)

    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)

    def __str__(self):
        return f'{self.category.name} ({self.certification_level}) — {self.user.email}'


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

