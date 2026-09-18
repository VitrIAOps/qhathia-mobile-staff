## 1. Create the `:features:profile` module

- [x] 1.1 Create the module directories: `features/profile/src/main/java/com/qhathia/mobile/staff/feature/profile/{hub,personal,notifications,security,settings,subscription,help,common,navigation}`, `features/profile/src/main/res/{values,values-en}`, `features/profile/src/test/java/com/qhathia/mobile/staff/feature/profile`
- [x] 1.2 Write `features/profile/build.gradle.kts` from the Feature-module template in `docs/android/android-frontend-standards.md`
  - Plugins and config: `android.library` + `kotlin.compose` + `ksp` + `hilt`, `namespace = "com.qhathia.mobile.staff.feature.profile"`, `compileSdk 36`, `minSdk 26`, `buildFeatures.compose = true`, JUnit Platform
  - Dependencies mirrored from `features/home/build.gradle.kts`: `:core:domain`, `:core:ui`, `:core:common`, Compose BOM, lifecycle, navigation-compose, `kotlinx.immutable`, Hilt + `ksp(kotlin.metadata.jvm)`, and the JUnit5/MockK/Turbine test set
- [x] 1.3 Register `include(":features:profile")` in `settings.gradle.kts`
- [x] 1.4 Add the minimal `features/profile/src/main/AndroidManifest.xml` (`<manifest />`)
- [x] 1.5 Confirm the empty module builds: `./gradlew :features:profile:assembleDebug`

## 2. Dependencies and app shell prerequisites

- [x] 2.1 Add `androidx.appcompat` and `androidx.biometric` (stable versions) to `gradle/libs.versions.toml`
- [x] 2.2 `:app`: add `appcompat`; switch `MainActivity` to `AppCompatActivity`; change the `Theme.QhathiaStaff` parent to `Theme.AppCompat.DayNight.NoActionBar`; verify edge-to-edge and the login screen are unchanged
- [x] 2.3 `:app`: add `res/xml/locales_config.xml` (es, en), `android:localeConfig` on `<application>`, and the `androidx.appcompat.app.AppLocalesMetadataHolderService` service entry with `autoStoreLocales=true` in `AndroidManifest.xml`

## 3. Domain layer — `:core:domain`

- [x] 3.1 Add `sessionId: String?` to `Session`
- [x] 3.2 Extend `Device`
  - New fields: `name`, `batteryLevel: Int?`, `networkStatus: String?`, `storeName: String?`, `lastPingAt: Instant?`, `lastSyncAt: Instant?`
  - Add `DeviceCommand`, `DeviceCommandType` (`REFRESH_CATALOG`, `RESTART_APP`) and `DeviceCommandStatus` (`PENDING`, `DELIVERED`, `ACKED`, `FAILED`, `EXPIRED`, `UNKNOWN`)
- [x] 3.3 Add the models:
  - `UserProfile`
  - `Subscription`, `SubscriptionStatus`, `PlanFeatures`
  - `NotificationPreferences`, `NotificationPreference`
  - `ActiveSession`
  - `StoreSettings`, `AppLanguage`
  - `ChangePasswordError` + `ChangePasswordException`
- [x] 3.4 Add repository ports: `UserProfileRepository`, `SubscriptionRepository` (`404` → `null`), `NotificationPreferencesRepository`, `AccountSecurityRepository`, `StoreSettingsRepository`, `AppPreferencesRepository`; extend `DeviceRepository` with `getCatalogCount`, `sendCommand`, `getCommands`
- [x] 3.5 Add use cases:
  - Profile and subscription: `GetUserProfileUseCase`, `GetSubscriptionUseCase`
  - Notifications: `GetNotificationPreferencesUseCase`, `SetNotificationPreferenceUseCase`
  - Security: `GetActiveSessionsUseCase`, `RevokeSessionUseCase`, `RevokeOtherSessionsUseCase`, `ChangePasswordUseCase`
  - Store and language: `GetStoreSettingsUseCase`, `SetPricesIncludeIgvUseCase`, `ObserveAppLanguageUseCase`, `SetAppLanguageUseCase`
  - Biometric: `ObserveBiometricEnabledUseCase`, `SetBiometricEnabledUseCase`
  - Devices: `GetCatalogCountUseCase`, `SendDeviceCommandUseCase`, `GetDeviceCommandsUseCase`
