## Why

The 2026-11-03 QA regression (`docs/handoff/android-regresion-2026-11-03.md` §3.1) finds six problems in sign-in, the session and the security screen. The backend already does its part (Fase 36):

- **F-48** — A wrong current password in "Cambiar contraseña" answers `401 CURRENT_PASSWORD_INCORRECT`. The bearer plugin treats every 401 as an expired token: it refreshes and repeats the request. If that refresh failed for lack of network, the merchant would be signed out for mistyping a password.
- **F-53** — When the enrolled fingerprint is removed from the phone, the app clears the preference the moment it notices. That makes the biometric gate disappear the next frame, so the merchant walks in without the "Tu huella ya no está configurada…" notice, and Seguridad turns the switch off silently.
- **F-73** — Seguridad lists every session on its own row: 19 rows of "qa-sim-tablet", each with its own "Cerrar".
- **F-49** — A token rotation without a session (after `pm clear`) sends `POST /notifications/tokens` and gets a 401. A registration that fails for lack of network is never retried, so the phone gets no order pushes until the next login.
- **F-50** — The keyboard's ✓ on the password field only hides the keyboard; it does not sign in. The same happens in Cambiar contraseña and Datos personales.
- **F-51** — The password eye is announced as "Show password" in English.

## What Changes

- **A wrong current password is not an expired token (F-48).** The authenticated client no longer refreshes on a `401` whose `errorCode` is `CURRENT_PASSWORD_INCORRECT`. Cambiar contraseña sends one request and shows "La contraseña actual no es correcta". Errors are chosen by `errorCode`. A refresh that fails for lack of network no longer clears the session; only a refresh the server rejects does.
- **Fingerprint removed (F-53).** The unlock screen shows "Tu huella ya no está configurada en este equipo. Ingresa con tu contraseña." with "Continuar". The preference is cleared only after continuing signs out, so the notice is actually seen. Seguridad, finding the preference on with no fingerprint, turns it off *and says so*: "Desactivamos «Ingresar con huella» porque este equipo ya no tiene huellas registradas."
- **Sessions grouped (F-73).** "Este equipo" first, then one row per device name. A device with two or more sessions reads "qa-sim-tablet · 19 sesiones", with its last use and "Cerrar todas" (after a confirmation, it closes each session, then reloads). A device with one session keeps today's row.
- **Push registration (F-49).** Registration is skipped when there is no session, so a token rotation while signed out sends nothing. While signed in, a failed registration is retried when the network comes back, up to 10 attempts, and the retries stop on sign-out.
- **Keyboard Done (F-50).** Done on the password submits the login. Done on the last field submits Cambiar contraseña when "Guardar" is enabled, and saves Datos personales when "Guardar" is enabled.
- **Password eye (F-51).** It reads "Mostrar contraseña" / "Ocultar contraseña", translated.

## Capabilities

### New Capabilities

_None._

### Modified Capabilities

- `merchant-auth`: "Login screen presentation" (Done submits; the eye's description is translated).
- `session-management`: "Transparent access-token refresh" (no refresh on `CURRENT_PASSWORD_INCORRECT`; a refresh that fails for lack of network keeps the session).
- `biometric-unlock`: "Biometrics no longer available" (the preference is cleared after continuing, not before the notice).
- `account-security`: "List active sessions" (grouped by device, "Cerrar todas"), "Change password" (one request, errors by `errorCode`, Done submits), "Fingerprint sign-in setting" (the silent clear now says so).
- `order-notifications`: "Register the device for order push notifications" (no request without a session; retry when the network returns).

## Impact

- **`core:network`** — `KtorClientFactory` (`reAuthorizeOnResponse` skips `CURRENT_PASSWORD_INCORRECT`).
- **`core:domain`** — `RegisterPushTokenUseCase` (session check, returns `Result`).
- **`core:data`** — `AccountSecurityRepositoryImpl` (errors by `errorCode`), `SessionAuthTokenProvider` (keeps the session on a network failure).
- **`core:ui`** — `QhathiaTextField` eye description from strings.
- **`core:notifications`** — new `NetworkMonitor` (connectivity `Flow`).
- **`core:testing`** — fakes for the session and the network monitor.
- **`app`** — `MainViewModel` (retrying registration, cancelled on sign-out).
- **`features:auth`** — `LoginContent` (Done), `BiometricUnlockViewModel` (order of sign-out and clear).
- **`features:profile`** — Seguridad (grouping, "Cerrar todas", message on silent clear), Cambiar contraseña and Datos personales (Done).
- **Depends on** `problem-detail-errors` (the `errorCode`) and `pairing-flow-v2` (`QhathiaTextField.keyboardActions`). The branch is cut from `feature/pairing-flow-v2`.
- **Not in this change:**
  - the order notification opening Inicio with biometrics on (F-52) — `notifications-routing`;
  - the orphan session from killing the app mid-login (S:LOG-022): no client-side fix, per the handoff.
