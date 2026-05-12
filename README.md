# Neighborhood Emergency Preparedness Hub

A community platform for disaster preparedness, mutual aid, and neighbourhood resilience. Users can register as **Standard** members or **Experts**, connect with neighbours, and access critical information — even offline.

---

## Repository Structure

```
├── backend/          Django REST API
├── frontend/         React (Vite) web client
├── mobile/           Android (Kotlin) app
├── nginx/            Nginx config for production
└── docker-compose.yml
```

---

## Local Development

Two options: **Docker Compose** (easiest, no local installs needed) or **native** setup.

---

### Option A — Docker Compose (recommended)

#### Prerequisites

| Tool | Version |
|------|---------|
| Docker | Latest |
| Android Studio | Latest (for mobile only) |

#### Running

```bash
docker-compose -f docker-compose.yml -f docker-compose.local.yml up --build
```

- Frontend: **http://localhost**
- API: **http://localhost:8000/api/docs/**

Subsequent runs (no code changes):
```bash
docker-compose -f docker-compose.yml -f docker-compose.local.yml up
```

To stop:
```bash
docker-compose -f docker-compose.yml -f docker-compose.local.yml down
```

> On first run, sample hubs, users, forum posts, help requests/offers, and offline mesh messages are automatically populated. Login with `admin@example.com`, `standard1@example.com`, or `expert1@example.com` and password `password123`.

> If you have a `backend/firebase-credentials.json` file it will be used for push notifications. Without it the app runs normally — only push notifications to mobile are disabled.

> **Mobile:** Android emulator debug builds automatically connect to `http://10.0.2.2:8000` — this works out of the box since the local Docker Compose exposes port 8000 directly. Run the emulator from Android Studio while Docker Compose is up.

---

### Option B — Native Setup

#### Prerequisites

| Tool | Version |
|------|---------|
| Python | 3.10+ |
| Node.js | 18+ |
| npm | 9+ |
| Docker | Latest (for local Postgres) |
| Android Studio | Latest (for mobile) |

### 1. Database (PostgreSQL)

The backend requires PostgreSQL. Choose one of the options below.

#### Option A — Docker (recommended, no install needed)

```bash
docker run -d \
  --name emergencyhub-db \
  -e POSTGRES_DB=emergencyhub \
  -e POSTGRES_USER=emergencyhub \
  -e POSTGRES_PASSWORD=emergencyhub \
  -p 5432:5432 \
  postgres:16-alpine
```

To stop/start it later:
```bash
docker stop emergencyhub-db
docker start emergencyhub-db
```

#### Option B — Native install

**macOS (Homebrew):**
```bash
brew install postgresql@16
brew services start postgresql@16
psql postgres -c "CREATE USER emergencyhub WITH PASSWORD 'emergencyhub';"
psql postgres -c "CREATE DATABASE emergencyhub OWNER emergencyhub;"
```

**Ubuntu/Debian:**
```bash
sudo apt install postgresql postgresql-contrib
sudo systemctl start postgresql
sudo -u postgres psql -c "CREATE USER emergencyhub WITH PASSWORD 'emergencyhub';"
sudo -u postgres psql -c "CREATE DATABASE emergencyhub OWNER emergencyhub;"
```

**Windows:**

Download and install from https://www.postgresql.org/download/windows/, then open **pgAdmin** or **psql** and run:
```sql
CREATE USER emergencyhub WITH PASSWORD 'emergencyhub';
CREATE DATABASE emergencyhub OWNER emergencyhub;
```

> Either option works — the Django settings default to `DB_NAME=emergencyhub`, `DB_USER=emergencyhub`, `DB_PASSWORD=emergencyhub`, `DB_HOST=localhost`. No `.env` file needed if you use these exact credentials.

### 2. Backend (Django — port 8000)

```bash
python -m venv .venv
source .venv/bin/activate      # macOS/Linux
# .venv\Scripts\activate       # Windows

cd backend
pip install -r requirements.txt
python manage.py migrate
python manage.py populate_sample_data   # Optional: load sample hubs, users, content, and mesh messages
python manage.py runserver
```

API is now live at **http://localhost:8000**.

Sample credentials (if `populate_sample_data` was run):

