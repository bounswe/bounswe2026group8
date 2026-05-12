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

By default the frontend connects to `http://localhost:8000`. To point it at a different backend, set the environment variable before starting:

```bash
VITE_API_BASE=http://your-backend-url npm run dev
```

## Pages

| Route | Component | Auth Required | Description |
|-------|-----------|---------------|-------------|
| `/` | LandingPage | No | Hero section, feature cards, entry buttons |
| `/signup` | SignUpPage | No | Registration form (role-aware, expertise field for Experts) |
| `/signin` | SignInPage | No | Login form |
| `/dashboard` | DashboardPage | Yes | Welcome card, feature grid, logout |
| `/forum` | ForumPage | No | Community discussion board with tab filters |
| `/forum/posts/:id` | PostDetailPage | No | Post detail with comments and voting |
| `/forum/new` | PostCreatePage | Yes | Create a new forum post |
| `/help-requests` | HelpRequestsPage | Yes | Tabbed view: requests list and offers list |
| `/help-requests/new` | HelpRequestCreatePage | Yes | Create a help request with geolocation |
| `/help-requests/:id` | HelpRequestDetailPage | Yes | Request detail, map, comments, resolve button |
| `/profile` | ProfilePage | Yes | Profile info, resources, expertise fields |
| `/profile/:id` | ProfilePage | Yes | View another user's public profile |
| `/staff` | StaffDashboardPage | Staff | User management and moderation tools |
| `/staff/expertise` | ExpertiseVerificationPage | Staff | Review and approve expert submissions |

## Project Structure

```
frontend/src/
├── services/
│   └── api.js                     Fetch wrapper for all backend endpoints
├── context/
│   ├── AuthContext.jsx            Token + user state management
│   └── HubContext.jsx             Hub selection state
├── components/
│   ├── ProtectedRoute.jsx         Redirects to /signin if not logged in
│   └── HubSelector.jsx            Hub picker dropdown
├── pages/
│   ├── LandingPage.jsx
│   ├── SignUpPage.jsx
│   ├── SignInPage.jsx
│   ├── DashboardPage.jsx
│   ├── ForumPage.jsx              Forum post list with type and role filters
│   ├── PostDetailPage.jsx         Post detail, comments, voting, repost
│   ├── PostCreatePage.jsx         New forum post form
│   ├── HelpRequestsPage.jsx       Requests/Offers tabs, category filter
│   ├── HelpRequestCreatePage.jsx  New help request form with geolocation
│   ├── HelpRequestDetailPage.jsx  Detail view, Leaflet map, comments, resolve
│   ├── ProfilePage.jsx            Profile, resources, expertise management
│   ├── StaffDashboardPage.jsx     Staff moderation dashboard
│   └── ExpertiseVerificationPage.jsx  Expert submission review
├── locales/                       i18n translation files (en, tr, es, zh)
├── App.jsx                        Router setup
├── main.jsx                       Entry point
└── index.css                      Full design system
```

## Running Tests

### Unit tests (Jest + React Testing Library)

```bash
npm test
```

### End-to-end tests (Playwright)

```bash
# Terminal 1
cd ../backend && python manage.py runserver localhost:8000

# Terminal 2 (this directory)
npm run dev -- --host 127.0.0.1 --port 5173

# Terminal 3 (this directory, after both servers are up)
npm run e2e
```

## Features

- **Role-aware sign-up** — Expertise category selector appears only when role = Expert
- **JWT management** — stored in `localStorage`, validated via `GET /me` on page load
- **Protected routes** — redirects unauthenticated users to sign-in
- **Client-side validation** — immediate feedback before server round-trip
- **Internationalisation** — English, Turkish, Spanish, Chinese via i18next
- **Dark UI** — glassmorphism, gradient accents, responsive layout
- **Hub selector** — persistent picker for filtering content by city
- **Forum** — post list, detail view, comments, voting, reporting, reposting, type filters
- **Help Requests** — list, create, view detail, category filtering, expert take-on, resolve
- **Help Offers** — list, create, delete, detail modal, category filtering
- **Profile** — edit personal info, manage resources and expertise fields
- **Leaflet map** — displays request location when coordinates are available
- **Staff tools** — user role management, forum and help request moderation, expertise verification
