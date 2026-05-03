from django.urls import path
from .views import (
    RegisterView, LoginView, LogoutView, MeView, HubListView, ProfileView,
    ResourceListView, ResourceDetailView,
    ExpertiseFieldListView, ExpertiseFieldDetailView, FCMTokenView,
    UserPublicProfileView, ExpertiseCategoryListView,
)

urlpatterns = [
    path('register', RegisterView.as_view(), name='register'),
    path('login', LoginView.as_view(), name='login'),
    path('logout', LogoutView.as_view(), name='logout'),
    path('me', MeView.as_view(), name='me'),
    path('profile', ProfileView.as_view(), name='profile'),
    path('resources', ResourceListView.as_view(), name='resource-list'),
    path('resources/<int:pk>', ResourceDetailView.as_view(), name='resource-detail'),
    path('expertise', ExpertiseFieldListView.as_view(), name='expertise-list'),
    path('expertise/<int:pk>', ExpertiseFieldDetailView.as_view(), name='expertise-detail'),
    path('users/<int:pk>/', UserPublicProfileView.as_view(), name='user-public-profile'),
    path('hubs/', HubListView.as_view(), name='hub-list'),
    path('expertise-categories/', ExpertiseCategoryListView.as_view(), name='expertise-category-list'),
    path('accounts/fcm-token/', FCMTokenView.as_view(), name='fcm-token'),
]
