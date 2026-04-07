# Emergency Preparedness Hub — Android Mobile App

Android client for the Neighborhood Emergency Preparedness Hub authentication system. Consumes the **same Django REST API** as the React web frontend.

---

## Project Structure

```
mobile/
├── app/
│   ├── build.gradle.kts           # App dependencies (Retrofit, Material, Coroutines)
│   ├── proguard-rules.pro
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/bounswe2026group8/emergencyhub/
│       │   ├── api/
│       │   │   ├── AuthModels.kt       # Request/response data classes
│       │   │   ├── ApiService.kt       # Retrofit interface (4 endpoints)
│       │   │   └── RetrofitClient.kt   # Retrofit singleton + OkHttp interceptor
│       │   ├── auth/
│       │   │   └── TokenManager.kt     # SharedPreferences token storage
│       │   ├── ui/
│       │   │   ├── LandingActivity.kt  # Welcome screen
│       │   │   ├── SignUpActivity.kt   # Registration form
│       │   │   ├── SignInActivity.kt   # Login form
│       │   │   └── DashboardActivity.kt # Post-login dashboard
│       │   └── map/
│       │       ├── data/
│       │       │   ├── GatheringPoint.kt        # Data model for map points
│       │       │   ├── MapRepository.kt        # Region + URL + data logic
│       │       │   └── PreferencesManager.kt   # SharedPreferences (location, map file)
│       │       │
│       │       ├── rendering/
│       │       │   ├── MapRenderer.kt          # Loads map + draws markers
│       │       │   └── MapScreenController.kt  # Connects UI with logic
│       │       │
│       │       └── ui/
│       │           ├── MapActivity.kt          # Main map screen
│       │           └── OfflineFeaturesActivity.kt  # Entry point for offline features
│       └── res/
│           ├── layout/                 # XML layouts for each screen
│           ├── raw/                    # static data
│           ├── values/                 # Colors, strings, themes, styles
│           └── drawable/               # Badge backgrounds
├── build.gradle.kts               # Root Gradle config
├── settings.gradle.kts
├── gradle.properties
├── gradle/wrapper/
│   └── gradle-wrapper.properties  # Gradle 8.5
└── README.md
```

---

## Authentication Flow

```
┌──────────┐     ┌──────────┐     ┌──────────┐     ┌───────────┐
│ Landing  │────▶│ Sign Up  │────▶│ Sign In  │────▶│ Dashboard │
│          │────▶│          │     │          │     │           │
│ (welcome │     │ (register│     │ (login   │     │ (welcome  │
│  screen) │     │  form)   │     │  form)   │     │  + cards) │
└──────────┘     └──────────┘     └──────────┘     └───────────┘
                                                        │
                                                   Logout │
                                                        ▼
                                                  ┌──────────┐
                                                  │ Landing  │
                                                  └──────────┘
```

1. **Landing**: If a token exists in SharedPreferences, skips to Dashboard
2. **Sign Up**: POST `/register` → on success → navigate to Sign In
3. **Sign In**: POST `/login` → store JWT token → navigate to Dashboard
4. **Dashboard**: GET `/me` to validate token → display user info + feature cards
5. **Logout**: POST `/logout` → clear token → navigate to Landing

---

## Backend API Usage

All endpoints are served by the Django backend at `http://10.0.2.2:8000` (emulator alias for host localhost).

| Endpoint | Method | Auth Required | Purpose |
|----------|--------|---------------|---------|
| `/register` | POST | No | Create new account |
| `/login` | POST | No | Get JWT tokens |
| `/me` | GET | Yes (Bearer) | Get current user profile |
| `/logout` | POST | Yes (Bearer) | Logout (server acknowledgement) |


### Login response
```json
{
  "message": "Login successful",
  "token": "<access_jwt>",
  "refresh": "<refresh_jwt>",
  "user": { "id", "full_name", "email", "role", ... }
}
```


## Map Flow
```
┌──────────────┐
│ Open Map     │
│ (MapActivity)│
└──────┬───────┘
       │
       ▼
┌──────────────┐
│ Render Cached│
│ Map (if any) │
└──────┬───────┘
       │
       ▼
┌──────────────┐
│ Get Location │
│ (GPS)        │
└──────┬───────┘
       │
       ▼
┌──────────────┐
│ Geocoder     │
│ (country/state)
└──────┬───────┘
       │
       ▼
┌──────────────┐
│ Map Exists ? │
└──────┬───────┘
       │
   ┌───┴───────────────┐
   │                   │
   ▼                   ▼
┌──────────────┐   ┌──────────────┐
│ Use Existing │   │ Download Map │
│ .map file    │   │ (background) │
└──────┬───────┘   └──────┬───────┘
       │                  │
       └──────────┬───────┘
                  ▼
           ┌──────────────┐
           │ Render Map   │
           │ (Mapsforge)  │
           └──────┬───────┘
                  │
                  ▼
           ┌──────────────┐
           │ Get Nearby   │
           │ Points       │
           └──────┬───────┘
                  │
                  ▼
           ┌──────────────┐
           │ Show Markers │
           │ + Nearest    │
           └──────────────┘
```
---

## How to Run

### Prerequisites
- Android Studio (Arctic Fox or newer)
- Android SDK 34
- Backend running on `localhost:8000`

### Steps
1. Open Android Studio
2. Select **Open** → navigate to `mobile/` folder
3. Let Gradle sync (it will download the Gradle wrapper automatically)
4. Start the Django backend: `cd backend && python manage.py runserver`
5. Run the app on an emulator (the app connects to `10.0.2.2:8000`)

### For physical devices
Change `BASE_URL` in `RetrofitClient.kt` from `http://10.0.2.2:8000` to your machine's local IP (e.g., `http://192.168.1.x:8000`) and ensure both devices are on the same network.

---

## Token Handling

- **Storage**: JWT access token stored in `SharedPreferences` (key: `access_token`)
- **Attachment**: OkHttp interceptor automatically adds `Authorization: Bearer <token>` to all requests
- **Validation**: On Dashboard load, `/me` is called to validate the token
- **Expiry**: If `/me` returns 401, the token is cleared and user is redirected to Landing
- **Logout**: Token is removed from SharedPreferences; server `/logout` is called for consistency
- **Refresh token**: Stored but not yet used for silent renewal (can be added in a future milestone)

---

## Implemented vs Placeholder

| Feature | Status |
|---------|--------|
| Landing screen | ✅ Implemented |
| Sign Up (STANDARD/EXPERT) | ✅ Implemented |
| Sign In | ✅ Implemented |
| Dashboard (welcome + user info) | ✅ Implemented |
| Role-dependent expertise field | ✅ Implemented |
| JWT token storage + auto-attach | ✅ Implemented |
| Forum card | 🔲 UI placeholder |
| Help Requests card | 🔲 UI placeholder |
| Profile card | 🔲 UI placeholder |
| Offline Info card | ✅ Implemented |
