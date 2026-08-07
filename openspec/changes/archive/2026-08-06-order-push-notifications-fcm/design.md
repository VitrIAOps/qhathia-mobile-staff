## Context

The staff app is a multi-module Clean Architecture + MVI codebase (`:app`, `core:*`, `features:*`). Auth already persists an OWNER/SELLER JWT that the authenticated Ktor client injects and refreshes automatically (`SessionAuthTokenProvider`). `MainViewModel` observes the session and flips `QhathiaRoot` between the login graph and the `MainShell` (bottom-nav) shell. The Profile tab is a placeholder in `:app` that currently only links to device pairing; a `LogoutUseCase` already exists in `core:domain` but is wired to no UI.

The backend shipped FCM push in Fase 15. **API constraints (`docs/android/api-contract.md` §9.6):**
- `POST /api/v1/notifications/tokens` — JWT `OWNER`/`SELLER`/`ADMIN` (**DEVICE → 403**). Body `{ token, platform: "ANDROID", deviceLabel? }`; `storeId`/`userId` come from the JWT. Upsert by token. Response `204`. The app calls it on every start, on `onNewToken()`, and after `switch-store`.
- `DELETE /api/v1/notifications/tokens` — body `{ token }`, idempotent, `204`. Used for logout / opt-out.
- Push is **data-only, PII-free**: `{ event, orderId, storeId, status }` where `event ∈ {NEW_ORDER, ORDER_TAKEN}`, `status ∈ {PENDING, CONFIRMED, REJECTED}`. Fan-out to all active tokens of the order's store. Google never sees names/products/amounts.
- Push is an accelerator, not the source of truth — the app must still sync `GET /orders?status=PENDING` when the Requests screen opens (that screen is out of scope here).

`google-services.json` is currently at the repo root and is **git-ignored** (`.gitignore:44`). It must move to `app/` so the `google-services` plugin can find it; it stays out of version control.

## Goals / Non-Goals

**Goals:**
- Receive a push for every order created for the store and post a local notification while a merchant is logged in.
- Register the FCM token after login, on authenticated app start, and on Firebase token rotation; unregister it on logout.
- Add a working Logout button on the Profile screen that returns to login.
- Follow existing module/layer patterns exactly (service-per-API in `core:network`, `NetworkResult → Result` at the data boundary, ports + use cases in `core:domain`, new capability as a proper Gradle module).

**Non-Goals:**
- No Requests/Orders screen, order-detail fetch, or `GET /orders/{id}` enrichment — the notification uses generic, PII-free copy derived from `event`/`status` only. (Contract allows an authenticated fetch to enrich; deferred.)
- No `switch-store` re-registration flow (multi-store switching is not yet implemented in the app); re-registration hooks are structured so this is a later addition.
- No in-app notification center, notification history, grouping, or deep-link into a specific order.
- No Firebase Analytics/Crashlytics — only Cloud Messaging.

## Decisions

**1. New `core:notifications` Android-library module (not a feature, not a package in `:app`).** Push handling is app infrastructure (an Android `Service` + notification plumbing), not a UI vertical slice, so it belongs under `core:*` rather than `features:*` — but per project rule 4 it is still a proper Gradle module, never a package inside `:app`. Namespace `com.qhathia.mobile.staff.core.notifications`. It owns `QhathiaMessagingService : FirebaseMessagingService`, the notification channel + `NotificationCompat` builder, and a Hilt module. Its manifest declares the `<service>` (with the `com.google.firebase.MESSAGING_EVENT` intent filter) and the `POST_NOTIFICATIONS` permission, which merge into the app manifest. It depends on `core:domain` (use cases) and `core:common`. *Alternative considered:* put the service in `:app` — rejected by rule 4 and to keep `:app` a thin shell. *Alternative:* a `features/notifications` module — rejected because there is no screen/ViewModel; it is cross-cutting infrastructure.

**2. Ports for both the backend token API and the Firebase SDK, keeping the SDK out of domain.** `core:domain` gets:
- `NotificationTokenRepository` — `suspend fun registerToken(token, deviceLabel?): Result<Unit>` and `suspend fun unregisterToken(token): Result<Unit>`.
- `PushTokenProvider` — `suspend fun currentToken(): String?` and `suspend fun deleteToken()`, abstracting `FirebaseMessaging`. Implemented in `core:data` (or `core:notifications`) over `FirebaseMessaging.getInstance()` using `kotlinx-coroutines-play-services` `.await()`.
- `RegisterPushTokenUseCase` — fetches the current token via `PushTokenProvider` and registers it; fail-open (a failure never blocks login/startup).
- `UnregisterPushTokenUseCase` — retrieves the current token, calls `unregisterToken`, then `PushTokenProvider.deleteToken()`; fail-open.

