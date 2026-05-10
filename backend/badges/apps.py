from django.apps import AppConfig

class BadgesConfig(AppConfig):
    default_auto_field = 'django.db.models.BigAutoField'
    name = 'badges'

    def ready(self):
        # This tells Django to load the signals when the app starts up
        import badges.signals