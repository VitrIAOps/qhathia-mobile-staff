## Context

Perfil (`PROFILE_ROUTE`) is a placeholder inside `:app` (`app/ui/profile/ProfileScreen.kt` + `ProfileViewModel`). It offers a link-devices button, which opens `:features:device-linking`'s "Dispositivos vinculados" list, and sign out. The ClickUp task [Perfil de comercio y configuracion](https://app.clickup.com/t/86e38gtxw) replaces it with eight Figma `Qhatia-DS` screens:

| Screen | Node |
|---|---|
| Mi perfil | `2086-3671` |
| Datos personales | `2269-4227` |
| Notificaciones | `2292-6906` |
| Seguridad | `2086-7140` |
| Configuración | `2293-7282` |
| Dispositivos | `2293-7459` |
| Ayuda | `2294-9087` |
| Mi suscripción | `2294-9694` |

What each screen reads from, per `docs/android/api-contract.md`:

| Screen | Endpoints | Roles |
|---|---|---|
| Mi perfil / Datos personales | `GET /users/me` (§3, §18: `phone`, `passwordUpdatedAt`, `stores[]`) | any |
| Subscription card / Mi suscripción | `GET /subscriptions/current` (§18: price, limits, flags, `startedAt`/`endsAt`; `404` = none) | OWNER/ADMIN |
| Notificaciones | `GET/PUT /notifications/preferences` (§20, partial `PUT`) | OWNER/SELLER/ADMIN |
| Seguridad | `GET /auth/sessions`, `DELETE /auth/sessions/{id}`, `POST /auth/sessions/revoke-others` (header `X-Refresh-Token`), `POST /auth/change-password` (§19) | any |
| Configuración | `GET/PUT /stores/{storeId}` (`language`, `pricesIncludeIgv`, §22) | GET any · PUT OWNER/ADMIN |
| Dispositivos | `GET /devices` (§18 shape), `GET /catalog/count`, `POST/GET /devices/{id}/commands` (§23), `POST /devices/pair` (existing) | OWNER/ADMIN |
| Login | `POST /auth/login` + `deviceLabel` → `sessionId` (§19) | — |

Current code facts that shape the design:
- `Session` / `PersistedSession` hold no `sessionId`, and `LoginRequestDto` has no `deviceLabel`.
- `DeviceDto` predates §18: it has no `name`, `batteryLevel`, `networkStatus`, `storeName` or `lastSyncAt`.
- `MainActivity` is a `ComponentActivity` on the platform `Theme.Material` theme. Per-app locales on API < 33 (`AppCompatDelegate.setApplicationLocales`) and `BiometricPrompt` both need an `AppCompatActivity` / `FragmentActivity`.
- Only `values/strings.xml` exists, in 11 modules.
- `MainShell` already hides the bottom bar on non-top-level routes.
- Feature modules may depend only on `:core:domain`, `:core:ui` and `:core:common`, never on each other.

Decided with the requester:
- Datos personales is **read-only**.
- Language changes the **app locale**, with English strings added everywhere; it is device-owned and mirrored to the backend.
- Fingerprint sign-in is **implemented now**, using the biometrics enrolled on the device. If none are enrolled, the merchant is told to configure them.
- Elements without backend support stay visible as **Próximamente** and are listed in project memory (`profile-backend-gaps`).

## Goals / Non-Goals

**Goals:**
- Replace the Perfil placeholder with the eight screens, faithful to Figma in light and dark.
- Every sub-screen is pushed over Perfil without the bottom bar; back returns to Mi perfil.
- Use every backend capability from phases 18–23 that the designs expose.
- Device-owned language that switches the whole app between Spanish and English.
- Fingerprint gate using the device's enrolled biometrics.

**Non-Goals:**
- Editing profile or store data (read-only by decision); photo upload.
- Payments: method, history, cancellation.
- Remote tablet settings (§21 kiosk mode, PIN, brightness): no Figma node in this task.
- Renaming a tablet (`PATCH /devices/{id}`): not in the design.
- "Clientes hoy", FAQ content from the backend, guides, terms, WhatsApp support.
- Switching the active store.
- Translating backend-provided content.

