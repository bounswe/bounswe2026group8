from django.urls import path
from .views import MyBadgesView, UserBadgeListView

app_name = 'badges'

urlpatterns = [
    path('my-badges/', MyBadgesView.as_view(), name='my-badges'),
    path('users/<int:user_id>/', UserBadgeListView.as_view(), name='user-badges'),
]