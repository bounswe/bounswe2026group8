# Backend — Django REST API

Django REST Framework backend for the Neighborhood Emergency Preparedness Hub.

## Setup

```bash
# From the repo root
cd backend
pip install -r requirements.txt
python manage.py migrate
python manage.py runserver     # → http://localhost:8000
```

## API Endpoints

Protected endpoints require the header: `Authorization: Bearer <your-token>`

### Authentication

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/register` | No | Create account (Standard or Expert) |
| POST | `/login` | No | Returns JWT token + user data |
| POST | `/logout` | Bearer | Logout (client discards token) |
| GET | `/me` | Bearer | Returns current user profile |

### Help Requests

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/help-requests/` | Bearer | List help requests (filterable by `hub_id`, `category`) |
| POST | `/help-requests/` | Bearer | Create a new help request |
| GET | `/help-requests/{id}/` | Bearer | Get full detail of a help request |
| PUT | `/help-requests/{id}/` | Bearer | Update a help request (author only) |
| DELETE | `/help-requests/{id}/` | Bearer | Delete a help request (author only) |
| PATCH | `/help-requests/{id}/status/` | Bearer | Mark a help request as resolved (author only) |

### Help Request Comments

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/help-requests/{id}/comments/` | Bearer | List comments on a help request |
| POST | `/help-requests/{id}/comments/` | Bearer | Add a comment (auto-promotes status if expert) |

### Help Offers

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/help-offers/` | Bearer | List help offers (filterable by `hub_id`, `category`) |
| POST | `/help-offers/` | Bearer | Create a new help offer |
| DELETE | `/help-offers/{id}/` | Bearer | Delete a help offer (author only) |


### Profile

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| `GET` | `/profile` | Required | Get the authenticated user's profile |
| `PATCH` | `/profile` | Required | Update the authenticated user's profile (partial) |
| `GET` | `/users/<id>/` | Required | Get another user's public profile by ID |
| `GET` | `/resources` | Required | List the user's resources (shown on profile) |
| `POST` | `/resources` | Required | Add a resource |
| `PATCH` | `/resources/<id>` | Required | Update a resource |
| `DELETE` | `/resources/<id>` | Required | Delete a resource |
| `GET` | `/expertise` | Required (EXPERT only) | List expertise fields |
| `POST` | `/expertise` | Required (EXPERT only) | Add an expertise field |
| `PATCH` | `/expertise/<id>` | Required (EXPERT only) | Update an expertise field |
| `DELETE` | `/expertise/<id>` | Required (EXPERT only) | Delete an expertise field |

## Project Structure

```
backend/
├── backend/              Django project settings
│   ├── settings.py
│   ├── urls.py
│   └── wsgi.py
├── accounts/             Auth app
│   ├── models.py         Custom User model (STANDARD / EXPERT roles)
│   ├── serializers.py    Register, Login, User serializers
│   ├── views.py          API views for auth endpoints
│   ├── urls.py           Route definitions
│   ├── admin.py          Django admin registration
│   └── tests.py          Auth test suite
├── help_requests/        Help requests & offers app
│   ├── models.py         HelpRequest, HelpComment, HelpOffer models
│   ├── serializers.py    List, detail, create, update serializers
│   ├── views.py          CRUD views for requests, comments, and offers
│   ├── services.py       Business logic (expert status promotion)
│   ├── urls.py           Route definitions
│   ├── admin.py          Django admin registration
│   └── tests.py          40 automated API tests
├── manage.py
├── requirements.txt
└── db.sqlite3            Local dev database (git-ignored)
```

## Running Tests

```bash
python manage.py test accounts
python manage.py test help_requests
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
