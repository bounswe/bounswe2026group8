from django.urls import path
from .views import MyBadgesView

app_name = 'badges'

urlpatterns = [
    path('my-badges/', MyBadgesView.as_view(), name='my-badges'),
]