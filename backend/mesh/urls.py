from django.urls import path

from .views import MeshSyncView, MeshPostListView, MeshCommentListView

urlpatterns = [
    path('sync/', MeshSyncView.as_view(), name='mesh-sync'),
    path('', MeshPostListView.as_view(), name='mesh-post-list'),
    path('<str:post_id>/comments/', MeshCommentListView.as_view(), name='mesh-comment-list'),
]
