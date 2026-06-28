## Why

The Qhathia staff app currently has no way for a merchant to authenticate. Without login there is no session, no JWT, and no access to any store-scoped feature. This is the gateway capability that every other operational feature (inventory, orders) depends on, so it must land first.

## What Changes

- Add a login screen where a merchant signs in with **email + password**, integrated against `POST /api/v1/auth/login`.
- Add **client-side validation** (email format, non-empty password) before hitting the network, plus mapping of API errors (401 invalid credentials, 400 validation, network/timeout) to user-facing messages.
- Add a **session layer** that persists and exposes the access token, refresh token, user identity, and active store ("capabilities") for the rest of the app.
- Add **token refresh** plumbing against `POST /api/v1/auth/refresh` (token rotation) so an expired access token is renewed transparently.
- Add **navigation to a Home placeholder** on success (the Home screen itself is out of scope — only the destination route).
- Establish the foundational **Gradle modules** the app needs to support a layered feature: `core:common`, `core:network`, `core:domain`, `core:data`, `core:ui`, `core:testing`, and `features:auth`. The project is currently single-module (`:app` only).
- Stand up the shared **Ktor client**, `NetworkResult` error model, and the secure session datastore as reusable core infrastructure.

Non-goals: Home screen implementation, password change/recovery, registration (closed onboarding, admin-only), multi-store switch UI, biometric login.

## Capabilities

### New Capabilities
- `merchant-auth`: Email/password login for merchants against the Auth API, including request validation, error mapping, and the loading/disabled UI states.
- `session-management`: Persistence and lifecycle of the authenticated session — access token, refresh token, user, active store — plus transparent access-token refresh via token rotation.

### Modified Capabilities
<!-- None — no existing specs in openspec/specs/. -->

## Impact

- **New modules:** `core:common`, `core:network`, `core:domain`, `core:data`, `core:ui`, `core:testing`, `features:auth` (created as proper Gradle subprojects; `settings.gradle.kts` updated).
- **APIs consumed:** `POST /api/v1/auth/login`, `POST /api/v1/auth/refresh` (base URL `http://87.99.156.164`, plain HTTP — requires cleartext traffic config).
- **Build/config:** version catalog already declares the needed Ktor/Hilt/DataStore/serialization entries; `:app` gains dependencies on the new modules, `BASE_URL`/`DEBUG` BuildConfig fields, and `INTERNET` + `ACCESS_NETWORK_STATE` manifest permissions.
- **Dependencies added to use:** Hilt, Ktor (CIO), kotlinx-serialization, DataStore + Keystore-backed encryption for token-at-rest, Navigation Compose.
- **Security:** tokens stored encrypted at rest; password never logged; Ktor `Logging` set to `NONE` in release.