| Email | Password | Role | Hub |
|-------|----------|------|-----|
| `admin@example.com` | `password123` | App Admin / Django Admin | Istanbul / Sariyer |
| `standard1@example.com` | `password123` | Standard | Istanbul / Sariyer |
| `expert1@example.com` | `password123` | Expert | Istanbul / Sariyer |

`populate_sample_data` is intended for local development. It seeds exactly three district hubs (`Istanbul / Sariyer`, `Izmir / Konak`, `Ankara / Cankaya`), 15 users total with 5 users in each hub, forum posts, help requests/offers, and offline mesh messages for testing offline messaging.

### 3. Frontend (React + Vite — port 5173)

```bash
cd frontend
npm install
npm run dev
```

Open **http://localhost:5173** in your browser.

No `.env` file needed — the frontend automatically falls back to `http://localhost:8000` when `VITE_API_BASE` is not set.

> Both servers must run simultaneously.

### 4. Mobile (Android)

Open the `mobile/` folder in Android Studio and run on an **emulator**.

No configuration needed — debug builds automatically point to `http://10.0.2.2:8000` (the emulator's address for `localhost:8000` on your machine).

**Physical device on the same network:**

Find your machine's local IP (`ipconfig` on Windows, `ifconfig` on macOS/Linux), then add it to `mobile/local.properties`:
```
BASE_URL=http://x.x.x.x:8000/
```
Rebuild and install the APK. Both your machine and the device must be on the same network.

> The backend must be running locally for the mobile app to work.

---

## Production

Production is deployed on EC2 via Docker Compose and served through Nginx with HTTPS.

### How it works

| Layer | Role |
|-------|------|
| Nginx | Terminates SSL, routes `/api/` → Django, `/media/` → files, `/` → React |
| Django | Runs via Gunicorn on port 8000 (internal) |
| React | Built with `VITE_API_BASE=/api`, served as static files by Nginx |
| Mobile (release) | Points to `https://emergencyhub.duckdns.org/api/` |

### Deploying

Deployment is automatic — push to `main` triggers the CI/CD pipeline:
1. Runs backend, frontend, and Android unit tests in parallel
2. SSHes into EC2 and runs `docker-compose up --build -d` (only if all tests pass)

To deploy manually:

```bash
ssh -i <your-key.pem> ubuntu@<EC2_HOST>
cd bounswe2026group8
git pull origin main
docker-compose up --build -d
```

### Environment variables (EC2)

Create a `.env` file on the EC2 server next to `docker-compose.yml`:

```
DJANGO_SECRET_KEY=your-secret-key
DB_NAME=emergencyhub
DB_USER=emergencyhub
DB_PASSWORD=your-db-password
DJANGO_DEBUG=False
```

---

## Running Tests

### Backend (Django)

```bash
cd backend
python manage.py test accounts forum help_requests badges
```

### Frontend — Unit tests (Jest)

```bash
cd frontend
npm test
```

### Frontend — End-to-end tests (Playwright)

```bash
# Terminal 1
cd backend && python manage.py runserver localhost:8000

# Terminal 2
cd frontend && npm run dev -- --host 127.0.0.1 --port 5173

# Terminal 3 (after both servers are up)
cd frontend && npm run e2e
```

### Android — Unit tests

```bash
cd mobile
./gradlew testDebugUnitTest --no-daemon   # macOS/Linux
bash gradlew testDebugUnitTest --no-daemon  # Windows (Git Bash)
```

### Android — Instrumented tests (requires a connected emulator or device)

```bash
cd mobile
./gradlew connectedDebugAndroidTest --no-daemon # macOS/Linux
bash gradlew connectedDebugAndroidTest --no-daemon  # Windows (Git Bash)
```

---

## Tech Stack

- **Backend:** Django 4.2, Django REST Framework, PostgreSQL
- **Frontend:** React 19, Vite, React Router, Leaflet
- **Mobile:** Android (Kotlin), Retrofit, OkHttp, OSMDroid
- **Auth:** JWT (SimpleJWT)
- **Infrastructure:** EC2, Docker Compose, Nginx, Let's Encrypt

---

## Team

CMPE354 — Group 8