- [x] 3.6 `SetAppLanguageUseCase`: persist locally first, then do a best-effort store update only for OWNER/ADMIN; the result reflects only the local write

## 4. Network layer — `:core:network`

- [x] 4.1 `AuthDtos`: `LoginRequestDto.deviceLabel: String?`, `AuthResponseDto.sessionId: String?`; add `ActiveSessionDto` and `ChangePasswordRequestDto`
- [x] 4.2 `AuthApiService`: `getSessions(refreshToken)` and `revokeOthers(refreshToken)`, both with the `X-Refresh-Token` header; `revokeSession(id)`; `changePassword(body)` — all on the authenticated client
- [x] 4.3 Add `UserApiService` (`GET /users/me`, with `UserProfileDto` carrying `phone`, `passwordUpdatedAt` and `stores`) and `SubscriptionApiService` (`GET /subscriptions/current`, §18 shape)
- [x] 4.4 `NotificationApiService`: `getPreferences()` and `updatePreferences(body)`; `NotificationPreferencesDto` and a partial `UpdateNotificationPreferencesDto` with nullable fields, serialized without explicit nulls
- [x] 4.5 Add `StoreApiService` (`GET/PUT /stores/{storeId}`, `StoreSettingsDto` with `language`, `pricesIncludeIgv`, `igvRate`; partial update body) and `CatalogApiService` (`GET /catalog/count`)
- [x] 4.6 `DeviceDtos`/`DeviceApiService`: add the §18 fields to `DeviceDto`; add `sendCommand(deviceId, body)` and `getCommands(deviceId)` with `DeviceCommandDto`
- [x] 4.7 Register the new services in `NetworkModule`; make sure debug logging sanitizes `X-Refresh-Token`

## 5. Data layer — `:core:data`

- [x] 5.1 `PersistedSession.sessionId: String? = null` (older blobs still decode); map it both ways; keep it when refreshed tokens are stored
- [x] 5.2 Add `DeviceLabelProvider` (`Build.MANUFACTURER` + `Build.MODEL`, de-duplicated, max 100) and pass it into `AuthRepositoryImpl.login`; map `sessionId` in `toSession`
- [x] 5.3 Add mappers for profile, subscription, notification preferences, active sessions, store settings, extended devices and device commands (ISO → `Instant?`, money → `BigDecimal`, unknown enums → fallback)
- [x] 5.4 Implement `UserProfileRepositoryImpl`, `SubscriptionRepositoryImpl`, `NotificationPreferencesRepositoryImpl`, `StoreSettingsRepositoryImpl` (active store id from the session)
- [x] 5.5 Implement `AccountSecurityRepositoryImpl`
  - The refresh token for headers and `currentRefreshToken` comes from the session
  - `ChangePasswordError` mapping: `401` → `WrongCurrent`, `400` → `SameAsCurrent`, `422` → `Validation`, transport failure → `Network`
- [x] 5.6 Extend `DeviceRepositoryImpl` with catalog count and commands
- [x] 5.7 Add `AppPreferencesRepositoryImpl` on a separate Preferences DataStore `qhathia_app_prefs` (language, biometric flag), with its own qualified DataStore provider in `DataModule`; bind all new repositories

## 6. Test doubles — `:core:testing`

- [x] 6.1 Add `FakeUserProfileRepository`, `FakeSubscriptionRepository`, `FakeNotificationPreferencesRepository`, `FakeAccountSecurityRepository`, `FakeStoreSettingsRepository`, `FakeAppPreferencesRepository`, each with settable results, failure modes and recorded calls
- [x] 6.2 Extend `FakeDeviceRepository` (catalog count, commands queue, recorded commands) and `FakeSessionRepository`/fixtures with `sessionId`
- [x] 6.3 Add fixtures for every new model, plus a controllable `Clock`

