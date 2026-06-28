## Context

The Qhathia staff app is in a single-module transitional state — `settings.gradle.kts` declares only `:app`, and all code lives in packages under `:app`. The project standards (`docs/android/android-frontend-standards.md`) mandate Clean Architecture + MVI across **separate Gradle modules**, never packages inside `:app`. Login is the first real feature, so it must also bootstrap the foundational `core:*` modules every later feature will reuse.

The Auth API (`docs/android/api-contract.md`) exposes `POST /api/v1/auth/login` (public) returning `accessToken`, `refreshToken`, `expiresIn`, `user`, `store`, and `stores[]`, and `POST /api/v1/auth/refresh` (public, token rotation). Base URL is `http://87.99.156.164` — plain HTTP, so cleartext traffic must be permitted. Access token TTL ~24h; refresh ~7d.

Constraints: Kotlin + Jetpack Compose, Hilt (KSP2, never kapt), Ktor CIO, kotlinx-serialization, StateFlow/SharedFlow, JUnit5 + MockK + Turbine. Target is **phones only**.

## Goals / Non-Goals

**Goals:**
- Working email/password login wired to the real Auth API with validation, error mapping, and the loading/disabled states from the spec.
- A reusable session layer: encrypted token storage, observable auth state, bearer-token injection, transparent refresh with rotation.
- Establish the `core:*` and `features:auth` module skeleton with correct dependency wiring per the Dependency Graph Rules.

**Non-Goals:**
- Home screen UI (only a placeholder destination route).
- Registration, password change/recovery, multi-store switch UI, biometric login.
- Offline caching beyond the session itself.

## Decisions

### Module layout

Create these Gradle modules (each a root-level subproject registered in `settings.gradle.kts`):

| Module | Type | Responsibility |
|---|---|---|
| `core:common` | Android library | `Result`/dispatcher utilities, constants, extension functions |
| `core:network` | Android library | Ktor `HttpClient` singleton, `NetworkResult`, Auth DTOs, `AuthApiService`, auth/refresh Ktor plugins |
| `core:domain` | Pure Kotlin (JVM) | `Session`, `User`, `Store` domain models; `AuthRepository`, `SessionRepository` ports; `LoginUseCase`, `ObserveSessionUseCase`, `LogoutUseCase` |
| `core:data` | Android library | `AuthRepositoryImpl`, `SessionRepositoryImpl`, DTO↔domain mappers, encrypted `SessionLocalDataSource` (DataStore + Keystore cipher) |
| `core:ui` | Android library | `AppTheme`, color/typography tokens (DS: Urbanist, `#0a2540` primary, `#d93b00` error, etc.), shared `QhathiaTextField`, `QhathiaButton` components |
| `core:testing` | Android library | `MainDispatcherRule`, fakes (`FakeAuthRepository`, `FakeSessionRepository`), fixtures |
| `features:auth` | Feature (Compose+Hilt) | `LoginScreen`/`LoginContent`, `LoginViewModel`, `LoginUiState/Event/Effect`, auth nav graph |

Dependency directions follow the standard: `features:auth → core:domain, core:ui, core:common`; `core:data → core:domain, core:network`; `core:network` stays free of domain types (map at the data boundary). `:app` depends on `features:auth` + all `core:*`, owns the manifest, `NavHost`, BuildConfig fields, and the Hilt `@HiltAndroidApp` Application.

**Alternative considered:** keep login as packages in `:app` for speed — rejected; it violates the mandatory module rule and would force a painful refactor for the next feature.

### Session storage — DataStore + Keystore cipher

Store the session in a `Preferences`/`Proto` DataStore with token fields encrypted via an Android Keystore-backed cipher. The standards explicitly steer away from the unmaintained `EncryptedSharedPreferences`/`security-crypto` for new work in favor of DataStore + a Keystore cipher. `SessionRepository` exposes the session as a cold `Flow`, lifted to a `StateFlow<AuthState>` for observers.

**Alternative considered:** `EncryptedSharedPreferences` — rejected per standards (library unmaintained, alpha-only API).

### Token injection & refresh via Ktor `Auth` plugin

Use Ktor's `Auth` plugin with `bearer { loadTokens { … }; refreshTokens { … } }`. `loadTokens` reads the current tokens from the session store; `refreshTokens` calls `/api/v1/auth/refresh`, persists the rotated pair, and returns the new tokens. On refresh `401`, clear the session. Login and refresh endpoints themselves are public and excluded from the auth provider. This keeps token logic out of use-cases and ViewModels, per the Network Standards.

**Alternative considered:** a manual response interceptor / per-call header — rejected; duplicates auth logic across call sites, which the standards forbid.

### Error model

`AuthApiService` wraps Ktor calls in `runCatching { … }.toNetworkResult()` returning `NetworkResult<T>` (`Success` / `HttpError(code,msg)` / `NetworkError(cause)`). `AuthRepositoryImpl` maps this to a domain `Result<Session>` and a sealed `LoginError` (InvalidCredentials=401, Validation=400, Network, Unknown) so the ViewModel can choose copy. Timeouts: `requestTimeoutMillis = 30s`, `connectTimeoutMillis = 15s` (per standards' Ktor config), tuned for mobile.

### MVI for the screen

`LoginViewModel` exposes `StateFlow<LoginUiState>` (email, password, field errors, `isSubmitting`, `passwordVisible`) and `SharedFlow<LoginUiEffect>` (`NavigateToHome`, `ShowError`). `LoginScreen` is the stateful composable; `LoginContent` is stateless and previewable (light + dark, required by Definition of Done). Validation runs in the ViewModel before dispatching the use-case.

## Risks / Trade-offs

- **Cleartext HTTP to a fixed IP** → Production has no HTTPS. Mitigation: scope `usesCleartextTraffic` / a network-security-config to that host only; isolate `BASE_URL` in BuildConfig so a future HTTPS domain is a one-line change. Flag the security debt.
- **Multi-store login returns `store=null`** when a user has 2+ stores (JWT carries no active store) → Mitigation: session layer treats `activeStore` as nullable; for this change, navigate to Home regardless. The switch-store flow is a separate future change; document the gap.
- **Bootstrapping 7 modules at once is heavy** → Mitigation: create them minimal (only what login needs); empty `AndroidManifest.xml` for libraries; verify with a build after wiring before implementing feature code.
- **Hilt on AGP 9 / Kotlin 2.3 metadata mismatch** → Mitigation: add `ksp(libs.kotlin.metadata.jvm)` to every Hilt module and the documented `gradle.properties` flags, per the version-catalog notes.
- **Keystore cipher complexity** → Mitigation: keep the crypto in a single `SessionLocalDataSource`; if Keystore setup blocks progress, fall back to plain DataStore behind the same interface and track hardening as follow-up (tokens still process-private).

## Migration Plan

Additive only — no existing behavior changes. Order: scaffold modules → wire `settings.gradle.kts` and `:app` deps → build green → implement core (network/session) → implement feature → tests → manifest + BuildConfig. Rollback is simply not shipping; nothing pre-existing is modified beyond `:app` wiring and manifest additions.

## Open Questions

- Exact Home destination contract — assumed a single placeholder route owned by `:app` for now.
- Whether to proactively refresh near expiry vs. lazily on `401` — starting with lazy-on-401 + expiry check; revisit if UX shows a noticeable first-call delay.