This mirrors the existing `AuthTokenProvider` boundary pattern (a network-facing port implemented in data) and keeps `core:domain` framework-free.

**3. `NotificationApiService` in `core:network` on the authenticated client.** Two suspend methods returning `NetworkResult<Unit>` via `runCatching { ... }.toNetworkResult()`, exactly like `DeviceApiService`. `RegisterTokenRequestDto(token, platform, deviceLabel)` and `UnregisterTokenRequestDto(token)` are `@Serializable` with `@SerialName`. Both `204` responses map to `Result.success(Unit)`. Provided in `NetworkModule` with `@AuthenticatedClient`, so the JWT (and its refresh) is injected automatically. `NotificationTokenRepositoryImpl` in `core:data` maps `NetworkResult → Result`, bound in `DataModule` with `@Binds @Singleton`.

**4. Token-registration lifecycle.** Registration must run only while authenticated (JWT required):
- **After login:** `LoginViewModel` currently emits `NavigateToHome` on success. Trigger `RegisterPushTokenUseCase` here (fire-and-forget in `viewModelScope`), or from a post-login hook in `:app`. Chosen: invoke it from `:app` (`QhathiaRoot`/`MainViewModel`) reacting to the session becoming non-null, so registration is centralized and also covers **authenticated app start** with one code path. `MainViewModel` gains `RegisterPushTokenUseCase` and calls it when `authState` transitions to `Authenticated`.
- **On token rotation:** `QhathiaMessagingService.onNewToken()` calls `RegisterPushTokenUseCase` — but only if a session exists (the use case/repository no-ops when there is no JWT; the authenticated call would 401 and fail-open otherwise).
- *Alternative considered:* registering inside `AuthRepositoryImpl.login()` — rejected to keep the data layer free of Firebase and to reuse one path for login + restart.

**5. Notification content is generic and PII-free, built from the data payload only.** `onMessageReceived` reads `event`/`status` and posts a `NotificationCompat` notification with localized copy: `NEW_ORDER` → title "Nueva orden de compra" / body "Tienes una nueva orden por atender"; `ORDER_TAKEN` (`CONFIRMED`/`REJECTED`) → an order-updated message. Content-tap opens `MainActivity` via a `PendingIntent` (`FLAG_IMMUTABLE`). No order id is shown and no network fetch is made in this change. This respects the "Google never sees PII" design and avoids depending on an Orders API that does not yet exist in the app. Data messages are delivered to `onMessageReceived` even in the background (the backend sends **data-only**, no `notification` block), so the app always builds the notification itself — required for the localized, no-PII copy.

**6. Explicit logout ordering (unregister push → invalidate session server-side → clear local).** Both the `DELETE /notifications/tokens` and `POST /auth/logout` calls need the JWT, so they MUST run **before** the local session is cleared. `LogoutUseCase` is extended to, in order: (1) run `UnregisterPushTokenUseCase` (fail-open); (2) call `AuthRepository.logout(refreshToken)` to invalidate the refresh token server-side (fail-open); (3) `sessionRepository.clearSession()`. The push-unregister runs **first** so that, if the access token happens to be expired, the bearer plugin can still refresh against the not-yet-invalidated refresh token before step (2) revokes it. Every network step is fail-open — logout must complete even offline. Clearing the session flips `MainViewModel.authState` to `Unauthenticated`, and `QhathiaRoot` recomposes to the login graph automatically — no manual navigation needed. `LogoutUseCase` reads the refresh token from `sessionRepository.observeSession().first()` before clearing.