## Decisions

### Decision 1 — Module layout

**New `:features:profile`** (feature template: `android.library` + `kotlin.compose` + `ksp` + `hilt`, `namespace = com.qhathia.mobile.staff.feature.profile`, `compileSdk 36`, `minSdk 26`). It depends on `:core:domain`, `:core:ui`, `:core:common`. The standards' module tree already names `features/profile/` for "User profile, settings".

```
features/profile/src/main/java/com/qhathia/mobile/staff/feature/profile/
├── hub/            ProfileHubScreen · ProfileHubContent · ProfileHubViewModel · model/…
├── personal/       PersonalDataScreen · …Content · …ViewModel · model/…
├── notifications/  NotificationPreferencesScreen · …Content · …ViewModel · model/…
├── security/       SecurityScreen · …Content · …ViewModel · ChangePasswordScreen · …ViewModel · model/…
├── settings/       AppSettingsScreen · …Content · …ViewModel · model/…
├── subscription/   SubscriptionScreen · …Content · …ViewModel · model/…
├── help/           HelpScreen · HelpContent · FaqItem (static, no ViewModel)
├── common/         ProfileFormatters (relative time, dates es-PE/en, +51 phone, initials, role label)
└── navigation/     ProfileNavigation.kt
```

**`:features:device-linking` evolves into Dispositivos.** It already owns the device list and the pairing dialog, and the task says to replace the previous implementation. `DeviceLinkingContent` is rebuilt as cards, `PairDeviceDialog` is kept, and the route and graph names stay the same, so `:app` changes are minimal.

**`:features:auth` gains `BiometricUnlockScreen`**, since auth owns session entry.

*Alternatives rejected:*
- *One module per sub-screen (seven modules):* each would share the same top bar, rows and formatters through `:core:ui` anyway, and the build would gain seven near-empty modules for one ClickUp task.
- *Devices inside `:features:profile`:* this would duplicate the pairing dialog, or make a feature depend on another.

Dependency flow:

```
:app → :features:{profile,device-linking,auth} → :core:{domain,ui,common}
:core:data → :core:{domain,network}
```

### Decision 2 — Shared settings UI in `:core:ui`

The screens share a visual grammar, so these move into `core/ui/.../component/settings/`, domain-free as the sales components already are:

```kotlin
@Composable fun QhathiaGradientTopBar(title: String, onBack: (() -> Unit)?, actions: @Composable RowScope.() -> Unit = {})
@Composable fun QhathiaSettingsSection(title: String?, content: @Composable ColumnScope.() -> Unit)  // group label + rounded card
@Composable fun QhathiaNavigationRow(icon: ImageVector?, title: String, subtitle: String?, onClick: (() -> Unit)?, comingSoon: Boolean = false, trailing: …)
@Composable fun QhathiaSwitchRow(title: String, subtitle: String?, checked: Boolean, onCheckedChange: ((Boolean) -> Unit)?, enabled: Boolean = true)
@Composable fun QhathiaComingSoonTag()
@Composable fun QhathiaInitialsAvatar(initials: String, size: Dp)
@Composable fun QhathiaPlanProgressBar(fraction: Float, startLabel: String, endLabel: String)
```

- `QhathiaSwitchRow` merges its semantics into one `toggleable` node with `Role.Switch`.
- The `comingSoon` flag disables the row and appends "Próximamente" to its content description.
- The switch colours use a `switchChecked` success token added to `QhathiaColors`.
- The existing `QhathiaSegmentedControl` covers the language selector: two outlined pills with the selected one filled in primary.

### Decision 3 — Domain and data

New or extended domain types in `:core:domain`:

