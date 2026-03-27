"""
URL routes for the help_requests app.

All routes are mounted under /help-requests/ in the root URL config.
"""

from django.urls import path
from .views import HelpRequestListCreateView, HelpRequestDetailView

urlpatterns = [
    path('', HelpRequestListCreateView.as_view(), name='help-request-list-create'),
    path('<int:pk>/', HelpRequestDetailView.as_view(), name='help-request-detail'),
]
