## Why

Store owners need to react to purchase orders as they arrive, but the staff app has no way to alert them when the kiosk tablet creates an order — they must open the app and poll the Requests tab. The backend shipped FCM push in Fase 15 (`api-contract.md` §9.6); the app now needs to register for those pushes and surface a notification for every incoming order, but only while a merchant is logged in.

## What Changes

- Integrate **Firebase Cloud Messaging** into the app: add the Firebase BOM + `firebase-messaging`, apply the `google-services` plugin, and move `google-services.json` from the repo root into `app/`.
- Register the device's FCM token with the backend (`POST /api/v1/notifications/tokens`) after login, on app start while authenticated, and whenever Firebase rotates the token (`onNewToken`).
- Add a `FirebaseMessagingService` that receives the **data-only, PII-free** push (`{event, orderId, storeId, status}`) and posts a **local** notification per order (`NEW_ORDER` → "nueva orden de compra"; `ORDER_TAKEN` → order confirmed/rejected). Tapping the notification opens the app.
- Stop notifications on logout: unregister the token with the backend (`DELETE /api/v1/notifications/tokens`) **before** clearing the session, then call Firebase `deleteToken()`.
- Invalidate the session server-side on logout via `POST /api/v1/auth/logout` (body `{ refreshToken }`), so the refresh token cannot be reused after sign-out.
- Add a real **Logout** button to the Profile screen (currently a placeholder), returning the app to the login screen.
- Declare the `POST_NOTIFICATIONS` permission and request it at runtime (API 33+).

## Capabilities

### New Capabilities
- `order-notifications`: FCM token lifecycle (register on login/start/rotation, unregister on logout) and display of a local notification for each purchase-order push received while authenticated.

### Modified Capabilities
- `session-management`: logout becomes an explicit, user-triggered action that unregisters the push token before clearing the session (adds a "Sign out" requirement; the existing persistence/refresh requirements are unchanged).

## Impact

- **New module:** `core:notifications` (Android library) — `FirebaseMessagingService`, notification channel + builder, FCM token provider, Hilt module, and a manifest declaring the service + `POST_NOTIFICATIONS` permission.
- **`core:domain`:** `NotificationTokenRepository` port, `PushTokenProvider` port, `RegisterPushTokenUseCase`, `UnregisterPushTokenUseCase`; `AuthRepository` gains `logout(refreshToken)`; `LogoutUseCase` extended to unregister the push token and invalidate the session server-side before clearing.
- **`core:network`:** `NotificationApiService` (`POST`/`DELETE /api/v1/notifications/tokens`) + request DTOs on the authenticated client; `AuthApiService` gains `logout` (`POST /api/v1/auth/logout`) on the authenticated client + `LogoutRequestDto`.
- **`core:data`:** `NotificationTokenRepositoryImpl` (+ mapper) and `FirebaseMessaging`-backed `PushTokenProvider`, bound in `DataModule`.
- **`:app`:** apply `google-services`; add `google-services.json`; declare/request `POST_NOTIFICATIONS`; trigger token registration on login + authenticated startup; add a `ProfileViewModel` + Logout button wired to `LogoutUseCase`.
- **Build:** version catalog gains Firebase BOM, `firebase-messaging`, the `google-services` plugin, and `kotlinx-coroutines-play-services`.
- **APIs consumed:** `POST /api/v1/notifications/tokens`, `DELETE /api/v1/notifications/tokens` (§9.6), `POST /api/v1/auth/logout` (§1). No order-detail fetch in this change (generic, PII-free notification copy).