```kotlin
data class UserProfile(val id: String, val fullName: String, val email: String, val phone: String?,
                       val passwordUpdatedAt: Instant?, val role: UserRole, val storeCount: Int)
data class Subscription(val planType: String, val planName: String, val status: SubscriptionStatus,
                        val priceMonthlyPen: BigDecimal, val productLimit: Int?, val features: PlanFeatures,
                        val startedAt: Instant?, val endsAt: Instant?)
data class NotificationPreferences(val values: Map<NotificationPreference, Boolean>, val activeCount: Int, val totalCount: Int)
enum class NotificationPreference { NEW_ORDER, ORDER_EXPIRING, TABLET_DISCONNECTED, PRODUCT_OUT_OF_STOCK, LOW_BATTERY, WEEKLY_REPORT, SELLING_SUGGESTIONS }
data class ActiveSession(val sessionId: String, val deviceLabel: String, val lastUsedAt: Instant?, val isCurrent: Boolean)
data class StoreSettings(val storeId: String, val language: AppLanguage?, val pricesIncludeIgv: Boolean, val igvRate: BigDecimal)
enum class AppLanguage(val tag: String) { SPANISH("es"), ENGLISH("en") }
// Device gains: name, batteryLevel: Int?, networkStatus: String?, storeName: String?, lastPingAt: Instant?, lastSyncAt: Instant?
data class DeviceCommand(val id: String, val type: DeviceCommandType, val status: DeviceCommandStatus,
                         val requestedAt: Instant?, val resultDetail: String?)
// Session gains: sessionId: String?
```

Repository ports:
- `UserProfileRepository.getProfile()`
- `SubscriptionRepository.getCurrent(): Result<Subscription?>`, where `404` maps to `null`
- `NotificationPreferencesRepository.get()` / `.set(pref, enabled)`
- `AccountSecurityRepository.getSessions()`, `.revokeSession(id)`, `.revokeOtherSessions()`, `.changePassword(current, new)`. Failures carry `ChangePasswordException(ChangePasswordError)`.
- `StoreSettingsRepository.get()` / `.updateLanguage(lang)` / `.updatePricesIncludeIgv(bool)`, for the active store id from the session.
- `DeviceRepository` gains `getCatalogCount()`, `sendCommand(deviceId, type)`, `getCommands(deviceId)`.
- `AppPreferencesRepository`: `observeLanguage(): Flow<AppLanguage?>`, `setLanguage`, `observeBiometricEnabled(): Flow<Boolean>`, `setBiometricEnabled`.

Use cases (one per action, as the codebase does):
- `GetUserProfileUseCase`, `GetSubscriptionUseCase`
- `GetNotificationPreferencesUseCase`, `SetNotificationPreferenceUseCase`
- `GetActiveSessionsUseCase`, `RevokeSessionUseCase`, `RevokeOtherSessionsUseCase`, `ChangePasswordUseCase`
- `GetStoreSettingsUseCase`, `SetAppLanguageUseCase`, `SetPricesIncludeIgvUseCase`, `ObserveAppLanguageUseCase`
- `SetBiometricEnabledUseCase`, `ObserveBiometricEnabledUseCase`
- `SendDeviceCommandUseCase`, `GetDeviceCommandsUseCase`, `GetCatalogCountUseCase`

`SetAppLanguageUseCase` owns the ordering: persist locally, then, if the role is OWNER/ADMIN, a best-effort `updateLanguage`. Its result reports only the local write.

`:core:network`:
- New `UserApiService`, `SubscriptionApiService`, `StoreApiService`, `CatalogApiService`, all on the authenticated client.
- `AuthApiService` gains `getSessions(refreshToken)`, `revokeSession(id)` and `revokeOthers(refreshToken)`, which set `X-Refresh-Token`, plus `changePassword(body)`.
- `NotificationApiService` gains `getPreferences()` and `updatePreferences(body)`. The body DTO has all seven fields nullable, with `explicitNulls = false`, so only the changed key is sent.
- `DeviceApiService` gains `getCatalogCount`, `sendCommand` and `getCommands`.
- `DeviceDto` gains the §18 fields; `LoginRequestDto` gains `deviceLabel: String?`; `AuthResponseDto` gains `sessionId: String?`.

