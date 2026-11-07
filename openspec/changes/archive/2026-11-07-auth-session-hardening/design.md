## Context

**F-48.** `KtorClientFactory` installs the bearer `Auth` plugin on the authenticated client. On every `401` the plugin calls `refreshTokens` and replays the request once. `AuthApiService.changePassword` runs on that client, so a `401 CURRENT_PASSWORD_INCORRECT` costs a refresh and a second request. `SessionAuthTokenProvider.refreshTokens` clears the session on *any* refresh failure, so a refresh lost to the network signs the merchant out. Ktor 3.4.3's `AuthConfig` offers `reAuthorizeOnResponse { response -> Boolean }`, which decides which responses count as "unauthorized" (checked with `javap` on the cached jar). `problem-detail-errors` already makes `AuthApiService` read the ProblemDetail.

**F-53.** `BiometricUnlockViewModel` clears the preference as soon as the screen reports `BiometricGone`. `MainViewModel.authState` reads `biometric && !isUnlocked` to show the gate, so clearing the preference flips the state to `Authenticated` on the next frame and the notice is never seen. `SecurityViewModel.onResumed` also clears it, without telling the merchant.

**F-73.** `SecurityContent.SessionsBody` draws one `SessionRow` per session. The backend caps sessions at 20 per user, so one request already returns all of them.

**F-49.** `RegisterPushTokenUseCase` swallows every failure and has no session check. `QhathiaMessagingService.onNewToken` calls it unconditionally, which after `pm clear` means a 401. `MainViewModel` registers once on the transition to signed-in and never retries. There is no connectivity observer in the repo; `ACCESS_NETWORK_STATE` is already declared.

**F-50 / F-51.** `QhathiaTextField` gained `keyboardActions` in `pairing-flow-v2`. Login, Cambiar contraseña and Datos personales do not use it. The eye's content description is hard-coded English.

Modules (no new module; graph unchanged):

```
app (MainViewModel) ──► core:domain ◄── core:data ──► core:network (KtorClientFactory)
features:auth ─────────┤   (RegisterPushTokenUseCase,   (AccountSecurityRepositoryImpl,
features:profile ──────┘    NetworkMonitor port)          SessionAuthTokenProvider, ConnectivityNetworkMonitor)
core:ui (QhathiaTextField)                                core:testing (FakeNetworkMonitor, fakes)
```

## Goals / Non-Goals

**Goals:**
- A wrong password costs one request and never risks the session. Only a refresh the server rejects signs the merchant out.
- The fingerprint-removed notice is seen before anything is cleared.
- Sessions read as devices, and a noisy device can be closed in one action.
- The phone ends up registered for pushes whenever it is signed in and online, and never calls the endpoint signed out.
- Keyboard Done does what the primary button does; the eye speaks the app's language.

**Non-Goals:**
- Refusing to refresh on `SESSION_REVOKED`. The refresh then fails with a 401 and the session is cleared, which is the intended outcome.
- Pagination of sessions (the 20-session cap makes it unnecessary).
- The orphan session from killing the app during login (S:LOG-022): no client fix.
- F-52 (notification routing with biometrics): `notifications-routing`.

## Decisions

### 1. One client-wide rule instead of a per-call workaround (F-48)

```kotlin
install(Auth) {
    reAuthorizeOnResponse { response ->
        response.status == HttpStatusCode.Unauthorized &&
            response.problemErrorCode(json) != ERROR_CURRENT_PASSWORD_INCORRECT
    }
    bearer { … }
}
```

`problemErrorCode` reads the body best effort and returns null on any failure, the same way `readProblem` does. The rule lives in `KtorClientFactory`, so every authenticated call obeys it, not only change-password.

- *Why not the handoff's `AuthCircuitBreaker` + manual retry:* it duplicates the plugin's job inside one service and needs a second request path for "real" 401s. `reAuthorizeOnResponse` is the plugin's own extension point for exactly this.
- *Reading the body twice:* the client saves response bodies (the same reason `toNetworkResult(json)` can read them after validation), so the predicate's read does not consume what the caller reads later.

`AccountSecurityRepositoryImpl.toChangePasswordError` decides on the `HttpError`:
- `401` + `CURRENT_PASSWORD_INCORRECT` → `WrongCurrent`;
- other `401`s → `Unknown`, because the plugin already handled the session;
- `400` → `SameAsCurrent`;
- `422` → `Validation`.

