## 1. Module scaffolding — core layers

- [x] 1.1 Create `core/common/` dir tree (`src/main/java/com/qhathia/mobile/staff/core/common`, `src/test/...`)
- [x] 1.2 Write `core/common/build.gradle.kts` (Android Library template), add empty `src/main/AndroidManifest.xml`
- [x] 1.3 Create `core/network/` dir tree; write `build.gradle.kts` (Android Library + serialization + ksp + hilt), add `AndroidManifest.xml`
- [x] 1.4 Create `core/domain/` dir tree; write `build.gradle.kts` (Pure Kotlin `kotlin("jvm")` template — no Android plugin)
- [x] 1.5 Create `core/data/` dir tree; write `build.gradle.kts` (Android Library + ksp + hilt), add `AndroidManifest.xml`
- [x] 1.6 Create `core/ui/` dir tree; write `build.gradle.kts` (Android Library + compose), add `AndroidManifest.xml`
- [x] 1.7 Create `core/testing/` dir tree; write `build.gradle.kts` (Android Library), add `AndroidManifest.xml`
- [x] 1.8 Create `features/auth/` dir tree; write `build.gradle.kts` (Feature template: compose + ksp + hilt), add `AndroidManifest.xml`
- [x] 1.9 Register all new modules in root `settings.gradle.kts` via `include(...)`
- [x] 1.10 Add Hilt `gradle.properties` flags + `ksp(libs.kotlin.metadata.jvm)` to every Hilt module (per version-catalog notes)
- [x] 1.11 Run `./gradlew help` (or sync) to confirm the new module graph resolves before writing feature code

## 2. core:domain — models, ports, use cases

- [x] 2.1 Add domain models: `Session`, `User`, `Store`, `UserRole` (enum)
- [x] 2.2 Add `AuthRepository` port (`login`, `refresh`) and `SessionRepository` port (`observeSession`, `saveSession`, `clearSession`, `currentAccessToken`)
- [x] 2.3 Add sealed `LoginError` (InvalidCredentials, Validation, Network, Unknown)
- [x] 2.4 Add `LoginUseCase`, `ObserveSessionUseCase`, `LogoutUseCase`
- [x] 2.5 Unit-test `LoginUseCase` against a fake `AuthRepository`

## 3. core:network — Ktor client, DTOs, Auth API

- [x] 3.1 Add `NetworkResult<T>` sealed type + `Result.toNetworkResult()` extension
- [x] 3.2 Add `@Serializable` DTOs: `LoginRequestDto`, `RefreshRequestDto`, `AuthResponseDto`, `UserDto`, `StoreDto`
- [x] 3.3 Add `KtorClientFactory` / `NetworkModule` providing the `HttpClient` (CIO, ContentNegotiation, HttpTimeout 30s/15s, Logging BODY in debug / NONE in release, `defaultRequest` BASE_URL)
- [x] 3.4 Install Ktor `Auth` `bearer { loadTokens / refreshTokens }` wired to a token provider interface (impl supplied by core:data); exclude `/auth/login` and `/auth/refresh` from the auth provider
- [x] 3.5 Add `AuthApiService` (`login`, `refresh`) returning `NetworkResult<AuthResponseDto>`
- [x] 3.6 Unit-test `AuthApiService` mapping for 200 / 401 / 400 / network error (Ktor MockEngine)

## 4. core:data — repositories, mappers, encrypted session store

- [x] 4.1 Add DTO→domain mappers (`AuthResponseDto.toSession()` etc.)
- [x] 4.2 Implement `SessionLocalDataSource` using DataStore + Android Keystore-backed cipher for token fields
- [x] 4.3 Implement `SessionRepositoryImpl` (observe as Flow, save, clear, expose access token + token provider for core:network)
- [x] 4.4 Implement `AuthRepositoryImpl` mapping `NetworkResult` → `Result<Session>` / `LoginError`; on refresh failure clear session
- [x] 4.5 Add `DataModule` (`@Binds` repositories, `@Provides` datastore)
- [x] 4.6 Unit-test `AuthRepositoryImpl` error mapping and `SessionRepositoryImpl` persist/restore/clear

## 5. core:ui — theme + DS components

- [x] 5.1 Add color tokens (text-primary `#0a2540`, text-secondary `#616161`, text-disabled `#9e9e9e`, border-default `#e0e0e0`, border-strong `#9e9e9e`, border-focus `#0a2540`, status-error `#d93b00`, bg-surface white) and Urbanist typography scale (Body MD 14/20, SM 12/18)
- [x] 5.2 Add `AppTheme` (light + dark) Material 3
- [x] 5.3 Add `QhathiaTextField` (placeholder-only, leading icon, 8dp radius, 40dp height, states Empty/Filled/Focused/Error/Disabled, optional password masking + visibility toggle)
- [x] 5.4 Add `QhathiaButton` (primary) and a loading variant that swaps to a `CircularProgressIndicator`
- [x] 5.5 Add `@Preview` (light + dark) for the components

## 6. features:auth — login screen (MVI)

- [x] 6.1 Add `LoginUiState`, `LoginUiEvent`, `LoginUiEffect`
- [x] 6.2 Add `LoginViewModel` (`@HiltViewModel`): client-side email/password validation, calls `LoginUseCase`, `isSubmitting` state, maps `LoginError`→message, emits `NavigateToHome`/`ShowError`
- [x] 6.3 Add `LoginContent` stateless composable: Qhathia wordmark, email + password `QhathiaTextField`, login button; while submitting show progress in place of the button and disable inputs
- [x] 6.4 Add `LoginScreen` stateful composable binding the ViewModel + collecting effects
- [x] 6.5 Add `@Preview` (light + dark) for `LoginContent` incl. error and submitting states
- [x] 6.6 Add auth nav graph (`loginRoute`) exposing navigation to Home
- [x] 6.7 Add `AuthModule` if any feature-scoped bindings are needed
- [x] 6.8 Unit-test `LoginViewModel` (validation blocks request, success emits NavigateToHome, 401 shows error, submitting toggles) with Turbine + fakes

## 7. app — wiring, navigation, manifest, config

- [x] 7.1 Add `@HiltAndroidApp` Application class and `@AndroidEntryPoint` on the host Activity
- [x] 7.2 Add `:app` dependencies on `features:auth` + all `core:*` modules
- [x] 7.3 Add `BASE_URL` (`http://87.99.156.164`) and `DEBUG`-gated logging BuildConfig fields
- [x] 7.4 Add `NavHost` with start destination = login when unauthenticated, Home placeholder route on success; observe session for start-destination decision
- [x] 7.5 Add a minimal Home placeholder destination (route only — no feature UI)
- [x] 7.6 Add `INTERNET` + `ACCESS_NETWORK_STATE` permissions and host-scoped cleartext/network-security-config for `87.99.156.164` to `app/src/main/AndroidManifest.xml`

## 8. Verification — Definition of Done

- [x] 8.1 `./gradlew test` green across new modules
- [x] 8.2 `./gradlew lint` passes
- [x] 8.3 `./gradlew assembleDebug` builds; manual smoke: valid login → Home, wrong password → error, airplane mode → connectivity error
- [x] 8.4 Confirm tokens are stored encrypted and password is never logged (Logging=NONE in release)
- [x] 8.5 Confirm `LoginContent` has working light + dark `@Preview`
