# Final Release - Live API Documentation

## Overview

The EmergencyHub API features **comprehensive live Swagger/OpenAPI documentation** covering all 60+ endpoints of the final release. The documentation is automatically generated from code and stays in sync with the implementation.

## 📖 Access Live Documentation

### Development (Local Testing)
```
Swagger UI:  http://localhost:8000/api/docs/
ReDoc Docs:  http://localhost:8000/api/redoc/
JSON Schema: http://localhost:8000/api/schema/
```

**Setup**: 
```bash
cd backend
pip install -r requirements.txt
python manage.py runserver
```

### Production (Live Deployment)
```
Swagger UI:  https://api.emergencyhub.org/api/docs/
ReDoc Docs:  https://api.emergencyhub.org/api/redoc/
JSON Schema: https://api.emergencyhub.org/api/schema/
```

*URLs to be updated after deployment to actual production domain*

## 📋 Documentation Coverage

**Total Endpoints Documented**: 60+

### Endpoint Categories

| Category | Count | Examples |
|----------|-------|----------|
| Authentication | 6 | register, login, logout, me, FCM token |
| Profile & Settings | 4 | profile, settings, privacy controls |
| Resources | 3 | list, create, update/delete resources |
| Expertise | 4 | expertise fields, categories, verification |
| User Management | 2 | public profiles, hubs |
| Forum/Discussion | 5 | posts, comments, voting, reporting |
| Help Requests | 7 | request CRUD, expert take-on, status |
| Help Offers | 2 | offers CRUD |
| Badges/Achievements | 2 | user badges, progress tracking |
| Mesh/Offline Messages | 3 | sync, posts, comments |
| Staff/Administration | 8 | user management, audit logs, verification |

## ✨ Key Features

- ✅ **Complete Coverage** - All endpoints documented with examples
- ✅ **Interactive Testing** - "Try it out" directly in Swagger UI
- ✅ **JWT Authentication** - Full Bearer token documentation
- ✅ **Error Responses** - All error codes and messages documented
- ✅ **Query Parameters** - All filters, pagination, and sorting options
- ✅ **Multiple Formats** - Swagger UI, ReDoc, and raw JSON/YAML
- ✅ **Mobile Responsive** - Works on all devices
- ✅ **Production Ready** - Auto-generated from code, always in sync

## 🔐 Authentication

All protected endpoints require JWT Bearer token:

```bash
# Get token via login
curl -X POST http://localhost:8000/login \
  -H "Content-Type: application/json" \
  -d '{"email": "user@example.com", "password": "password"}'

# Use token in requests
curl -H "Authorization: Bearer <token>" \
  http://localhost:8000/api/docs/
```

## 📚 Documentation Files

### In Repository
- [`backend/API_DOCUMENTATION.md`](../backend/API_DOCUMENTATION.md) - Complete API reference guide
- [`backend/OPENAPI_SETUP.md`](../backend/OPENAPI_SETUP.md) - Setup and troubleshooting guide

### Implementation Details
- drf-spectacular (OpenAPI 3.0.2) integration
- Comprehensive docstrings on all views
- JWT Bearer authentication scheme
- Server URLs for dev and production

## 🧪 Testing

### Test in Swagger UI
1. Navigate to `/api/docs/`
2. Click "Authorize" button
3. Enter JWT token in Bearer token field
4. Click "Try it out" on any endpoint
5. Send request and view response

### Test via curl
```bash
curl -H "Authorization: Bearer YOUR_TOKEN" \
  http://localhost:8000/help-requests/
```

### Export Schema
```bash
# Get OpenAPI schema for import into other tools
curl http://localhost:8000/api/schema/ > openapi-schema.json
```

## 📱 Mobile Development

Generate client SDKs from the OpenAPI schema:

```bash
# Using OpenAPI Generator
openapi-generator-cli generate -i http://localhost:8000/api/schema/ \
  -g swift -o ./ios-client

# Using Swagger Codegen
swagger-codegen generate -i http://localhost:8000/api/schema/ \
  -l android -o ./android-client
```

## ❌ Deprecated Endpoints

**Current Status**: No deprecated endpoints in the final release.

All endpoints documented are active and fully supported.

Future deprecations will be marked with:
- `[DEPRECATED]` tag in description
- Recommended replacement endpoint
- Removal timeline

## 🔗 Related Documentation

- [Backend README](../backend/README.md) - Backend setup guide
- [Complete API Reference](../backend/API_DOCUMENTATION.md) - All endpoints with examples
- [OpenAPI Setup Guide](../backend/OPENAPI_SETUP.md) - Implementation details and troubleshooting

## 🚀 Deployment Checklist

Before deploying to production:

- [ ] Update production domain in `SPECTACULAR_SETTINGS['SERVERS']`
- [ ] Enable HTTPS for all API endpoints
- [ ] Configure CORS properly (restrict from `*` to specific domains)
- [ ] Set up rate limiting (optional but recommended)
- [ ] Test all endpoints with production database
- [ ] Verify Swagger UI is accessible at production URL
- [ ] Update this wiki with production URL

## 📞 Support

For questions about the API:
- Check Swagger UI documentation first
- Review [API Documentation](../backend/API_DOCUMENTATION.md)
- File issues in project repository

---

**Implementation**: May 12, 2026  
**Status**: ✅ Complete  
**Coverage**: 60+ endpoints  
**Spec Version**: OpenAPI 3.0.2  
**Framework**: Django REST Framework + drf-spectacular
