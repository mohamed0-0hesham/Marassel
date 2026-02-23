# Architectural Decisions — Marassel

This document records the key architectural decisions made during the development of Marassel, the reasoning behind them, and their trade-offs.

---

## 1. MVI (Model-View-Intent) as the UI Architecture

**Decision:** All screens use a strict MVI pattern implemented via `BaseViewModel<State, Event, Effect>`.

**Context:** The app has several screens (Auth, Username, ChatRoom) with complex interaction flows — form validation, optimistic UI updates, background work status, and one-shot navigation side-effects. A predictable, unidirectional data flow simplifies reasoning about state.

**Implementation:**
- `UiState` — a single immutable data class per screen, exposed as `StateFlow`.
- `UiEvent` — sealed interface representing every user action.
- `UiEffect` — one-shot side-effects (navigation, snackbar, launching external intents) delivered via a `Channel`.
- `BaseViewModel` provides `setState`, `setEffect`, and a coroutine-safe `launch` helper.

**Trade-offs:**
- (+) State is always a single source of truth; UI simply renders it.
- (+) Effects are buffered and consumed exactly once, avoiding lost navigations.
- (−) Boilerplate: each screen needs a State data class, Event sealed interface, and Effect sealed interface.
- (−) Very simple screens (like a static info page) don't benefit from the ceremony.

---

## 2. Clean Architecture with Domain Layer

**Decision:** The codebase is split into three layers: `data`, `domain`, and `ui`, with dependencies flowing inward (ui → domain → data).

**Context:** Separating business logic from framework code makes use cases independently testable and keeps ViewModels thin.

**Key boundaries:**
- **`domain/repository`** — interfaces only; no Firebase, Android, or DataStore imports.
- **`domain/usecase`** — orchestrates repository calls, validation, and returns sealed result types (e.g., `SendMessageResult`, `DeleteResult`).
- **`data/repository`** — implements domain interfaces using Firebase SDKs and DataStore.
- **`ui`** — ViewModels depend only on use cases and repository interfaces (injected via Hilt).

**Trade-offs:**
- (+) Use cases like `SendMessageUseCase` can be unit-tested with pure mocks — no Android framework needed.
- (+) Swapping Firebase for another backend would only require new `data` implementations.
- (−) Extra indirection for simple operations (e.g., `observeTypingUsers` is a thin pass-through).

---

## 3. Firebase Realtime Database (Not Firestore)

**Decision:** Messages are stored in Firebase Realtime Database rather than Cloud Firestore.

**Context:** For a real-time chat application, RTDB offers lower latency for small-payload real-time syncing (sub-100ms), simpler listener-based APIs, and built-in `onDisconnect` hooks which are used for typing indicators.

**Data paths:**
- `marassel/messages` — all chat messages, indexed by `timestamp`.
- `marassel/typing` — ephemeral typing status per user, auto-cleaned via `onDisconnect().removeValue()`.

