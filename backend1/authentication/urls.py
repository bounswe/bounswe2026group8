"""
URL configuration for the authentication app.
"""

from django.urls import path
from . import views

app_name = 'authentication'

urlpatterns = [
    path('login', views.LoginView.as_view(), name='login'),
]