## 7. Shared components — `:core:ui`

- [x] 7.1 Add `QhathiaGradientTopBar` (title, optional back, actions) on `headerGradient`
- [x] 7.2 Add `QhathiaSettingsSection`, `QhathiaNavigationRow` (with `comingSoon`), `QhathiaSwitchRow` (merged `toggleable` + `Role.Switch`), `QhathiaComingSoonTag`, `QhathiaInitialsAvatar`, `QhathiaPlanProgressBar`; add the `switchChecked` token; light/dark previews for each
- [x] 7.3 Add `androidx.biometric` to `core/ui/build.gradle.kts` and `rememberBiometricAuthenticator()`
  - `availability()`: `Available | NoneEnrolled | NoHardware | Unavailable`, from `BIOMETRIC_STRONG or BIOMETRIC_WEAK`
  - `authenticate(...)`: returns `BiometricResult`, requires a `FragmentActivity` from `LocalActivity`
  - Helper intent for enrollment settings: `ACTION_BIOMETRIC_ENROLL` on API 30+, else `ACTION_SECURITY_SETTINGS`

## 8. Login and session — `:features:auth`

- [x] 8.1 Verify login persists `sessionId` end to end, without UI changes; add `BiometricUnlockScreen`
  - Content: wordmark, user name, "Desbloquear con huella", "Ingresar con contraseña"
  - The prompt opens automatically on entry
  - A "no longer enrolled" variant shows the message and "Continuar"
  - Light/dark previews
- [x] 8.2 Add `BiometricUnlockViewModel`
  - Success → unlock callback
  - Password fallback → `LogoutUseCase`
  - Not enrolled → clear the preference, then logout on continue

## 9. Mi perfil hub, Datos personales, Mi suscripción, Ayuda — `:features:profile`

- [x] 9.1 `common/ProfileFormatters`: relative time (plural-aware, injected `Clock`), `es-PE`/`en` dates in `America/Lima`, `+51` phone, initials, role labels, plan feature list from flags
- [x] 9.2 Hub MVI + `ProfileHubViewModel`
  - Loads run in parallel with independent failures: profile, notification counts, and — only for OWNER/ADMIN — subscription and devices
  - The session fallback for name and email
  - The connected-tablet count uses the 10-min rule
  - Events: `Refresh` on resume, logout confirmation → `LogoutUseCase`
- [x] 9.3 `ProfileHubContent`/`Screen`: gradient header with bell, identity card with edit, subscription card (active / no subscription / hidden for SELLER), menu rows with live and fallback subtitles, "Cerrar sesión" with confirm dialog; previews for owner, seller, no subscription, loading
- [x] 9.4 `PersonalData` ViewModel + read-only content (avatar, "Cambiar foto" Próximamente, read-only fields, "—" for missing phone, loading/error); previews
- [x] 9.5 `Subscription` ViewModel + content (plan card, feature checklist, progress, no-subscription empty state, Próximamente payment sections and cancel); previews
- [x] 9.6 `Help` content: FAQ accordion with static answers, quick guides Próximamente, version footer from `PackageManager` with "Términos y privacidad" Próximamente; previews

## 10. Notificaciones — `:features:profile`

- [x] 10.1 MVI + `NotificationPreferencesViewModel`: load, optimistic per-key toggle with a `saving` set, single-key `PUT`, adopt the response, revert on failure plus `ShowError`
- [x] 10.2 `NotificationPreferencesContent`/`Screen`: three `QhathiaSettingsSection`s with `QhathiaSwitchRow`s, loading/error/retry, snackbar; previews

## 11. Seguridad and Cambiar contraseña — `:features:profile`

- [x] 11.1 `SecurityViewModel`
  - Profile (`passwordUpdatedAt`), sessions sorted with the current one resolved by flag or stored id, per-row revoke with confirm (`204`/`404` remove the row), revoke-others with confirm (hidden when alone)
  - Biometric toggle flow: `Available` → prompt effect, `NoneEnrolled` → enroll hint effect, `NoHardware`/`Unavailable` → message; clear the preference on resume when no longer available
