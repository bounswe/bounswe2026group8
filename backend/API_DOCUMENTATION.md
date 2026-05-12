# EmergencyHub API Documentation

**Live API Documentation (Swagger UI):**
- Swagger UI: `http://localhost:8000/api/docs/` (development)
- ReDoc: `http://localhost:8000/api/redoc/` (development)
- OpenAPI Schema: `http://localhost:8000/api/schema/` (development)

Production URLs to be updated after deployment.

## API Overview

The EmergencyHub API provides comprehensive endpoints for managing emergency preparedness, community coordination, expert take-on workflows, badge achievements, offline messaging, and staff administration.

### Authentication

All endpoints (except `/register`, `/login`, `/hubs/`, and `/expertise-categories/`) require JWT authentication via the Authorization header:

```
Authorization: Bearer <access_token>
```

Obtain tokens by posting credentials to `/login`:

```bash
curl -X POST http://localhost:8000/login \
  -H "Content-Type: application/json" \
  -d '{"email": "user@example.com", "password": "password123"}'
```

### API Endpoints by Category

#### Authentication (6 endpoints)
- `POST /register` - Create new account
- `POST /login` - Authenticate and get JWT tokens
- `POST /logout` - Logout
- `GET /me` - Get current user
- `PATCH /me` - Update hub location
- `POST /accounts/fcm-token/` - Register FCM token for push notifications

#### Profile & Settings (4 endpoints)
- `GET /profile` - Get user profile
- `PATCH /profile` - Update profile
- `GET /settings` - Get user settings
- `PATCH /settings` - Update settings (notifications, privacy)

#### Resources (3 endpoints)
- `GET /resources` - List user's resources
- `POST /resources` - Add resource
- `PATCH|DELETE /resources/{id}` - Update/delete resource

#### Expertise (Expert Role Only, 4 endpoints)
- `GET /expertise` - List expert's expertise fields
- `POST /expertise` - Add expertise (creates PENDING)
- `PATCH|DELETE /expertise/{id}` - Update/delete expertise
- `GET /expertise-categories/` - List available categories (public)

#### User Management (2 endpoints)
- `GET /users/{id}` - Get public profile (respects privacy settings)
- `GET /hubs/` - List emergency hubs (public)

#### Forum/Discussion (5 endpoints)
- `GET /forum/posts` - List posts (filterable)
- `POST /forum/posts` - Create post
- `GET|PUT|DELETE /forum/posts/{id}` - Get/update/delete post
- `GET|POST /forum/posts/{id}/comments` - List/create comments
- `DELETE /forum/comments/{id}` - Delete comment

#### Help Requests (7 endpoints)
- `GET /help-requests` - List help requests (with filters)
- `POST /help-requests` - Create help request
- `GET|PUT|DELETE /help-requests/{id}` - Get/update/delete request
- `PATCH /help-requests/{id}/status` - Mark resolved
- `POST /help-requests/{id}/take-on` - Expert claims responsibility
- `POST /help-requests/{id}/release` - Expert releases responsibility
- `GET|POST /help-requests/{id}/comments` - Manage comments
- `DELETE /help-requests/comments/{id}` - Delete comment

#### Help Offers (2 endpoints)
- `GET /help-offers` - List help offers (with filters)
- `POST /help-offers` - Create offer
- `DELETE /help-offers/{id}` - Delete offer

#### Badges/Achievements (2 endpoints)
- `GET /api/badges/my-badges` - Get current user's badges
- `GET /api/badges/users/{id}` - Get specific user's badges

#### Mesh/Offline Messages (3 endpoints)
- `POST /mesh-messages/sync` - Sync offline messages
- `GET /mesh-messages` - List offline posts
- `GET /mesh-messages/{id}/comments` - List comments on post

#### Image Upload (1 endpoint)
- `POST /help-requests/upload` - Upload images (multipart)

#### Staff/Administration (8 endpoints, requires staff role)

**User Management (ADMIN only):**
- `GET /staff/users` - List users with filters
- `PATCH /staff/users/{id}/staff-role` - Assign/revoke staff roles
- `PATCH /staff/users/{id}/status` - Suspend/reactivate account

**Hub Management (ADMIN only):**
- `GET|POST /staff/hubs` - List/create hubs
- `PATCH|DELETE /staff/hubs/{id}` - Update/delete hub

**Audit Logging (ADMIN only):**
- `GET /staff/audit-logs` - View audit log

**Expertise Verification (VERIFICATION_COORDINATOR or ADMIN):**
- `GET /staff/expertise-verifications` - List expertise for review
- `PATCH /staff/expertise-verifications/{id}` - Approve/reject expertise

## Key Workflow Examples

### Expert Take-On Flow
1. Expert views help requests: `GET /help-requests?expertise_match=true`
2. Expert claims responsibility: `POST /help-requests/{id}/take-on`
3. System updates request status to EXPERT_RESPONDING
4. Expert adds comments: `POST /help-requests/{id}/comments`
5. Expert or requester marks resolved: `PATCH /help-requests/{id}/status`
6. Expert can release if needed: `POST /help-requests/{id}/release`

### Badge Achievement System
- Badges awarded automatically based on user actions
- View progress: `GET /api/badges/my-badges`
- Track specific user: `GET /api/badges/users/{id}`

### Offline Messaging (Mesh)
- Mobile app captures posts/comments offline
- Sync when online: `POST /mesh-messages/sync`
- Retrieve offline posts: `GET /mesh-messages`
- Later queries pull full posts: `GET /forum/posts`

## Error Responses

All errors follow standard HTTP status codes with JSON response bodies:

```json
{
  "detail": "Error message",
  "errors": {
    "field_name": ["specific error"]
  }
}
```

Common status codes:
- `200 OK` - Successful retrieval/update
- `201 Created` - Successful creation
- `204 No Content` - Successful deletion
- `400 Bad Request` - Invalid request data
- `401 Unauthorized` - Missing/invalid authentication
- `403 Forbidden` - Insufficient permissions
- `404 Not Found` - Resource not found
- `409 Conflict` - Already assigned/taken

## Rate Limiting & Pagination

- No rate limiting currently implemented (to be added)
- Pagination: Use query parameters `limit` and `offset` where available
- Default limit: 100 items

## Deprecation Policy

As of the final release (May 2026), all documented endpoints are active and supported. 
Deprecated endpoints will be clearly marked in the API documentation with:
- `[DEPRECATED]` tag in description
- Recommended replacement endpoint
- Removal timeline

Currently: **No deprecated endpoints**

## OpenAPI/Swagger Specification

Full OpenAPI 3.0.2 specification available at:
- Schema JSON: `/api/schema/`
- Schema YAML: `/api/schema.yaml`

Can be imported into API clients like:
- Postman
- Insomnia
- Swagger UI (already hosted)
- OpenAPI-compatible clients

## Contact & Support

For API issues, questions, or documentation updates:
- Email: support@emergencyhub.org
- Issue Tracker: [Project GitHub Issues]
- Internal Wiki: [Project Documentation Wiki]

---

*Last Updated: May 12, 2026*
*API Version: 1.0.0*