`:core:data`:
- Mappers parse ISO timestamps to `Instant?` (unparseable → null) and money to `BigDecimal`. Unknown enum values map to `UNKNOWN` or `null`.
- `PersistedSession` gains `sessionId: String? = null`, so older blobs still decode. The refresh path copies it over.
- `AuthRepositoryImpl.login` takes the label from a `DeviceLabelProvider` (`Build.MANUFACTURER` + `Build.MODEL`, de-duplicated when the model already starts with the manufacturer, max 100).
- `AppPreferencesRepositoryImpl` uses a **separate** Preferences DataStore file, `qhathia_app_prefs`, so clearing the session never touches it.

### Decision 4 — Language: device-owned, applied through AppCompat per-app locales

- **Apply:** `AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))`.
  - On API 33+ this delegates to the system `LocaleManager`, which persists per app.
  - On API 26–32 AppCompat persists it when the manifest declares `androidx.appcompat.app.AppLocalesMetadataHolderService` with `autoStoreLocales=true`.
  - `res/xml/locales_config.xml` (es, en) plus `android:localeConfig` also exposes the choice in system settings on 33+.
- **Source of truth:** the app DataStore value. On app start, `QhathiaStaffApp` reconciles it: a system-settings change made on 33+ is read back with `getApplicationLocales()` and persisted, so both agree. With no value, the default follows the system language (English → `en`, else `es`) and is persisted.
- **Applying lives in `:app`** (`AppLocaleController`, observing `ObserveAppLanguageUseCase`), so `:core:data` stays free of AppCompat and features never touch Activity APIs.
- **Backend mirror:** fire-and-forget `PUT /stores/{id}` for OWNER/ADMIN. Configuración re-sends it when opened if the store's `language` differs from the local value. The store response never writes to the local value.
- **Translations:** `values-en/strings.xml` for every module with strings, including plurals. Spanish stays the default `values/`, so a missing translation falls back to Spanish instead of crashing. Dates and numbers use the current app locale (`es-PE` / `en-US`).
- `MainActivity` becomes an `AppCompatActivity`, and the theme parent becomes `Theme.AppCompat.DayNight.NoActionBar`. Compose draws everything, so this is invisible. It is required for locale recreation on < 33 and for `BiometricPrompt`.

*Alternatives rejected:*
- *Wrapping `Context` with a custom `Locale` in `attachBaseContext`:* hand-rolled, breaks on WebView/system dialogs, and duplicates what AppCompat maintains.
- *Backend as the source:* contradicts the task ("exclusivamente configurado desde el device"), and the contract says the store `language` is the kiosk's.

### Decision 5 — Fingerprint: setting in Seguridad, gate in the root

**Capability check.** `BiometricManager.from(context).canAuthenticate(BIOMETRIC_STRONG or BIOMETRIC_WEAK)` maps to `Available | NoneEnrolled | NoHardware | Unavailable`. It lives in `:core:ui` as `rememberBiometricAuthenticator()`, which returns an object with `availability()` and `authenticate(title, subtitle, negativeText): BiometricResult`. `:core:ui` gains `androidx.biometric`. The prompt needs a `FragmentActivity`, obtained from `LocalActivity`. This keeps features on their allowed dependencies. The authenticator is a UI concern that holds no business rules and no credentials.

**Enable flow** (`SecurityViewModel`), driven by UI events:
- The screen checks availability and sends `BiometricToggleRequested(availability)`.
- `Available`: the ViewModel emits `UiEffect.PromptBiometric`, and the screen reports `BiometricResult`. Success → `SetBiometricEnabledUseCase(true)`.
- `NoneEnrolled`: `UiEffect.ShowEnrollHint` → snackbar/dialog "Configura tu huella…" with an action launching `Settings.ACTION_BIOMETRIC_ENROLL` (API 30+), else `ACTION_SECURITY_SETTINGS`.
- `NoHardware` / `Unavailable`: a message, and the toggle stays off.
- On every resume, if the preference is on and availability is no longer `Available`, the preference is cleared.

**No `CryptoObject`.** The token is already encrypted with the Keystore (`KeystoreSessionCipher`). The gate is a presence check before revealing an already-restored session, not a key release. Binding the session key to biometric auth would lose the session every time enrollment changes.

