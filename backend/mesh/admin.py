from django.contrib import admin

from .models import MeshOfflineMessage


@admin.register(MeshOfflineMessage)
class MeshOfflineMessageAdmin(admin.ModelAdmin):
    list_display = ('id', 'post_type', 'parent_post_id', 'author_device_id', 'uploaded_at')
    list_filter = ('post_type',)
    search_fields = ('id', 'author_device_id', 'title', 'body')
    readonly_fields = ('uploaded_at',)
