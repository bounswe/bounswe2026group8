# Frontend — React Web Client

React + Vite web client for the Neighborhood Emergency Preparedness Hub.

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
| `/dashboard` | DashboardPage | Yes | Welcome card, feature grid, logout |
| `/help-requests` | HelpRequestsPage | Yes | Tabbed view: help requests list + help offers list |
| `/help-requests/new` | HelpRequestCreatePage | Yes | Form to create a new help request |
| `/help-requests/:id` | HelpRequestDetailPage | Yes | Request detail, map, comments, resolve button |

## Project Structure

```
frontend/src/
├── services/
│   └── api.js                     Fetch wrapper for backend endpoints
├── context/
│   └── AuthContext.jsx            Token + user state management
├── components/
│   └── ProtectedRoute.jsx         Redirects to /signin if not logged in
├── pages/
│   ├── LandingPage.jsx
│   ├── SignUpPage.jsx
│   ├── SignInPage.jsx
│   ├── DashboardPage.jsx
│   ├── HelpRequestsPage.jsx       Requests/Offers tabs, category filter, offer CRUD
│   ├── HelpRequestCreatePage.jsx  New help request form with geolocation
│   └── HelpRequestDetailPage.jsx  Detail view, Leaflet map, comments, resolve
├── App.jsx                        Router setup
├── main.jsx                       Entry point
└── index.css                      Full design system
```

## Features

- **Role-aware sign-up** — Expertise Field appears only when role = Expert
- **Token management** — stored in `localStorage`, validated via `GET /me` on page load
- **Protected routes** — redirects unauthenticated users to sign-in
- **Client-side validation** — immediate feedback before server round-trip
- **Modern dark UI** — glassmorphism, gradient accents, Inter font, responsive
- **Help Requests** — list, create, view detail, category filtering, mark as resolved (author only)
- **Help Offers** — list, create, delete (author only), detail modal, category filtering
- **Tab switcher** — Requests and Offers tabs share the same page with a shared category filter
- **Leaflet map** — displays request location when coordinates are available
- **Comments** — threaded comments with expert badge, auto status promotion
- **Browser geolocation** — optional "Use my location" button on request creation