**Gate.** `MainViewModel.authState` gains `Locked`, computed as `session != null && biometricEnabled && !unlockedThisProcess`.
- `unlockedThisProcess` is reset by a `ProcessLifecycleOwner` observer when the app has been in the background at least 5 minutes. The time it went to background is held in memory; it is lost on process death, and a cold start is locked anyway.
- `QhathiaRoot` renders `BiometricUnlockScreen` from `:features:auth` for `Locked`.
- A notification's `pendingDestination` is kept, and honoured by the shell after unlock.
- "Ingresar con contraseña" runs `LogoutUseCase`; the biometric preference is untouched.
- `NoneEnrolled` at the gate: clear the preference, show the message, and "Continuar" → logout.

### Decision 6 — Navigation

`ProfileNavigation.kt` exposes:
- `PROFILE_ROUTE = "profile"` (the tab root) and one route per sub-screen: `profile/personal`, `profile/notifications`, `profile/security`, `profile/security/password`, `profile/settings`, `profile/subscription`, `profile/help`.
- `fun NavGraphBuilder.profileGraph(navController: NavController, onNavigateToDevices: () -> Unit, onNavigateToOrderRequests: () -> Unit, isDevicesAvailable: …)`.

Destinations inside the graph use `navController.navigate(...)` / `popBackStack()`. Devices is handed to `:app`, which navigates to `DEVICE_LINKING_ROUTE`.

- **Bottom bar:** `MainShell` detects top-level routes by comparing `destination.route` across the hierarchy. The sub-routes are flat composables (not nested under a `navigation(route = PROFILE_ROUTE)` graph), so only `profile` itself matches and the bar hides on every sub-screen.
- **Returning to Mi perfil:** Mi perfil refreshes its live subtitles when it becomes `RESUMED` again (`LifecycleResumeEffect` → `Refresh` event), so toggles changed in Notificaciones show on return without a shared cache.
- **Change password → Seguridad:** the password screen returns a `password_changed` result through the previous entry's `savedStateHandle`, the same pattern Inventario uses for registration.
- `:app` deletes `ui/profile/*` and the `profile_*` strings it no longer uses.

### Decision 7 — MVI contracts (per screen)

All screens use `StateFlow<UiState>` + `onEvent` + a `SharedFlow<UiEffect>` for navigation and snackbars, following the existing features.
- **Hub:** `ProfileHubUiState(profile: LoadState<UserProfile>, fallbackName/email from Session, role, subscription: LoadState<Subscription?>, activeAlerts: Pair<Int,Int>?, connectedTablets: Int?, showLogoutConfirm)`. The four loads run in parallel (`supervisorScope`), and each failure is local. Subscription and devices are skipped for SELLER.
- **Notificaciones:** a per-key `saving: Set<NotificationPreference>`. Optimistic flip, `PUT` with a single key, adopt the response on success, revert that key and emit `ShowError` on failure.
- **Seguridad:** `sessions: LoadState<List<ActiveSession>>`, `biometricEnabled` (from Flow), `passwordUpdatedAt`, `pendingRevoke: ActiveSession?`, `showRevokeOthersConfirm`. Current-session resolution: `isCurrent || sessionId == session.sessionId`, sorted current first, then `lastUsedAt` descending.
- **Cambiar contraseña:** field values, visibility toggles, per-field errors, `isSubmitting`. Validation matches the spec. `ChangePasswordError { WrongCurrent, SameAsCurrent, Validation, Network, Unknown }`.
- **Configuración:** `language` (from the local Flow), `store: LoadState<StoreSettings>`, `canEditStore = role in OWNER/ADMIN`, `igvSaving`. Language events call `SetAppLanguageUseCase`; the locale switch happens in `:app` as a reaction to the Flow.
- **Dispositivos:** `devices: LoadState<List<DeviceCardModel>>`, `catalogCount: Int?`, `commands: Map<deviceId, Map<DeviceCommandType, DeviceCommand>>`, `pendingRestart: deviceId?`, pairing dialog state (existing).
  - Polling is a `viewModelScope` job started when any command is in flight. It runs only while the screen is started: the screen sends `ScreenStarted` / `ScreenStopped` from `LifecycleStartEffect`. It ticks every 15 s and ends when nothing is in flight.
  - "Connected" = `status == ACTIVE && now - lastPingAt <= 10 min`, with a `Clock` injected for tests.

