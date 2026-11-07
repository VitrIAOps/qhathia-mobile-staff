## 1. Backup (`app`) — F-72

- [x] 1.1 `AndroidManifest.xml`: `android:allowBackup="false"`, remove `android:fullBackupContent`; remove `res/xml/backup_rules.xml` (design decision 1)
- [x] 1.2 `res/xml/data_extraction_rules.xml`: `<cloud-backup>` and `<device-transfer>` exclude `root`, `file`, `database`, `sharedpref` and `external`; comment why

## 2. Debug log (`core:network`) — F-20

- [x] 2.1 `LogRedaction.kt`: internal `redactSecrets(message)` for keys ending in `password` / `token` (case-insensitive), `exitPin`, `phone`, `email` (design decision 2)
- [x] 2.2 `KtorClientFactory`: the logger writes `Log.d("KtorClient", redactSecrets(message))`; KDoc
- [x] 2.3 `LogRedactionTest`: login request and response, change-password body, push token, exit PIN, phone and email are redacted; `passwordUpdatedAt`, `tokenType`, `exitPinSet` and null values are left; escaped quotes inside a value do not leak its tail

## 3. Verification

- [x] 3.1 Run, one at a time: `:core:network:testDebugUnitTest`, `:app:testDebugUnitTest`, then `testDebugUnitTest`
- [x] 3.2 Run `./gradlew lintDebug` and `./gradlew assembleDebug`

## 4. Manual — QA

- [ ] 4.1 Manual — S:RED-062: `adb shell dumpsys package com.qhathia.mobile.staff | grep flags` has no `ALLOW_BACKUP`
- [ ] 4.2 Manual — D:RED-061 (phone part), debug build: sign in, change the password, save a phone; `adb logcat -s KtorClient` shows `***` for passwords, tokens and the phone, and no `Bearer` value
