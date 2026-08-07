## 1. Firebase & Gradle setup

- [x] 1.1 Add to `gradle/libs.versions.toml`: `firebaseBom` version + `firebase-bom` and `firebase-messaging` libraries (messaging without explicit version, resolved by BOM), `kotlinx-coroutines-play-services` library, and the `google-services` plugin (`com.google.gms.google-services`).
- [x] 1.2 Declare the `google-services` plugin `apply false` in the root `build.gradle.kts` plugins block.
- [x] 1.3 Apply `alias(libs.plugins.google.services)` in `app/build.gradle.kts`.
- [x] 1.4 Move `google-services.json` from the repo root to `app/google-services.json` (it is git-ignored — keep it out of version control; document that CI must provide it).

## 2. Domain layer (`core:domain`)

- [x] 2.1 Add `NotificationTokenRepository` port (`repository/NotificationTokenRepository.kt`) with `suspend fun registerToken(token: String, deviceLabel: String?): Result<Unit>` and `suspend fun unregisterToken(token: String): Result<Unit>`.
- [x] 2.2 Add `PushTokenProvider` port (`repository/PushTokenProvider.kt`) with `suspend fun currentToken(): String?` and `suspend fun deleteToken()` — abstracts the Firebase SDK out of the domain.
- [x] 2.3 Add `RegisterPushTokenUseCase` (`usecase/RegisterPushTokenUseCase.kt`): read the current token via `PushTokenProvider`; if non-null, call `registerToken`; fail-open (never throw).
- [x] 2.4 Add `UnregisterPushTokenUseCase` (`usecase/UnregisterPushTokenUseCase.kt`): read the current token, call `unregisterToken`, then `PushTokenProvider.deleteToken()`; fail-open.
- [x] 2.5 Add `suspend fun logout(refreshToken: String): Result<Unit>` to the `AuthRepository` port.
- [x] 2.6 Extend `LogoutUseCase` to, in order (each fail-open): (1) run `UnregisterPushTokenUseCase`; (2) read the refresh token from `sessionRepository.observeSession().first()` and call `AuthRepository.logout(refreshToken)`; (3) `sessionRepository.clearSession()`.
- [x] 2.7 Add unit tests for both new use cases and the updated `LogoutUseCase` (fakes; assert unregister + server logout run before clear, ordering is preserved, and that any network failure is swallowed while clear still runs).

## 3. Network layer (`core:network`)

- [x] 3.1 Add `RegisterTokenRequestDto(token, platform, deviceLabel?)` and `UnregisterTokenRequestDto(token)` (`dto/NotificationDtos.kt`), `@Serializable` with `@SerialName`.
- [x] 3.2 Add `NotificationApiService` (`service/NotificationApiService.kt`) on the authenticated client: `registerToken(body): NetworkResult<Unit>` (`POST /api/v1/notifications/tokens`) and `unregisterToken(body): NetworkResult<Unit>` (`DELETE /api/v1/notifications/tokens`), using `runCatching { ... }.toNetworkResult()`; treat `204` as success.
- [x] 3.3 Provide `NotificationApiService` in `NetworkModule` with `@AuthenticatedClient`.
- [x] 3.4 Add a service test for `NotificationApiService` (mirror `DeviceApiService` / `AuthApiServiceTest`), covering register/unregister success and error mapping.
- [x] 3.5 Add `LogoutRequestDto(refreshToken)` (`@Serializable`) to `dto/AuthDtos.kt`.
- [x] 3.6 Add `logout(body: LogoutRequestDto): NetworkResult<Unit>` (`POST /api/v1/auth/logout`) to `AuthApiService` using an injected `@AuthenticatedClient` (treat `204` as success); update the class doc to note login/refresh use the public client while logout uses the authenticated one, and update `provideAuthApiService` in `NetworkModule` to pass both clients.
- [x] 3.7 Implement `AuthRepository.logout` in `AuthRepositoryImpl` — map `NetworkResult → Result<Unit>`; update `FakeAuthRepository` in `core:testing` for the new method. Add/extend tests.

## 4. Data layer (`core:data`)

