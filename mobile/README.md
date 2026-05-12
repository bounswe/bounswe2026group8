# Mobile — Android App (Kotlin)

Native Android client for the Neighborhood Emergency Preparedness Hub. Consumes the same Django REST API as the React web frontend.

---

## Prerequisites

- Android Studio (Hedgehog or newer)
- Android SDK 34
- Backend running (see root README for setup options)

---

## How to Run

### Emulator (no configuration needed)

1. Open Android Studio → **Open** → select the `mobile/` folder
2. Let Gradle sync complete
3. Start the backend (`cd backend && python manage.py runserver`)
4. Run the app on an emulator

Debug builds automatically connect to `http://10.0.2.2:8000` — the Android emulator's alias for `localhost` on the host machine.

### Physical device (same network as your machine)

Find your machine's local IP (`ipconfig` on Windows, `ifconfig` on macOS/Linux), then add it to `mobile/local.properties` (create the file if it doesn't exist):

```
sdk.dir=/path/to/your/Android/Sdk
BASE_URL=http://192.168.1.x:8000/
```

Rebuild and install the APK on the device. Both devices must be on the same network.

> On Windows you may need to allow port 8000 through the firewall:
> ```
> netsh advfirewall firewall add rule name="Django 8000" dir=in action=allow protocol=TCP localport=8000
> ```

---

## Building the APK

**From Android Studio:**
Build → Build Bundle(s) / APK(s) → Build APK(s)

Output: `app/build/outputs/apk/debug/app-debug.apk`

**From command line:**
```bash
cd mobile
bash gradlew assembleDebug --no-daemon   # Windows (Git Bash)
./gradlew assembleDebug --no-daemon      # macOS/Linux
```

---

## Running Tests

### Unit tests

```bash
cd mobile
bash gradlew testDebugUnitTest --no-daemon   # Windows
./gradlew testDebugUnitTest --no-daemon      # macOS/Linux
```

### Instrumented tests (requires a running emulator or connected device)

```bash
cd mobile
bash gradlew connectedDebugAndroidTest --no-daemon
```

---

## Project Structure

```
mobile/
├── app/
│   ├── build.gradle.kts               App dependencies and build config
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/bounswe2026group8/emergencyhub/
│       │   ├── api/
│       │   │   ├── ApiService.kt          Retrofit interface (all endpoints)
│       │   │   ├── AuthModels.kt          Auth request/response data classes
│       │   │   ├── ProfileModels.kt       Profile, resource, expertise data classes
│       │   │   ├── ForumModels.kt         Forum post/comment data classes
│       │   │   ├── HelpRequestModels.kt   Help request/offer data classes
│       │   │   ├── BadgeModels.kt         Badge data classes
│       │   │   ├── StaffModels.kt         Staff/moderation data classes
│       │   │   └── RetrofitClient.kt      Retrofit singleton + auth interceptor
│       │   ├── auth/
│       │   │   ├── TokenManager.kt        JWT storage in SharedPreferences
│       │   │   └── HubManager.kt          Hub selection persistence
│       │   ├── ui/                        Activities for each screen
│       │   ├── mesh/                      Offline mesh networking (BLE + Wi-Fi Direct)
│       │   ├── map/                       OSMDroid offline map support
│       │   ├── fcm/                       Firebase Cloud Messaging integration
│       │   └── util/                      Helpers (image upload, time formatting, etc.)
│       └── res/
│           ├── layout/                    XML layouts
│           ├── values/                    Strings, colors, themes
│           └── values-{tr,es,zh}/         Translated string resources
├── build.gradle.kts
├── settings.gradle.kts
├── local.properties                   SDK path + optional BASE_URL override (git-ignored)
└── gradle/wrapper/gradle-wrapper.properties  (Gradle 8.5)
```

---

## Screens

| Screen | Description |
|--------|-------------|
| Landing | Welcome screen; auto-skips to Dashboard if token exists |
| Sign Up | Registration form with role selector and expertise category for Expert role |
| Sign In | Login form |
| Dashboard | Feature cards (Forum, Help Requests, Profile, Offline Info, Map) |
| Forum | Post list with filters; create, view, vote, comment, repost |
| Post Detail | Full post with comments and voting |
| Help Requests | List with category and hub filters |
| Help Request Detail | Detail view, comments, expert take-on, resolve |
| Create Help Request | Form with location picker |
| Profile | View and edit profile, resources, expertise fields |
| My Badges | Earned badges and progress |
| Settings | Notification and language preferences |
| Offline Info | First-aid guides and gathering point map (works without internet) |
| Mesh Archive | Offline peer-to-peer messages received via BLE/Wi-Fi Direct |
| Staff Dashboard | Moderation tools (visible to staff roles only) |

---

## Authentication Flow

1. **Landing** — if a token exists in SharedPreferences, skip directly to Dashboard
2. **Sign Up** — POST `/register` → on success → navigate to Sign In
3. **Sign In** — POST `/login` → store JWT access token → navigate to Dashboard
4. **Dashboard** — GET `/me` validates token; 401 clears token and returns to Landing
5. **Logout** — POST `/logout` → clear token → navigate to Landing

The OkHttp interceptor in `RetrofitClient` automatically attaches `Authorization: Bearer <token>` to every request.

---

## Build Variants

| Variant | `BASE_URL` | Notes |
|---------|-----------|-------|
| `debug` | `http://10.0.2.2:8000/` (or `local.properties` override) | Development |
| `release` | `https://emergencyhub.duckdns.org/api/` | Production |

---

## Firebase (optional)

Push notifications require a `google-services.json` file in `mobile/app/`. Without it the app builds and runs normally — only FCM push notifications are disabled.

To set up: download `google-services.json` from your Firebase project console and place it at `mobile/app/google-services.json`.
