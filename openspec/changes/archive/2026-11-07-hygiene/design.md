## Context

`AndroidManifest.xml` sets `allowBackup="true"`, `fullBackupContent="@xml/backup_rules"` (API ≤ 30) and `dataExtractionRules="@xml/data_extraction_rules"` (API 31+). Both XML files are the Android Studio templates with every rule commented out, so everything is backed up: the session and app-preference DataStores included. The app keeps nothing worth restoring: the catalog, orders and settings live on the server, and a restored refresh token is a session moved to another device.

`KtorClientFactory` installs `Logging` only when `NetworkConfig.enableLogging` (`BuildConfig.DEBUG`), at `LogLevel.BODY`, through `println`, with `sanitizeHeader` for `Authorization` and the refresh-token header. Request and response bodies are printed whole.

## Goals / Non-Goals

**Goals:**
- Nothing of the app reaches a backup or a device transfer.
- A debug log can be shared without leaking credentials or personal data.

**Non-Goals:**
- HTTPS and the cleartext exception (F-20, blocked on a domain).
- Logging in release builds (stays off).
- Keeping the app language across a restore (product did not ask; it is one setting).

## Decisions

1. **Backup off, and Android 12+ rules that exclude everything.** `allowBackup="false"` stops Auto Backup and adb backup on every API level; on API 31+ device-to-device transfer ignores `allowBackup`, so `data_extraction_rules.xml` excludes `root`, `file`, `database`, `sharedpref` and `external` from both `<cloud-backup>` and `<device-transfer>`. `fullBackupContent` goes with its template file, since nothing is backed up.

2. **Redact values, keep bodies.** A pure `redactSecrets(message: String): String` replaces the string value of any JSON key that ends in `password` or `token` (case-insensitive: `password`, `currentPassword`, `newPassword`, `accessToken`, `refreshToken`, `currentRefreshToken`, `pairingToken`, `token`), or is `exitPin`, `phone` or `email`, with `"***"`. Keys such as `passwordUpdatedAt`, `tokenType` or `exitPinSet` stay, since they do not end that way. The logger calls it on every message.
   - *Why not `LogLevel.HEADERS`?* Bodies are what a developer needs to debug the backend's ProblemDetail; only a handful of values are sensitive.
   - *Why a regex and not JSON parsing?* Log messages are not always whole JSON (Ktor prefixes them, bodies can be truncated); a key/value pattern works on any fragment.

3. **`Log.d` with a tag.** Logcat filtering by `KtorClient`, and no output in JVM unit tests unless logging is enabled (no test enables it).

## Risks / Trade-offs

- [A new sensitive field with another name] → the key list is in one function with tests; adding a key is one line.
- [A restored phone needs a sign-in] → by design; the session belonged to the old device.
- [`Log.d` in a JVM test] → would throw "not mocked"; tests build the client with logging off, and `redactSecrets` is tested on its own.

## Migration Plan

Ships with the app. Backups made by earlier versions still hold the old data until Android rotates them; a restore from one is ignored once the new version is installed with `allowBackup="false"`. Rollback is reverting the change.

## Open Questions

_None._
