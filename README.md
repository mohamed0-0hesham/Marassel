# Marassel

A real-time chat application built with **Jetpack Compose**, **Firebase**, and **WorkManager**. Users can send text and media messages, see typing indicators, and receive background notifications for failed sends.

---

## Features

- **Real-time messaging** — text and media (image/video) via Firebase Realtime Database
- **Google & Email/Password authentication** — powered by Firebase Auth + Credential Manager
- **Media uploads** — camera capture or photo picker, uploaded to Firebase Storage with progress tracking
- **Background send/upload** — WorkManager handles message delivery even when the app is backgrounded
- **Foreground notifications** — upload progress and failed-message alerts with retry actions
- **Typing indicators** — live "X is typing…" status synced through Firebase
- **Pagination** — older messages loaded on scroll with end-of-history detection
- **Offline-first local queue** — messages are persisted in DataStore and merged with the Firebase stream
- **MVI architecture** — unidirectional data flow with `BaseViewModel<State, Event, Effect>`

---

## Prerequisites

| Tool | Minimum version | Notes |
|------|----------------|-------|
| **Android Studio** | Ladybug (2024.2+) | Kotlin 2.1 & Compose compiler plugin support |
| **JDK** | 17 | Set in `compileOptions` and `kotlinOptions` |
| **Gradle** | 9.2.1 | Bundled via the wrapper (`./gradlew`) |
| **Android SDK** | API 27 – 36 | `minSdk = 27`, `compileSdk = 36` |
| **Firebase project** | — | Realtime Database, Storage, and Auth enabled |
| **Google Cloud OAuth** | — | Web client ID for Google Sign-In |

---

## Getting Started

### 1. Clone the repository

```bash
git clone https://github.com/<your-org>/Marassel.git
cd Marassel
```

### 2. Set up Firebase

1. Go to the [Firebase Console](https://console.firebase.google.com/) and create a new project (or use an existing one).
2. Add an **Android app** with the package name:
   ```
   com.hesham0_0.marassel
   ```
3. Download the generated `google-services.json` and place it at:
   ```
   app/google-services.json
   ```
   > This file is gitignored for security — every developer needs their own copy.

4. In the Firebase Console, enable the following services:
   - **Authentication** → Sign-in providers: **Email/Password** and **Google**
   - **Realtime Database** → Create a database (start in test mode or apply your own rules)
   - **Storage** → Set up Cloud Storage (start in test mode or apply your own rules)

### 3. Configure Google Sign-In

The app uses Credential Manager with a **Web client ID**. The current ID is hardcoded in `GoogleSignInHelper.kt`:

```kotlin
const val WEB_CLIENT_ID = "62921054375-..."
```

To use your own Firebase project:

1. Go to **Firebase Console → Authentication → Sign-in method → Google** and enable it.
2. Copy the **Web client ID** (not the Android client ID) from the Google provider configuration.
3. Replace the `WEB_CLIENT_ID` constant in `GoogleSignInHelper.kt`.

### 4. Build and run

```bash
# Debug build
./gradlew assembleDebug

# Install on connected device/emulator
./gradlew installDebug
```

Or open the project in Android Studio and press **Run ▶**.

### 5. Run tests

```bash
# Unit tests (JVM)
./gradlew testDebugUnitTest

# Instrumented tests (requires device/emulator)
./gradlew connectedDebugAndroidTest
```

---

## Project Structure

```
app/src/main/java/com/hesham0_0/marassel/
├── core/
│   ├── mvi/              # BaseViewModel, UiState, UiEvent, UiEffect
│   └── network/          # NetworkMonitor (ConnectivityManager flow)
├── data/
│   ├── remote/           # FirebaseMessageDataSource, FirebaseStorageDataSource, DTOs
│   └── repository/       # Repository implementations (Auth, Message, User)
├── di/                   # Hilt modules (App, Firebase, Repository, Worker)
├── domain/
│   ├── model/            # MessageEntity, UserEntity, AuthUser, enums
│   ├── repository/       # Repository interfaces
│   └── usecase/          # Use cases (Send, Delete, Retry, Observe, Validate…)
├── ui/
│   ├── auth/             # Auth screen, ViewModel, Google helper
│   ├── chat/             # Chat room screen, ViewModel, components
│   ├── media/            # Media picker, camera capture, viewer
│   ├── navigation/       # NavGraph, Screen routes, extensions
│   ├── theme/            # Colors, typography, shapes, spacing
│   └── username/         # Onboarding username screen, ViewModel, components
├── worker/               # WorkManager workers, orchestrator, notifications
└── MarasselApplication.kt
```

---

## Firebase Database Rules (Recommended)

Below is a minimal starting point. Tighten as needed for production.

```json
{
  "rules": {
    "marassel": {
      "messages": {
        ".read": "auth != null",
        ".write": "auth != null",
        ".indexOn": ["timestamp"]
      },
      "typing": {
        ".read": "auth != null",
        ".write": "auth != null"
      }
    }
  }
}
```

---

## Environment Notes

- **ProGuard / R8** — enabled for release builds. Rules are in `app/proguard-rules.pro`.
- **WorkManager** — initialized manually via `Configuration.Provider` in `MarasselApplication` (the default `WorkManagerInitializer` is disabled in the manifest).
- **Notification channels** — created at app startup in `NotificationChannelSetup`.
- **DataStore** — a single `user_preferences` instance stores local message queue and user profiles.

---

## License

[MIT](LICENSE) © 2026 Mohamed Hesham
