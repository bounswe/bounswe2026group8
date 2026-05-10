"""
Django admin configuration for the help_requests app.

Registers HelpRequest, HelpComment, and HelpOffer so they can be
viewed and managed through the /admin/ interface.
"""

from django.contrib import admin
from .models import HelpRequest, HelpComment, HelpOffer


@admin.register(HelpRequest)
class HelpRequestAdmin(admin.ModelAdmin):
    list_display = ('title', 'hub', 'category', 'urgency', 'author', 'assigned_expert', 'status', 'comment_count', 'created_at')
    list_filter = ('category', 'urgency', 'status', 'hub', 'assigned_expert')
    search_fields = ('title', 'description')


@admin.register(HelpComment)
class HelpCommentAdmin(admin.ModelAdmin):
    list_display = ('id', 'request', 'author', 'created_at')
    list_filter = ('created_at',)


@admin.register(HelpOffer)
class HelpOfferAdmin(admin.ModelAdmin):
    list_display = ('skill_or_resource', 'hub', 'category', 'author', 'availability', 'created_at')
    list_filter = ('category', 'hub')
    search_fields = ('skill_or_resource', 'description')
