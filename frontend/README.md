# Frontend — React Web Client

React + Vite web client for the Neighborhood Emergency Preparedness Hub authentication flow.

## Setup

```bash
# From the repo root
cd frontend
npm install
npm run dev            # → http://localhost:5173
```

> The Django backend must be running on `http://localhost:8000` for API calls to work.

## Pages

| Route | Component | Auth Required | Description |
|-------|-----------|---------------|-------------|
| `/` | LandingPage | No | Hero section, feature cards, entry buttons |
| `/signup` | SignUpPage | No | Registration form (role-aware) |
| `/signin` | SignInPage | No | Login form |
| `/dashboard` | DashboardPage | Yes | Welcome card, feature placeholders, logout |

## Project Structure

```
frontend/src/
├── services/
│   └── api.js              Fetch wrapper for backend endpoints
├── context/
│   └── AuthContext.jsx      Token + user state management
├── components/
│   └── ProtectedRoute.jsx   Redirects to /signin if not logged in
├── pages/
│   ├── LandingPage.jsx
│   ├── SignUpPage.jsx
│   ├── SignInPage.jsx
│   └── DashboardPage.jsx
├── App.jsx                  Router setup
├── main.jsx                 Entry point
└── index.css                Full design system
```

## Features

- **Role-aware sign-up** — Expertise Field appears only when role = Expert
- **Token management** — stored in `localStorage`, validated via `GET /me` on page load
- **Protected routes** — `/dashboard` redirects unauthenticated users to sign-in
- **Client-side validation** — immediate feedback before server round-trip
- **Modern dark UI** — glassmorphism, gradient accents, Inter font, responsive
