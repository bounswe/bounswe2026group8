from django.urls import path
from .views import (
    RegisterView, LoginView, LogoutView, MeView, HubListView, ProfileView,
    ResourceListView, ResourceDetailView,
    ExpertiseFieldListView, ExpertiseFieldDetailView, FCMTokenView,
    UserPublicProfileView,
)
from .staff_views import (
    StaffUserListView,
    StaffUserStaffRoleView,
    StaffUserStatusView,
    StaffHubListCreateView,
    StaffHubDetailView,
    StaffAuditLogListView,
    ExpertiseVerificationListView,
    ExpertiseVerificationDecisionView,
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
    path('accounts/fcm-token/', FCMTokenView.as_view(), name='fcm-token'),

    # ── Staff (admin / verification coordinator) ─────────────────────────────
    path('staff/users/', StaffUserListView.as_view(), name='staff-user-list'),
    path('staff/users/<int:pk>/staff-role/', StaffUserStaffRoleView.as_view(), name='staff-user-staff-role'),
    path('staff/users/<int:pk>/status/', StaffUserStatusView.as_view(), name='staff-user-status'),
    path('staff/hubs/', StaffHubListCreateView.as_view(), name='staff-hub-list-create'),
    path('staff/hubs/<int:pk>/', StaffHubDetailView.as_view(), name='staff-hub-detail'),
    path('staff/audit-logs/', StaffAuditLogListView.as_view(), name='staff-audit-logs'),
    path(
        'staff/expertise-verifications/',
        ExpertiseVerificationListView.as_view(),
        name='staff-expertise-verification-list',
    ),
    path(
        'staff/expertise-verifications/<int:pk>/decision/',
        ExpertiseVerificationDecisionView.as_view(),
        name='staff-expertise-verification-decision',
    ),
]
