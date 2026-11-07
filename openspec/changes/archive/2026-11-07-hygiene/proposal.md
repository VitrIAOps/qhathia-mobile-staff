## Why

The 2026-11-03 QA regression (`docs/handoff/android-regresion-2026-11-03.md` §3.3) finds two ways the phone can leak what it holds:

- **F-72** — `android:allowBackup="true"` with the Android Studio template rules (everything commented out): Android backs up, and restores on another phone, the session DataStore (`qhathia_session.preferences_pb`) with its refresh token (S:RED-062, `dumpsys package` shows `ALLOW_BACKUP`).
- **F-20 (logs)** — Debug builds log every request and response body (`LogLevel.BODY`, `println`). Headers are sanitized, bodies are not: login and change-password passwords, `accessToken` / `refreshToken`, the push token, the tablet exit PIN, the phone and the email reach logcat. The release build QA tested logs nothing, but any debug build shared with QA or a tester leaks them. The HTTPS part of F-20 stays blocked on a domain.

F-84 (dark theme and large fonts) and F-92 (accessibility) were planned with this change; they touch files from most open branches, so they go to a later change, `a11y-dark-font`, cut from `main` once those branches merge.

## What Changes

- **No backup (F-72).** `allowBackup` is off, `fullBackupContent` and its template file go, and the Android 12+ `data_extraction_rules.xml` excludes everything from cloud backup and device transfer. A restored or transferred phone starts signed out, with nothing carried over.
- **Bodies without secrets (F-20).** The debug HTTP log keeps bodies, so it stays useful, but replaces the value of every password, token, exit PIN, phone and email field with `***`. It logs through `Log.d("KtorClient", …)` instead of `println`. Release builds still log nothing.

## Capabilities

### New Capabilities

- `app-hygiene`: what the app keeps out of backups and logs.

### Modified Capabilities

_None._

## Impact

- **`app`** — `AndroidManifest.xml` (`allowBackup="false"`, no `fullBackupContent`), `res/xml/data_extraction_rules.xml`; `res/xml/backup_rules.xml` removed.
- **`core:network`** — `KtorClientFactory` logger (redaction, `Log.d`), a small `LogRedaction.kt` with JUnit tests.
- **Branch:** cut from `feature/auth-session-hardening` (change 6), which last changed `KtorClientFactory`.