### 2. A refresh lost to the network keeps the session

`SessionAuthTokenProvider.refreshTokens` clears the session only when the refresh failure is not `LoginError.Network`. On `Network` it returns null without clearing: the original request fails, the screen shows its connectivity message, and the next request refreshes again.

### 3. Clear the fingerprint preference only after signing out (F-53)

- `BiometricGone` only sets `isBiometricGone`.
- `signOut()` runs `logoutUseCase()` first and then, when `isBiometricGone`, `setBiometricEnabledUseCase(false)`. The order matters: logging out makes the state `Unauthenticated`, so the cleared preference can no longer lift the gate into the app.
- `SecurityViewModel.onResumed`, when it clears a preference that no longer has a usable biometric, emits `ShowMessage(security_biometric_disabled_gone)`.

### 4. Sessions grouped in the UI state (F-73)

```kotlin
data class SessionGroup(val label: String, val sessions: List<ActiveSession>) {
    val lastUsedAt: Instant? get() = sessions.mapNotNull { it.lastUsedAt }.maxOrNull()
}
val currentSession: ActiveSession?   // first isCurrent
val otherGroups: List<SessionGroup>  // non-current, grouped by deviceLabel.trim(), newest first
val pendingRevokeGroup: SessionGroup?
```

- `SessionsBody` draws the current session, then one row per group: a group of one keeps `SessionRow`; a larger one uses `SessionGroupRow` ("%1$s · %2$d sesiones", last use, "Cerrar todas").
- Events `RevokeGroupRequested(group)`, `RevokeGroupDismissed`, `RevokeGroupConfirmed`.
- Confirming marks every id in `revokingIds`, revokes them one by one with the existing `RevokeSessionUseCase` (19 at most; a 404 already counts as done), then reloads. A failure shows `security_revoke_error` after the reload.
- *Why sequential:* a handful of small requests. Running them in parallel would buy little and risk rate limits.

### 5. Push registration: skip without a session, retry while signed in (F-49)

- `RegisterPushTokenUseCase(sessionRepository, …)` returns `Result<Unit>`.
  - With no session it returns success without calling. This also covers `onNewToken`.
  - Without a token it returns a failure.
  - Otherwise it returns the repository's result; exceptions are caught.
- New port `NetworkMonitor { val isOnline: Flow<Boolean> }` in `core:domain`, implemented in `core:data` as `ConnectivityNetworkMonitor` with `callbackFlow` over `ConnectivityManager.registerDefaultNetworkCallback`. It is bound in `DataModule`.
- `MainViewModel.ensurePushRegistered()` runs on each transition to signed-in. Up to 10 attempts: on failure it waits until `isOnline` emits true (capped at 15 s), then 1 s, then tries again. The job is cancelled when the session goes away.
- *Why a port in domain:* `MainViewModel` lives in `:app` and is tested on the JVM. A port lets the test drive "network back" with a `FakeNetworkMonitor` instead of Android classes.

### 6. Keyboard Done and the eye (F-50, F-51)

- Login password: `KeyboardActions(onDone = { keyboard?.hide(); onEvent(Submit) })`. The ViewModel already ignores repeats while submitting.
- `LabelledPasswordField(onImeAction)`: the confirmation field submits when `canSubmit`.
- Datos personales' last field saves when `canSave`. That is an implementation detail with no spec change; the merchant-profile spec does not describe keyboard actions.
- `QhathiaTextField` eye: `stringResource(if (passwordVisible) password_hide_cd else password_show_cd)`, in `core:ui` `values` / `values-en`.

## Risks / Trade-offs

- [A 401 body the predicate cannot read] → It counts as unauthorized, which is today's behaviour, so the fallback is safe.
- [Keeping the session on a network failure keeps an expired access token] → Every request then fails until the network returns, and the first one after that refreshes. Signing out on a flaky network was worse.
- [Retrying registration drains the battery] → At most 10 attempts per sign-in, each waiting for connectivity. No polling while offline.
- [Grouping by label merges two real devices with the same name] → They are shown as one device with N sessions, and "Cerrar todas" closes both. Acceptable: the merchant chose the labels, and every session is still closable.

## Migration Plan

Single merge, no flag, no persisted data. Rollback is reverting the commit.

## Open Questions

_None._
