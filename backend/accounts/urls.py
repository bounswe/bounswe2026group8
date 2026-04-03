from django.urls import path
from .views import RegisterView, LoginView, LogoutView, MeView, HubListView, FCMTokenView

urlpatterns = [
    path('register', RegisterView.as_view(), name='register'),
    path('login', LoginView.as_view(), name='login'),
    path('logout', LogoutView.as_view(), name='logout'),
    path('me', MeView.as_view(), name='me'),
    path('hubs/', HubListView.as_view(), name='hub-list'),
    path('accounts/fcm-token/', FCMTokenView.as_view(), name='fcm-token'),
]
