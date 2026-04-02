from django.conf import settings
from django.conf.urls.static import static
from django.contrib import admin
from django.urls import path, include

from help_requests.urls import help_request_urlpatterns, help_offer_urlpatterns

urlpatterns = [
    path('admin/', admin.site.urls),
    path('', include('accounts.urls')),
    path('forum/', include('forum.urls')),
    path('help-requests/', include(help_request_urlpatterns)),
    path('help-offers/', include(help_offer_urlpatterns)),
]

if settings.DEBUG:
    urlpatterns += static(settings.MEDIA_URL, document_root=settings.MEDIA_ROOT)
