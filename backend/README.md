# Backend — Django Auth API

Django REST Framework backend serving authentication endpoints for the Neighborhood Emergency Preparedness Hub.

## Setup

```bash
# From the repo root
cd backend
pip install -r requirements.txt
python manage.py migrate
python manage.py runserver     # → http://localhost:8000
```

## API Endpoints

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/register` | No | Create account (Standard or Expert) |
| POST | `/login` | No | Returns auth token + user data |
| POST | `/logout` | Token | Deletes the auth token |
| GET | `/me` | Token | Returns current user profile |

Protected endpoints require the header: `Authorization: Token <your-token>`

## Project Structure

```
backend/
├── backend/           Django project settings
│   ├── settings.py
│   ├── urls.py
│   └── wsgi.py
├── accounts/          Auth app
│   ├── models.py      Custom User model (STANDARD / EXPERT roles)
│   ├── serializers.py Register, Login, User serializers
│   ├── views.py       API views for all 4 endpoints
│   ├── urls.py        Route definitions
│   ├── admin.py       Django admin registration
│   └── tests.py       Automated test suite
├── manage.py
├── requirements.txt
└── db.sqlite3         Local dev database (git-ignored)
```

## Running Tests

```bash
python manage.py test accounts
```

## Key Design Decisions

- **Custom User model** (`accounts.User`) with email as the login identifier
- **Single model for both roles** — `role` field distinguishes Standard from Expert
- **Token authentication** via `rest_framework.authtoken`
- **Password hashing** handled by Django's `set_password()` (PBKDF2 by default)
- **CORS enabled** for cross-origin requests from the React frontend