**6a. `POST /auth/logout` runs on the authenticated client.** Unlike `login`/`refresh` (public client, deliberately kept off the bearer plugin so a failed refresh can't recurse), `logout` requires the JWT and carries `{ refreshToken }` in the body. `AuthApiService` gains a `logout(LogoutRequestDto): NetworkResult<Unit>` that uses an injected `@AuthenticatedClient` — the class doc is updated to note login/refresh use the public client while logout uses the authenticated one. `AuthRepository` gains `suspend fun logout(refreshToken: String): Result<Unit>`; `AuthRepositoryImpl` maps `204`/`NetworkResult → Result` and treats any failure as a swallowed best-effort (the use case ignores the result). *Alternative considered:* a separate `LogoutApiService` — rejected as over-engineering for a single call that is conceptually part of auth.

**7. Logout button stays on the existing Profile screen in `:app` via a small `ProfileViewModel`.** The Profile screen already lives in `:app` as a placeholder, and `:app` already has precedent for a use-case-backed ViewModel (`MainViewModel` → `ObserveSessionUseCase`). Adding a `ProfileViewModel` (`@HiltViewModel`, injects `LogoutUseCase`) with a confirm-dialog + Logout button is the smallest change that satisfies "add logout to the profile screen." *Alternative considered:* extract a `features/profile` module — deferred; it would be a larger refactor unrelated to notifications, and the task explicitly scopes logout onto the existing profile screen.

**8. Firebase / Gradle wiring.** Add to the version catalog: `firebase-bom`, `firebase-messaging` (no explicit version, via BOM), the `google-services` plugin, and `kotlinx-coroutines-play-services`. Apply `com.google.gms.google-services` in `:app` (plugin declared `apply false` at the root). `google-services.json` moves to `app/`. Firebase auto-initializes through the `FirebaseInitProvider` bundled with `firebase-common`, using resources the `google-services` plugin generates from the JSON — so no manual `FirebaseApp.initializeApp()` is needed. `core:notifications` depends on `firebase-messaging`; `:app` applies the plugin so the JSON-derived resources land in the merged app.

**9. `POST_NOTIFICATIONS` runtime permission (API 33+).** Declared in the `core:notifications` manifest (merges up). Requested at runtime on first authenticated entry to `MainShell` using the Compose `rememberLauncherForActivityResult` / `ActivityResultContracts.RequestPermission` API (minSdk 26, so guard with `Build.VERSION.SDK_INT >= 33`). If denied, token registration still proceeds (the OS simply suppresses the tray notification); no hard block.

**Module dependency additions (respecting the graph):**
- `core:notifications → core:domain, core:common` (+ `firebase-messaging`, Hilt)
- `core:data → firebase-messaging` (or host `PushTokenProvider` impl in `core:notifications`; chosen: keep the Firebase-backed `PushTokenProvider` in `core:notifications`, bound in its Hilt module, so `core:data` stays Firebase-free)
- `core:network` gains `NotificationApiService` (no new module deps)
- `:app → core:notifications`, applies `google-services`

## Risks / Trade-offs

- **[Firebase build config in CI]** `google-services.json` is git-ignored, so CI/other clones lack it and `assembleDebug` fails when the plugin can't find it. → Document that the file must be provided out-of-band (CI secret / local copy in `app/`). A follow-up can add a placeholder-or-secret injection step. This change only moves the existing file into `app/`.
- **[Generic notification copy]** Without an order fetch, the notification can't show customer/amount. → Matches the PII-free contract and the task's "atender de forma reactiva"; enriching via `GET /orders/{id}` is a documented future enhancement once an Orders capability exists.
- **[Token registered before permission granted]** On API 33+ a token may register while `POST_NOTIFICATIONS` is denied, so pushes arrive but are suppressed. → Acceptable; delivery resumes when the user later grants permission, and re-registration on next start keeps the token fresh. No data loss (push is an accelerator, not source of truth).
- **[Logout with no network]** `DELETE /notifications/tokens` and `POST /auth/logout` may fail offline, leaving a stale active token and a still-valid refresh token server-side. → Both calls are fail-open; local logout still clears the session. The backend's daily token purge and `UNREGISTERED` hygiene reap the FCM token, and the refresh token expires on its own TTL; `deleteToken()` locally stops FCM delivery to that install regardless.
- **[DEVICE role 403]** If a DEVICE token ever reached this app, registration would 403. → This app only ever holds OWNER/SELLER sessions; fail-open swallows it.
- **[Multi-store switch not covered]** Re-registration after `switch-store` is a §9.6 expectation but multi-store switching isn't implemented app-side yet. → Structured so a single `RegisterPushTokenUseCase` call can be added to the future switch flow.

## Migration Plan

Additive. New module + new authenticated calls behind the existing session; one behavior change (logout now unregisters the push token and invalidates the session server-side before clearing local state). No schema/data migration. Deploy steps: (1) place `google-services.json` in `app/`; (2) build with the `google-services` plugin. Rollback = revert the change and move the JSON back; no persisted app state is introduced beyond the Firebase token (owned by the SDK).

## Open Questions

- Should the local notification later be enriched via an authenticated `GET /orders/{id}` once an Orders capability lands? (Deferred; generic copy for now.)
