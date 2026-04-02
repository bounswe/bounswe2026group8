from django.contrib import admin
from .models import Post, Comment, Vote, Report


@admin.register(Post)
class PostAdmin(admin.ModelAdmin):
    list_display = ('title', 'hub', 'forum_type', 'author', 'status', 'upvote_count', 'comment_count', 'created_at')
    list_filter = ('forum_type', 'status', 'hub')
    search_fields = ('title', 'content')


@admin.register(Comment)
class CommentAdmin(admin.ModelAdmin):
    list_display = ('id', 'post', 'author', 'created_at')
    list_filter = ('created_at',)


@admin.register(Vote)
class VoteAdmin(admin.ModelAdmin):
    list_display = ('user', 'post', 'vote_type', 'created_at')


@admin.register(Report)
class ReportAdmin(admin.ModelAdmin):
    list_display = ('user', 'post', 'reason', 'created_at')
    list_filter = ('reason',)
