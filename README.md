# Neighborhood Emergency Preparedness Hub

A community platform for disaster preparedness, mutual aid, and neighbourhood resilience. Users can register as **Standard** members or **Experts**, connect with neighbours, and access critical information — even offline.

> **Milestone 1** — Authentication, registration, and entry‑flow (landing → sign‑up → sign‑in → dashboard).

---

## Repository Structure

```
├── backend/          Django REST API (auth endpoints)
├── frontend/         React (Vite) web client
└── README.md         ← you are here
```

---

## Quick Start

### Prerequisites

| Tool | Version |
|------|---------|
| Python | 3.10+ |
| Node.js | 18+ |
| npm | 9+ |

### 1. Backend (Django — port 8000)

```bash
# Create & activate a virtual environment
python -m venv .venv

# Windows
.venv\Scripts\activate
# macOS / Linux
source .venv/bin/activate

# Install dependencies & run
cd backend
pip install -r requirements.txt
python manage.py migrate
python manage.py runserver
```

The API is now live at **http://localhost:8000**.

### 2. Frontend (React + Vite — port 5173)

```bash
cd frontend
npm install
npm run dev
```

Open **http://localhost:5173** in your browser.

> **Both servers must run simultaneously.** The React app calls the Django API at `localhost:8000`.

---

## Auth API Endpoints

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/register` | No | Create a new Standard or Expert account |
| POST | `/login` | No | Obtain an auth token |
| POST | `/logout` | Token | Invalidate the current token |
| GET | `/me` | Token | Get the logged‑in user's profile |

---

## Running Backend Tests

```bash
cd backend
python manage.py test accounts
```

---

## Tech Stack

- **Backend:** Django 4.2, Django REST Framework, SQLite (dev)
- **Frontend:** React 19, Vite, React Router
- **Auth:** Token‑based (DRF `TokenAuthentication`)

---

## Team

CMPE354 — Group 8
