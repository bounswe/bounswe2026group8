# Backend — Django REST API

Django REST Framework backend for the Neighborhood Emergency Preparedness Hub.

## Setup

```bash
# From the repo root
python -m venv .venv
source .venv/bin/activate      # macOS/Linux
# .venv\Scripts\activate       # Windows

cd backend
pip install -r requirements.txt
python manage.py migrate
python manage.py populate_sample_data   # Optional: load sample hubs, users, content, and mesh messages
python manage.py runserver              # → http://localhost:8000
```

Sample credentials (after `populate_sample_data`):

| Email | Password | Role | Hub |
|-------|----------|------|-----|
| `admin@example.com` | `password123` | App Admin / Django Admin | Istanbul / Sariyer |
| `standard1@example.com` | `password123` | Standard | Istanbul / Sariyer |
| `expert1@example.com` | `password123` | Expert | Istanbul / Sariyer |

`populate_sample_data` is for local development data. It keeps the sample hub set to three district hubs (`Istanbul / Sariyer`, `Izmir / Konak`, `Ankara / Cankaya`) and creates 15 users total, with 5 users assigned to each hub. It also seeds forum content, help requests/offers, and offline mesh messages so the web, mobile, and offline messaging flows can be tested after setup.

## Project Structure

```
backend/
├── backend/              Django project settings
│   ├── settings.py
│   ├── urls.py
│   └── wsgi.py
├── accounts/             Auth, users, profiles, resources, expertise
│   ├── models.py         User, Hub, Profile, Resource, ExpertiseField, ExpertiseCategory
│   ├── serializers.py
│   ├── views.py
│   ├── urls.py
│   ├── admin.py
│   ├── tests.py
│   └── management/commands/populate_sample_data.py
├── forum/                Community discussion board
│   ├── models.py         Post, Comment, Vote
│   ├── serializers.py
│   ├── views.py
│   ├── urls.py
│   └── tests.py
├── help_requests/        Help requests, comments, and offers
│   ├── models.py         HelpRequest, HelpComment, HelpOffer
│   ├── serializers.py
│   ├── views.py
│   ├── services.py       Business logic (expert status promotion)
│   ├── urls.py
│   └── tests.py
├── badges/               Badge and achievement system
│   ├── models.py
│   ├── serializers.py
│   ├── views.py
│   └── tests.py
├── mesh/                 Offline mesh message relay
├── manage.py
└── requirements.txt
```

## API Endpoints

Protected endpoints require: `Authorization: Bearer <access_token>`

### Authentication

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/register` | No | Create account (Standard or Expert) |
| POST | `/login` | No | Returns JWT access + refresh tokens |
| POST | `/logout` | Bearer | Logout (clears server-side token) |
| GET | `/me` | Bearer | Returns current user profile |
| PATCH | `/me` | Bearer | Update hub or neighborhood address |

### Profile & Resources

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/profile` | Bearer | Get authenticated user's profile |
| PATCH | `/profile` | Bearer | Update profile fields (partial) |
| GET | `/users/<id>/` | Bearer | Get another user's public profile |
| GET | `/resources` | Bearer | List own resources |
| POST | `/resources` | Bearer | Add a resource |
| PATCH | `/resources/<id>` | Bearer | Update a resource |
| DELETE | `/resources/<id>` | Bearer | Delete a resource |

### Expertise (Expert role only)

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/expertise` | Bearer (Expert) | List own expertise fields |
| POST | `/expertise` | Bearer (Expert) | Add expertise — body: `{"category_id": <int>, "certification_level": "BEGINNER"\|"ADVANCED"}` |
| DELETE | `/expertise/<id>` | Bearer (Expert) | Delete an expertise field |
| GET | `/expertise-categories/` | No | List all active expertise categories |

### Forum

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/posts` | No | List posts (filterable by `forum_type`, `hub`) |
| POST | `/posts` | Bearer | Create a post |
| GET | `/posts/<id>` | No | Get post detail |
| PATCH | `/posts/<id>` | Bearer | Edit post (author only) |
| DELETE | `/posts/<id>` | Bearer | Delete post (author or moderator) |
| POST | `/posts/<id>/vote` | Bearer | Upvote or downvote a post |
| POST | `/posts/<id>/repost` | Bearer | Repost a post |
| GET | `/posts/<id>/comments` | No | List comments on a post |
| POST | `/posts/<id>/comments` | Bearer | Add a comment |
| DELETE | `/posts/<id>/comments/<cid>` | Bearer | Delete a comment (author or moderator) |

### Help Requests

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/help-requests/` | Bearer | List requests (filterable by `hub_id`, `category`) |
| POST | `/help-requests/` | Bearer | Create a help request |
| GET | `/help-requests/<id>/` | Bearer | Get request detail |
| PUT | `/help-requests/<id>/` | Bearer | Update a request (author only) |
| DELETE | `/help-requests/<id>/` | Bearer | Delete a request (author only) |
| PATCH | `/help-requests/<id>/status/` | Bearer | Mark as resolved (author only) |
| GET | `/help-requests/<id>/comments/` | Bearer | List comments |
| POST | `/help-requests/<id>/comments/` | Bearer | Add a comment (auto-promotes status if expert) |

### Help Offers

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/help-offers/` | Bearer | List offers (filterable by `hub_id`, `category`) |
| POST | `/help-offers/` | Bearer | Create an offer |
| DELETE | `/help-offers/<id>/` | Bearer | Delete an offer (author only) |

### Hubs

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/hubs/` | No | List all hubs |

### Badges

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/badges/my/` | Bearer | List own earned badges |

### User Settings

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/settings` | Bearer | Get notification and privacy settings |
| PATCH | `/settings` | Bearer | Update settings (partial) |

### Staff / Moderation (staff role required)

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/staff/users/` | Staff | List all users |
| PATCH | `/staff/users/<id>/role/` | Admin | Change a user's staff role |
| GET | `/staff/expertise-verifications/` | Staff | List expertise verification requests |
| PATCH | `/staff/expertise-verifications/<id>/decision/` | Staff | Approve or reject an expertise entry |

## Running Tests

```bash
# All apps
python manage.py test accounts forum help_requests badges

# Single app
python manage.py test accounts
```

## Key Design Decisions

- **Custom User model** (`accounts.User`) with email as the login identifier
- **Single model for both roles** — `role` field distinguishes Standard from Expert
- **JWT authentication** via `djangorestframework-simplejwt`
- **Password hashing** handled by Django's `set_password()` (PBKDF2 by default)
- **CORS enabled** for cross-origin requests from the React frontend
- **Service layer** (`services.py`) separates business logic from views
- **Status lifecycle** — OPEN → EXPERT_RESPONDING → RESOLVED (auto-promoted on expert comment, never reverts from RESOLVED)
- **Denormalized `comment_count`** — updated via F() expressions for race safety
- **Expertise write field** — POST `/expertise` accepts `category_id` (integer); GET returns `category` as a nested object with `id`, `name`, and `translations`
