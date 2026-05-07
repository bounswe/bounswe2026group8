from django.urls import path
from .views import (
    PostListCreateView,
    PostDetailView,
    CommentListCreateView,
    CommentDeleteView,
    VoteView,
    ReportView,
    RepostView,
    ImageUploadView,
    ForumModerationListView,
    ForumPostModerationView,
    ForumModerationCommentDeleteView,
)

urlpatterns = [
    path('posts/', PostListCreateView.as_view(), name='post-list-create'),
    path('posts/<int:pk>/', PostDetailView.as_view(), name='post-detail'),
    path('posts/<int:post_pk>/comments/', CommentListCreateView.as_view(), name='comment-list-create'),
    path('posts/<int:post_pk>/vote/', VoteView.as_view(), name='vote'),
    path('posts/<int:post_pk>/report/', ReportView.as_view(), name='report'),
    path('posts/<int:post_pk>/repost/', RepostView.as_view(), name='repost'),
    path('comments/<int:pk>/', CommentDeleteView.as_view(), name='comment-delete'),
    path('upload/', ImageUploadView.as_view(), name='image-upload'),

    # ── Moderation (moderators / admins) ──────────────────────────────────────
    path('moderation/posts/', ForumModerationListView.as_view(), name='forum-moderation-list'),
    path('posts/<int:pk>/moderation/', ForumPostModerationView.as_view(), name='forum-post-moderation'),
    path(
        'moderation/comments/<int:pk>/',
        ForumModerationCommentDeleteView.as_view(),
        name='forum-moderation-comment-delete',
    ),
]
