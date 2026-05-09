"""
URL routes for the help_requests app.

Help-request routes are mounted under /help-requests/ in the root URL config.
Help-offer routes are mounted under /help-offers/ in the root URL config.
The two sets are kept in separate lists so backend/urls.py can include them
under their respective prefixes.
"""

from django.urls import path
from .views import (
    HelpRequestListCreateView,
    HelpRequestDetailView,
    HelpRequestStatusView,
    HelpCommentListCreateView,
    HelpCommentDeleteView,
    HelpOfferListCreateView,
    HelpOfferDeleteView,
    ImageUploadView,
    HelpRequestModerationListView,
    HelpOfferModerationListView,
)

# Mounted at /help-requests/ by backend/urls.py.
help_request_urlpatterns = [
    path('', HelpRequestListCreateView.as_view(), name='help-request-list-create'),
    path('upload/', ImageUploadView.as_view(), name='help-request-image-upload'),
    path('moderation/', HelpRequestModerationListView.as_view(), name='help-request-moderation-list'),
    path('<int:pk>/', HelpRequestDetailView.as_view(), name='help-request-detail'),
    path('<int:pk>/status/', HelpRequestStatusView.as_view(), name='help-request-status'),
    path('<int:request_pk>/comments/', HelpCommentListCreateView.as_view(), name='help-comment-list-create'),
    path('comments/<int:pk>/', HelpCommentDeleteView.as_view(), name='help-comment-delete'),
]

# Mounted at /help-offers/ by backend/urls.py.
help_offer_urlpatterns = [
    path('', HelpOfferListCreateView.as_view(), name='help-offer-list-create'),
    path('moderation/', HelpOfferModerationListView.as_view(), name='help-offer-moderation-list'),
    path('<int:pk>/', HelpOfferDeleteView.as_view(), name='help-offer-delete'),
]