- [x] 11.2 `SecurityContent`/`Screen`: password row, fingerprint switch wired to `rememberBiometricAuthenticator`, enroll-hint dialog with "Abrir ajustes", sessions card with "este equipo"/"Cerrar", revoke-others action, error/retry per section; previews (with sessions, alone, biometric on, error)
- [x] 11.3 `ChangePasswordViewModel` + `Screen`: three password fields with show/hide, validation (non-empty, 8–100, match), submit with progress, error mapping to field or banner messages, `password_changed` result to the previous entry → "Contraseña actualizada" snackbar on Seguridad; previews

## 12. Configuración and app locale

- [x] 12.1 `AppSettingsViewModel`
  - Language from `ObserveAppLanguageUseCase`; selection → `SetAppLanguageUseCase`
  - Store settings load; on load, re-send the language when it differs from the store's (OWNER/ADMIN)
  - IGV optimistic toggle with revert on failure, read-only for SELLER, retry when the store fails
- [x] 12.2 `AppSettingsContent`/`Screen`: language segmented pills, IGV switch row (enabled/read-only/loading/retry); previews
- [x] 12.3 `:app` `AppLocaleController`
  - Observe the language and call `AppCompatDelegate.setApplicationLocales` when it differs from the current app locales
  - On start: reconcile from `getApplicationLocales()` (API 33+ system change) or default from the system language, and persist
- [x] 12.4 Add `values-en/strings.xml` (with plurals) to every module that has strings: `app`, `core/ui`, `core/notifications`, `features/auth`, `features/home`, `features/sales-statistics`, `features/product-inventory`, `features/product-register`, `features/product-detail`, `features/order-requests`, `features/device-linking`, `features/profile`
- [x] 12.5 Make date/number formatters that hardcode `es-PE` follow the current app locale (sales statistics, home, order requests, profile)

## 13. Dispositivos — `:features:device-linking`

- [x] 13.1 Rebuild `DeviceLinkingUiState`/`UiEvent`/`UiEffect`
  - Device card models, catalog count, per-device commands, restart confirmation
  - The existing pairing dialog state is kept
- [x] 13.2 `DeviceLinkingViewModel`
  - Parallel devices + catalog count, then each device's recent commands (hide finished commands older than 10 min)
  - Send command (disable while in flight; error keeps the button enabled)
  - Restart confirmation
  - 15 s polling only while started and something is in flight
  - Connection rule with an injected `Clock`
- [x] 13.3 `DeviceLinkingContent`: gradient top bar "Dispositivos", one card per tablet (identity + status, Catálogo visible, battery/connection/store/"Clientes hoy" Próximamente tiles, last-report caption when disconnected, the two action buttons, command status line), empty state, "+ Vincular otro dispositivo" text action opening `PairDeviceDialog` (floating + removed), loading/error; previews (connected, disconnected, command states, empty)
- [x] 13.4 `DeviceLinkingScreen`: `LifecycleStartEffect` → `ScreenStarted`/`ScreenStopped`; restart confirm dialog; update strings

## 14. Navigation and app wiring

- [x] 14.1 `features/profile/.../navigation/ProfileNavigation.kt`
  - `PROFILE_ROUTE` and the flat sub-routes (`profile/personal`, `profile/notifications`, `profile/security`, `profile/security/password`, `profile/settings`, `profile/subscription`, `profile/help`)
  - `NavGraphBuilder.profileGraph(navController, onNavigateToDevices, onNavigateToOrderRequests)`
