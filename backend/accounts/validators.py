import re
from django.core.exceptions import ValidationError
from django.utils.translation import gettext as _


class ComplexPasswordValidator:
    """
    Validates that a password contains uppercase, lowercase, and special characters.
    """
    def validate(self, password, user=None):
        has_upper = bool(re.search(r'[A-Z]', password))
        has_lower = bool(re.search(r'[a-z]', password))
        has_special = bool(re.search(r'[!@#$%^&*]', password))
        
        if not (has_upper and has_lower and has_special):
            raise ValidationError(
                _('Password must contain at least one uppercase letter, at least one lowercase letter, and at least one special character (!@#$%^&*).'),
                code='weak_password',
            )

    def get_help_text(self):
        return _('Your password must contain uppercase, lowercase, and special characters.')
