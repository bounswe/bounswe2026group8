# Django Authentication App Setup

This is a minimal Django REST Framework authentication app with login functionality.

## Project Structure

```
backend/
├── manage.py
├── db.sqlite3
├── requirements.txt
├── config/
│   ├── __init__.py
│   ├── settings.py       # Django settings
│   ├── urls.py           # Main URL configuration
│   ├── asgi.py
│   └── wsgi.py
└── authentication/
    ├── __init__.py
    ├── models.py         # Empty (uses Django's built-in User model)
    ├── views.py          # LoginView using APIView
    ├── serializers.py    # LoginSerializer and response serializers
    ├── urls.py           # Authentication app routes
    ├── services.py       # AuthService with credential validation and token generation
    ├── permissions.py    # Custom permissions (reserved for future use)
    └── tests/
        ├── __init__.py
        └── test_login.py # Login endpoint tests
```

## Setup Instructions

### 1. Installation

```bash
cd backend
pip install -r requirements.txt
```

### 2. Database Migration

```bash
python manage.py migrate
```

### 3. Create a Test User (Optional)

```bash
python manage.py shell
>>> from django.contrib.auth.models import User
>>> User.objects.create_user(username='testuser', email='test@example.com', password='testpass123')
>>> exit()
```

### 4. Run Development Server

```bash
python manage.py runserver
```

The server will start at `http://localhost:8000`

### 5. Run Tests

```bash
python manage.py test authentication.tests.test_login
```

## API Endpoints

### Login
- **Endpoint**: `POST /auth/login`
- **Request**:
  ```json
  {
      "email": "test@example.com",
      "password": "testpass123"
  }
  ```
- **Response (Success - 200)**:
  ```json
  {
      "accessToken": "eyJ0eXAiOiJKV1QiLCJhbGc...",
      "refreshToken": "eyJ0eXAiOiJKV1QiLCJhbGc...",
      "user": {
          "id": 1,
          "email": "test@example.com",
          "username": "testuser"
      }
  }
  ```
- **Response (Error - 401)**:
  ```json
  {
      "detail": "Invalid email or password"
  }
  ```

## Implementation Details

### Features
- ✅ JWT token generation (access + refresh tokens)
- ✅ Email and password validation
- ✅ Uses Django's built-in User model
- ✅ Password hashing with `check_password`
- ✅ Comprehensive test suite
- ✅ Clean separation of concerns (services, serializers, views)
- ✅ Proper error handling

### Key Files

**`authentication/services.py`**
- `AuthService.validate_credentials()`: Validates email/password
- `AuthService.generate_tokens()`: Generates JWT access and refresh tokens
- `AuthService.decode_token()`: Decodes and validates tokens

**`authentication/views.py`**
- `LoginView`: APIView for POST /auth/login

**`authentication/serializers.py`**
- `LoginSerializer`: Input validation
- `UserSerializer`: User data representation
- `LoginResponseSerializer`: Response structure

## Configuration

Edit `config/settings.py` to modify:
- `JWT_SECRET`: Change to a secure secret key in production
- `JWT_ALGORITHM`: JWT algorithm (default: HS256)
- `JWT_EXPIRATION_HOURS`: Access token expiration (default: 24 hours)
- `INSTALLED_APPS`: Include 'authentication' for the authentication app

## Next Steps

Planned extensions:
- User registration endpoint
- Token refresh endpoint
- User profile endpoint
- Email verification
- Password reset
- OAuth2 integration
