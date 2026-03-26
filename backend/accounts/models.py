from django.contrib.auth.models import AbstractBaseUser, BaseUserManager, PermissionsMixin
from django.db import models


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
    neighborhood_address = models.CharField(max_length=255, blank=True, null=True)
    expertise_field = models.CharField(max_length=255, blank=True, null=True)

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
