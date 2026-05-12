from django.conf import settings
from django.conf.urls.static import static
from django.contrib import admin
from django.urls import path, include
from drf_spectacular.views import SpectacularAPIView, SpectacularSwaggerView, SpectacularRedocView

from help_requests.urls import help_request_urlpatterns, help_offer_urlpatterns

urlpatterns = [
    path('admin/', admin.site.urls),
    
    # API Documentation
    # External URLs are /api/schema/, /api/docs/, /api/redoc/.
    # nginx (`location /api/` with `proxy_pass http://backend/;`) strips the
    # `/api/` prefix before forwarding, so Django routes here must NOT include
    # it. We pass `url='/api/schema/'` explicitly (instead of `url_name='schema'`)
    # so Swagger UI / ReDoc fetch the schema at the externally-reachable URL
    # rather than `/schema/`, which would fall through nginx to the React app.
    path('schema/', SpectacularAPIView.as_view(), name='schema'),
    # `?format=json` forces drf-spectacular's JSON renderer instead of the
    # default YAML one. ReDoc 2.5 and Swagger UI need JSON content; without
    # this, ReDoc fails with "Document must be JSON object, got string" and
    # browsers download the YAML file instead of rendering it.
    path('docs/', SpectacularSwaggerView.as_view(url='/api/schema/?format=json'), name='swagger-ui'),
    path('redoc/', SpectacularRedocView.as_view(url='/api/schema/?format=json'), name='redoc'),
    
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