- [x] 4.1 Add `NotificationTokenRepositoryImpl` (`repository/NotificationTokenRepositoryImpl.kt`) mapping `NetworkResult → Result`; build the request DTOs (platform `ANDROID`).
- [x] 4.2 Bind `NotificationTokenRepository` to `NotificationTokenRepositoryImpl` in `DataModule` (`@Binds @Singleton`).
- [x] 4.3 Add repository unit tests (register/unregister success, HTTP error, network error → `Result.failure`).

## 5. Create `core:notifications` module

- [x] 5.1 Create the module directory `core/notifications/src/main/java/com/qhathia/mobile/staff/core/notifications/`.
- [x] 5.2 Add `core/notifications/build.gradle.kts` from the Android Library template (Hilt + KSP), with deps on `core:domain`, `core:common`, `libs.firebase.messaging` (via BOM platform), `libs.kotlinx.coroutines.play.services`, and coroutines.
- [x] 5.3 Register `include(":core:notifications")` in `settings.gradle.kts`.
- [x] 5.4 Add `core/notifications/src/main/AndroidManifest.xml` declaring `<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />` and the `QhathiaMessagingService` `<service>` with the `com.google.firebase.MESSAGING_EVENT` intent filter (`exported="false"`).

## 6. Push service & notification plumbing (`core:notifications`)

- [x] 6.1 Add `FirebaseMessagingPushTokenProvider` implementing `PushTokenProvider` over `FirebaseMessaging.getInstance()` using `.await()` (coroutines-play-services); bind it in a `NotificationsModule` (`@Binds @Singleton`).
- [x] 6.2 Add `OrderNotificationPresenter` (or equivalent) that creates the notification channel (once) and posts a `NotificationCompat` notification from `event`/`status`, with a content-tap `PendingIntent` (`FLAG_IMMUTABLE`) opening `MainActivity`. Localized, PII-free copy for `NEW_ORDER` and `ORDER_TAKEN` (`CONFIRMED`/`REJECTED`).
- [x] 6.3 Add `QhathiaMessagingService : FirebaseMessagingService` (`@AndroidEntryPoint`): `onNewToken` → invoke `RegisterPushTokenUseCase` (no-op without a session); `onMessageReceived` → parse `data` and delegate to the presenter.
- [x] 6.4 Add notification strings to `core/notifications/src/main/res/values/strings.xml` (channel name/description, `NEW_ORDER` and `ORDER_TAKEN` titles/bodies).

## 7. Wire lifecycle & logout into the app (`:app`)

- [x] 7.1 Add `implementation(project(":core:notifications"))` to `app/build.gradle.kts`.
- [x] 7.2 In `MainViewModel`, inject `RegisterPushTokenUseCase` and invoke it (in `viewModelScope`) when `authState` transitions to `Authenticated` (covers login + authenticated start); ensure it runs once per transition.
- [x] 7.3 Request `POST_NOTIFICATIONS` at runtime on first authenticated entry to `MainShell` (guard `Build.VERSION.SDK_INT >= 33`) via `rememberLauncherForActivityResult` + `ActivityResultContracts.RequestPermission`; proceed regardless of the result.
- [x] 7.4 Add `ProfileViewModel` (`@HiltViewModel`, injects `LogoutUseCase`) in `:app` exposing a `logout()` action (mirrors `MainViewModel` precedent).
- [x] 7.5 Update `ProfileScreen` to bind `ProfileViewModel` and add a Logout button with a confirmation dialog; on confirm call `logout()`. Session clear flips `authState` → login (no manual navigation). Add profile logout strings and keep the light/dark `@Preview`.

## 8. Verification

- [x] 8.1 `./gradlew :core:domain:test :core:data:test :core:network:test` pass.
- [x] 8.2 `./gradlew assembleDebug` builds with `google-services.json` in `app/` and `./gradlew lint` is clean.
- [ ] 8.3 Manual check (emulator/device with Google Play services): log in → token registered (`POST /notifications/tokens` observed); send a test `NEW_ORDER` data push → local notification appears with no PII, tap opens the app; Profile → Logout → `DELETE /notifications/tokens` called, returns to login, no further pushes delivered. _(Requires FCM + a device — not runnable headless; use-case/logout ordering is covered by unit tests.)_
