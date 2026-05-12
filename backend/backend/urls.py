from django.conf import settings
from django.conf.urls.static import static
from django.contrib import admin
from django.urls import path, include
from drf_spectacular.views import SpectacularAPIView, SpectacularSwaggerView, SpectacularRedocView

from help_requests.urls import help_request_urlpatterns, help_offer_urlpatterns

urlpatterns = [
    path('admin/', admin.site.urls),
    
    # API Documentation
    # Note: external URLs are /api/schema/, /api/docs/, /api/redoc/.
    # nginx (`location /api/` with `proxy_pass http://backend/;`) strips the `/api/`
    # prefix before forwarding, so Django routes here must NOT include it.
    path('schema/', SpectacularAPIView.as_view(), name='schema'),
    path('docs/', SpectacularSwaggerView.as_view(url_name='schema'), name='swagger-ui'),
    path('redoc/', SpectacularRedocView.as_view(url_name='schema'), name='redoc'),
    
    # API Endpoints
    path('', include('accounts.urls')),
    path('forum/', include('forum.urls')),
    path('help-requests/', include(help_request_urlpatterns)),
    path('help-offers/', include(help_offer_urlpatterns)),
    path('mesh-messages/', include('mesh.urls')),
    path('api/badges/', include('badges.urls')),
]

if settings.DEBUG:
    urlpatterns += static(settings.MEDIA_URL, document_root=settings.MEDIA_ROOT)