### Decision 8 — Formatting and Próximamente

- `ProfileFormatters`:
  - Relative time ("hace 4 min", "hace 2 horas", "hace 4 meses", "hace un momento") from `Instant` and an injected `Clock`, localized through plurals.
  - Dates `d 'de' MMMM 'de' yyyy` / `d MMM yyyy` in the app locale, zone `America/Lima`.
  - `+51 999 888 777` from 9 digits.
  - Initials: the first letters of the first two words, uppercased, or "?" when blank.
  - Role labels: OWNER "Dueño(a)", SELLER "Vendedor(a)", ADMIN "Administrador(a)".
- Money: `S/ 89` when the value is integral, else `S/ 89.50`. This reuses `toSolesLabel` from `:core:ui` sales formatters.
- Próximamente elements are rendered by `QhathiaComingSoonTag` plus a disabled row/button, and send no event to ViewModels. Their list is in memory `profile-backend-gaps`.
- FAQ answers are string resources in `:features:profile` (es + en). They are drafted here and flagged for product review in Open Questions.

## Risks / Trade-offs

- **[`ComponentActivity` → `AppCompatActivity` and theme change]** → Possible visual regressions in system-drawn surfaces (status bar, dialogs). Mitigation: Compose owns all UI, and edge-to-edge is kept. Manually check login, a dialog and the photo picker in light and dark.
- **[Locale switch recreates the Activity]** → In-flight screen state could be lost. Mitigation: ViewModels survive recreation, and the switch only happens from Configuración, whose state is trivial. Verify back stack preservation after switching.
- **[English copy quality]** → The translations are drafted by the implementer. Mitigation: product review listed as an open question. Spanish remains the fallback for any missing key.
- **[Connection threshold guessed at 10 min]** → Tablets could show "Desconectada" while alive. Mitigation: one constant, and the gap is recorded in memory for backend to document the heartbeat interval.
- **[Biometric gate vs. background timer lost on process death]** → A killed app always asks again on cold start. That is the stricter side, so accepted.
- **[Biometric enrollment changes]** → The preference could promise a gate that cannot run. Mitigation: re-checked on Seguridad resume and at the gate; never a dead end, because the gate falls back to password sign-in.
- **[Command polling cost]** → 15 s polls per in-flight device. Mitigation: only while visible and while in flight; commands expire at 10 min server-side, so polling self-terminates.
- **[Catalog count is store-wide]** → Every tablet card shows the same "N productos". Accepted and recorded in backend gaps.
- **[`X-Refresh-Token` header exposes the refresh token to one more endpoint]** → It is our own backend over the same authenticated channel, and the contract requires it for `current` and revoke-others. Mitigation: never logged. The Ktor logging level already hides headers in release; add the header to the sanitized list in debug.
- **[Stored sessions without `sessionId`]** → "este equipo" cannot be matched until the next login. Mitigation: the header-based `current` flag covers it; the ID comparison is a second path.

## Migration Plan

- `PersistedSession.sessionId` defaults to null, so existing installs keep their session with no forced logout.
- Existing merchants have no language value. On first start after the update, the default is derived from the system language and persisted, so Spanish-system users see no change.
- Rollback: revert the change. The `qhathia_app_prefs` DataStore file is ignored by older builds, and AppCompat-stored locales on < 33 remain harmless because only `values/` (Spanish) would exist.

## Open Questions

- Product review of the English strings and the four FAQ answers.
- Backend: the heartbeat interval, to confirm the 10-minute "Conectada" threshold.
- Backend gaps in memory `profile-backend-gaps`: photo, payments, plan bullets, "Clientes hoy", per-tablet catalog, help content, support WhatsApp.
- Does the Mi perfil bell go to Solicitudes (assumed, as on Inicio) or to a future notifications inbox?
- Role label wording ("Dueño(a)" vs. a gendered label like the design's "Dueña"): the backend exposes no gender.
