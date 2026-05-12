from django.contrib import admin
from .models import Badge, UserBadge

# This tells Django to show these tables in the admin panel
admin.site.register(Badge)
admin.site.register(UserBadge)