**Trade-offs:**
- (+) Very low latency real-time sync; ideal for chat.
- (+) `onDisconnect` natively cleans up typing status if the app crashes.
- (−) No built-in complex queries (vs. Firestore's composite indexes).
- (−) Single-region only (RTDB doesn't support multi-region natively).
- (−) Scaling beyond ~100K concurrent connections requires sharding.

---

## 4. DataStore for Local Message Queue and User Profiles

**Decision:** Use Jetpack DataStore (Preferences) for both the user profile cache and the local pending-message queue.

**Context:** The app needs to persist two categories of small data: user profile fields (username, email, photo URL keyed by UID) and pending/failed messages that haven't been confirmed by Firebase yet. Both are key-value shaped and small enough that a lightweight store suffices.

**Implementation:**
- Messages are serialized to JSON via `kotlinx.serialization` and stored under keys prefixed with `pending_msg_`.
- Profiles are stored as individual preference keys: `profile_{uid}_username`, etc.
- `MessageRepositoryImpl.observeMessages()` combines the Firebase real-time stream with the local DataStore stream, deduplicating by `localId`.

**Trade-offs:**
- (+) No Room dependency — keeps the dependency graph smaller.
- (+) DataStore is coroutine-native and thread-safe.
- (−) Not suitable for large datasets or complex queries; if message history caching is needed, Room would be better.
- (−) JSON serialization inside DataStore is unconventional; a proto DataStore or Room would be more type-safe at scale.

---

## 5. WorkManager for Background Message Sending

**Decision:** All message sends (text and media) are dispatched through WorkManager, not directly from the ViewModel coroutine scope.

**Context:** Sending a message involves a network call that should survive process death, activity recreation, and network transitions. WorkManager provides guaranteed execution with constraints, retry policies, and foreground service support.

**Implementation:**
- `MessageSendOrchestrator` builds and enqueues work requests.
- **Text messages:** a single `SendMessageWorker` with network constraint.
- **Media messages:** `UploadMediaWorker` → `SendMessageWorker` chained via `beginUniqueWork().then()`.
- Exponential backoff with 10-second initial delay; max 3 attempts before marking `FAILED`.
- Upload progress is reported via `setProgress()` and observed by `WorkInfoMessageBridge`.
- Foreground notifications (with cancel/retry actions) keep the user informed.

**Trade-offs:**
- (+) Messages survive app kill and are retried automatically.
- (+) Upload progress is observable from any screen via `WorkManager.getWorkInfoByIdLiveData`.
- (−) WorkManager has minimum 15-second backoff (overridden here to 10s via expedited work).
- (−) Chaining upload → send means if upload succeeds but send fails, the upload isn't re-done on retry (which is correct but adds complexity).

---

## 6. Optimistic UI with Local-First Message Queue

**Decision:** Messages appear in the UI immediately as `PENDING` before they are confirmed by Firebase.

**Flow:**
1. User taps send → `SendMessageUseCase` validates and saves a `PENDING` `MessageEntity` to DataStore.
2. `observeMessages()` merges DataStore locals with the Firebase real-time stream.
3. WorkManager picks up the message and sends it to Firebase.
4. Once Firebase confirms, the real-time listener emits the message with status `SENT` and a `firebaseKey`.
5. The merge logic in `MessageRepositoryImpl` deduplicates by `localId`, preferring the Firebase version.

**Trade-offs:**
- (+) Instant feedback — the message appears in the list within milliseconds.
- (+) Failed messages show a retry button; the user never loses a message silently.
- (−) Brief duplication is possible if the merge timing is unlucky (mitigated by `distinctBy { localId }`).
- (−) The DataStore-based queue doesn't scale well beyond a few hundred pending messages.

---

## 7. Hilt for Dependency Injection

**Decision:** Use Dagger Hilt for compile-time dependency injection across the entire app.

**Modules:**
- `AppModule` — DataStore, `UsernameValidator`, `NetworkMonitor` binding.
- `FirebaseModule` — `FirebaseAuth`, `FirebaseDatabase`, `FirebaseStorage` singletons.
- `RepositoryModule` — binds interface → implementation for Auth, Message, User repositories.
- `WorkerModule` — provides `WorkManager` and `WorkInfoMessageBridge`.

**Workers use `@HiltWorker`** with `@AssistedInject`, and `MarasselApplication` provides a custom `HiltWorkerFactory` via `Configuration.Provider`.

**Trade-offs:**
- (+) Compile-time safety; errors are caught at build time, not runtime.
- (+) Standard Android ecosystem choice; well-documented and supported.
- (−) KSP/annotation processing adds to build time.
- (−) Testing requires either `@HiltAndroidTest` (for instrumented) or manual mock injection (for unit tests).

---

## 8. Single-Activity Navigation with Compose Navigation

**Decision:** The app uses a single `MainActivity` with Jetpack Compose Navigation (`NavHost`) for all screen transitions.

**Routes:** `auth` → `username/{suggestedName}` → `chat_room` → `media_viewer/{mediaUrl}`

**Start destination** is resolved dynamically by `StartDestinationViewModel`, which observes `AuthRepository.observeAuthState()`:
- Signed in → `chat_room`
- Not signed in → `auth`

**Trade-offs:**
- (+) Smooth Compose-native transitions with shared element support potential.
- (+) Type-safe route arguments (via `navArgument`).
- (−) Deep linking requires explicit route registration.
- (−) The `StartDestinationViewModel` emits `null` initially, causing a brief blank frame until auth state resolves.

---

## 9. Sealed Result Types Instead of Exceptions

**Decision:** Use cases return sealed class hierarchies (e.g., `SendMessageResult`, `DeleteResult`, `RetryMessageResult`) rather than throwing exceptions.

**Example:**
```kotlin
sealed class SendMessageResult {
    data class Success(val message: MessageEntity) : SendMessageResult()
    data class ValidationFailed(val reason: ...) : SendMessageResult()
    data object NotAuthenticated : SendMessageResult()
    data object NotOnboarded : SendMessageResult()
    data class StorageError(val cause: Throwable) : SendMessageResult()
}
```

**Trade-offs:**
- (+) Exhaustive `when` blocks force the ViewModel to handle every case explicitly.
- (+) No risk of uncaught exceptions crashing the app.
- (+) Each result variant carries context-specific data (e.g., `ValidationFailed` includes the reason).
- (−) More verbose than `Result<T>` for simple success/failure cases.

---

## 10. Notification Strategy

**Decision:** Two notification channels with different importance levels, plus actionable notification buttons.

| Channel | Importance | Purpose |
|---------|-----------|---------|
| `message_sending` | LOW (silent) | Ongoing upload/send progress |
| `message_failed` | DEFAULT (sound) | Failed message alerts with Retry action |

**Implementation:**
- Channels are created in `Application.onCreate()` via `NotificationChannelSetup`.
- `UploadMediaWorker` uses `setForeground()` with `FOREGROUND_SERVICE_TYPE_DATA_SYNC` to show progress.
- Failed notifications include a `RetryMessageReceiver` broadcast action and an "open app" content intent.

**Trade-offs:**
- (+) Users are never surprised by silent failures.
- (+) Retry from notification works without opening the app.
- (−) Requires `POST_NOTIFICATIONS` runtime permission on API 33+.
- (−) The `RetryMessageReceiver` currently re-launches the Activity rather than directly re-enqueueing work (architectural simplification).

---

## 11. Testing Strategy

**Layers tested:**

| Layer | Framework | What's Covered |
|-------|-----------|----------------|
| **Use cases** | JUnit + MockK + coroutines-test | Validation, auth guards, repository delegation, result mapping |
| **Repository** | JUnit + MockK | Merge logic, DataStore interaction, delegation to data sources |
| **Workers** | Robolectric + TestListenableWorkerBuilder | Input deserialization, retry logic, status updates |
| **WorkDataUtils** | Robolectric (for `android.net.Uri`) | Round-trip serialization of all worker input/output |
| **UI components** | Compose Testing (createComposeRule) | Input bar, message bubbles, context menu, status indicators |

**Design for testability:**
- Repository interfaces allow full mocking in use case tests.
- `WorkDataUtils` is a pure object with static functions, easily testable.
- ViewModels depend on use cases (not repositories directly), so tests at the use-case level cover business logic without ViewModel complexity.

---

## Summary

The architecture prioritizes **reliability** (WorkManager, optimistic queue, sealed results), **testability** (clean layer boundaries, interface-based DI), and **real-time responsiveness** (Firebase RTDB, local-first UI). The main trade-off is additional boilerplate from MVI + Clean Architecture, which is justified by the complexity of the chat domain (background work, network state, message lifecycle).