- [x] 14.2 Add `implementation(project(":features:profile"))` to `app/build.gradle.kts`
- [x] 14.3 `MainShell`: use `PROFILE_ROUTE` from `:features:profile` in `topLevelDestinations`; replace the `composable(PROFILE_ROUTE)` block with `profileGraph(...)` (devices → `navController.navigate(DEVICE_LINKING_ROUTE)`, bell → `switchTab(ORDER_REQUESTS_ROUTE)`); confirm the bottom bar hides on every sub-route and returns on back
- [x] 14.4 Delete `app/src/main/java/com/qhathia/mobile/staff/ui/profile/` and the now-unused `profile_*`/`placeholder_coming_soon` strings in `:app`
- [x] 14.5 `MainViewModel`/`QhathiaRoot`
  - Add `AuthState.Locked` (session + biometric enabled + not unlocked)
  - Reset the unlock after at least 5 min in background via `ProcessLifecycleOwner`
  - Render `BiometricUnlockScreen`; keep `pendingDestination` until unlocked

## 15. Tests

- [x] 15.1 Mappers: profile (null `passwordUpdatedAt`, store count), subscription (flags, null `endsAt`), preferences, sessions, store settings (unknown language → null), extended device, commands (unknown status → `UNKNOWN`), `sessionId` in `toSession`
- [x] 15.2 Repositories
  - Subscription `404` → `null`
  - Notification `PUT` body contains only the changed key
  - Sessions send `X-Refresh-Token`; change-password error mapping
  - Store update sends only the changed field; login sends `deviceLabel`
  - `PersistedSession` without `sessionId` decodes; refresh keeps `sessionId`
- [x] 15.3 `SetAppLanguageUseCaseTest`: local write first; OWNER sends, SELLER does not; backend failure still succeeds
- [x] 15.4 `ProfileHubViewModelTest`: owner loads four sources; seller skips subscription and devices; subtitle fallbacks on failure; connected count by threshold; `Refresh` on resume; logout invokes the use case
- [x] 15.5 `NotificationPreferencesViewModelTest`: optimistic toggle, single-key request, adopt response, revert on failure, independent concurrent toggles
- [x] 15.6 `SecurityViewModelTest`
  - Current-session resolution by flag and by stored id
  - Revoke removes on `204`/`404` and keeps on error; revoke-others keeps only current and is hidden when alone
  - Biometric: enable only after prompt success; none enrolled → enroll hint and stays off; cleared when no longer available
- [x] 15.7 `ChangePasswordViewModelTest`: validation gates submit; each error maps to its message; success emits the result and navigates back
- [x] 15.8 `AppSettingsViewModelTest`: local language shown over the store value; re-send on mismatch for owner; IGV optimistic + revert; seller read-only; store failure keeps language usable
- [x] 15.9 `DeviceLinkingViewModelTest` (update existing)
  - Cards from extended devices; catalog failure → null count; connected/disconnected by `Clock`
  - Command send disables the button and shows "Enviado"; failure re-enables
  - Polling runs only while started and in flight, and stops on `ACKED`/`EXPIRED`
  - Restart requires confirmation; pairing still reloads
- [x] 15.10 `MainViewModelTest`/`BiometricUnlockViewModelTest`: locked when enabled with a session; unlocked after success; background ≥ 5 min relocks; password fallback logs out and keeps the biometric preference; not enrolled clears the preference
- [x] 15.11 `ProfileFormattersTest`: relative times, dates in es and en, phone, initials, role labels, feature list from flags

## 16. Definition of Done

- [x] 16.1 `./gradlew test` passes
- [x] 16.2 `./gradlew lint` passes with no new findings, including `MissingTranslation` for English
- [x] 16.3 `./gradlew assembleDebug` builds
- [x] 16.4 Every new or rebuilt screen-level composable has working light and dark `@Preview`s
- [ ] 16.5 Manual pass against Figma nodes `2086-3671`, `2269-4227`, `2292-6906`, `2086-7140`, `2293-7282`, `2293-7459`, `2294-9087`, `2294-9694` in light and dark; back from every sub-screen returns to Mi perfil with the bottom bar
- [ ] 16.6 Manual pass: switch to English and back (whole app, survives restart and sign out); fingerprint enable with and without an enrolled biometric; cold start gate and password fallback; revoke a session from a second phone; change password keeps this session; send both tablet commands and watch the status reach "Listo"
- [ ] 16.7 Logged in as SELLER: no subscription card, no Dispositivos row, IGV read-only, language still switches
