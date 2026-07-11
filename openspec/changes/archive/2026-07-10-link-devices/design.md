## Context

The staff (merchant operational) app is a multi-module Clean Architecture + MVI codebase (`:app`, `core:*`, `features:*`). Auth already persists an OWNER JWT that the authenticated Ktor client injects automatically. The Profile tab in `MainShell` is currently a `PlaceholderScreen`; the bottom nav bar is hidden automatically for any route that is not one of the five top-level destinations.

This POC adds the first device-management surface: list the store's linked tablets and pair a new one by approving its pairing code.

**API constraints (from `docs/android/api-contract.md`, section 5):**
- `GET /api/v1/devices` — OWNER/ADMIN JWT; returns an array of the store's devices (`id`, `storeId`, `status`, `androidVersion`, `appVersion`, `lastPingAt`, `linkedAt`, ...). This is the list source (confirmed with the requester, overriding the task's `/devices/me`, which is DEVICE-role and single-record).
- `POST /api/v1/devices/pair` — OWNER/ADMIN JWT; body `{ pairingToken, decision: APPROVE|REJECT, deviceName, platform: ANDROID, appVersion, deviceFingerprint? }`. In the inverted pairing flow the tablet displays the 5-char code; the owner enters it here to approve. Returns `{ sessionId, status: APPROVED, storeId, deviceId, deviceName, platform }`. Errors: `400 INVALID_PAIRING_TOKEN | PAIRING_TOKEN_EXPIRED | PAIRING_TOKEN_ALREADY_USED`, `409 CATALOG_THRESHOLD_NOT_MET`, `503 CATALOG_READINESS_UNAVAILABLE`.

## Goals / Non-Goals

**Goals:**
- A full-screen (no bottom bar) Linked Devices screen reachable from Profile.
- List paired devices with a bold top-left title, empty state, and loading/error handling.
- Floating + button → near-full-screen dialog with a code input + submit that approves the pairing and, on success, reloads the list.
- Follow existing module/layer patterns exactly (mirror `features:product-inventory`, `core:network` service pattern, `NetworkResult → Result` mapping at the data boundary).

**Non-Goals:**
- No WebSocket status handling, DEVICE-token exchange, unlink, rename, heartbeat, or the legacy QR flow.
- No deep error taxonomy in the UI beyond "show an error" (POC): a single generic error message per the task.
- No new Profile screen redesign — only wire an entry point from the existing Profile placeholder.
- No device-detail screen.

## Decisions

**1. New `features/device-linking` Gradle module.** Per project rule 4, features are proper modules, never packages in `:app`. It mirrors `features/product-inventory` (Compose + Hilt + `core:domain`/`core:ui`/`core:common`). Namespace `com.qhathia.mobile.staff.feature.devicelinking`.

**2. Device domain layer in `core:domain`.** Add `Device` model, `DeviceRepository` port with `getDevices(): Result<List<Device>>` and `pairDevice(...): Result<Unit>`, plus `GetDevicesUseCase` and `PairDeviceUseCase`. Keeps the feature framework-free and testable, consistent with `GetProductsUseCase`/`RegisterProductUseCase`. A small `PairDeviceError` (generic for POC) mirrors the existing `RegisterProductError` pattern.

**3. `DeviceApiService` in `core:network` on the authenticated client.** Two suspend methods returning `NetworkResult<T>` via `runCatching { ... }.toNetworkResult()`, exactly like `ProductApiService`. DTOs (`DeviceDto`, `PairDeviceRequestDto`, `PairDeviceResponseDto`) are `@Serializable` with `@SerialName`. Provided in `NetworkModule` with `@AuthenticatedClient` (OWNER JWT auto-injected).

**4. `DeviceRepositoryImpl` in `core:data`.** Maps `NetworkResult → Result` at the boundary and DTO→domain via a `DeviceMappers.kt`, bound in `DataModule` with `@Binds @Singleton`. `pairDevice` maps a successful `APPROVE` response to `Result.success(Unit)` and any HTTP/network error to `Result.failure`.

**5. Pair request metadata defaults (POC).** The dialog only collects the code. `deviceName` defaults to a constant (e.g. "Tablet"), `platform = "ANDROID"`, `appVersion` from `BuildConfig.VERSION_NAME` (or a constant passed down), `decision = "APPROVE"`, `deviceFingerprint` omitted. Rationale: the task's form is code-only; richer metadata capture is out of scope for the POC. *Alternative considered:* prompt for a device name in the dialog — rejected to keep the POC form minimal as specified.

**6. Navigation: register the screen inside `MainShell`'s inner NavHost as a non-top-level route.** Because `showBottomBar` is true only for the five top-level destinations, any other route hides the bar automatically — satisfying "sin navbar" with zero changes to the bar logic. The Profile placeholder gets an `onNavigateToLinkedDevices` callback wired in `MainShell`. *Alternative considered:* a nested `deviceLinkingGraph` route on the outer `QhathiaNavHost` — rejected because the bottom bar lives in `MainShell`, so hosting there is where the "hide bar" behavior already works. The feature still exposes a `deviceLinkingGraph(onBack)` extension (mirroring `inventoryGraph`/`productRegisterGraph`) that `MainShell` calls.

**7. MVI contract in the feature.** `DeviceLinkingUiState` (`devices`, `isLoading`, `errorMessage`, `isPairDialogVisible`, `pairCode`, `isPairing`, `pairErrorMessage`), `DeviceLinkingUiEvent` (`Load`/`Retry`, `OpenPairDialog`, `DismissPairDialog`, `PairCodeChanged`, `SubmitPair`), `DeviceLinkingUiEffect` (optional, e.g. none needed since reload is state-driven). Stateless `DeviceLinkingContent` + `@Preview` (light/dark), stateful `DeviceLinkingScreen` binding the `@HiltViewModel`. On successful pair the ViewModel closes the dialog and re-invokes `GetDevicesUseCase`.

**Module dependency additions (respecting the graph):**
- `features:device-linking → core:domain, core:ui, core:common`
- `core:data → core:domain, core:network` (already)
- `:app → features:device-linking`

## Risks / Trade-offs

- **[Task/contract endpoint mismatch]** Task text says `/devices/me`; using `GET /api/v1/devices` instead. → Confirmed with requester; documented in proposal and here.
- **[Pair metadata is stubbed]** Default `deviceName`/`appVersion` may not be meaningful. → Acceptable for POC; a follow-up can add a name field and real app-version wiring.
- **[Coarse error handling]** 400/409/503 are collapsed into one generic message. → Matches the task's "por lo pronto mostrar error"; a later change can map `PAIRING_TOKEN_EXPIRED`/`CATALOG_THRESHOLD_NOT_MET` to specific copy.
- **[No realtime confirmation]** The staff side approves but does not observe the tablet's WebSocket outcome. → Out of scope; the `POST /pair` 200 is treated as success.

## Migration Plan

Additive only — no schema/data migration, no breaking API change. New module + new authenticated calls behind the existing OWNER session. Rollback = revert the change; no persisted state is introduced.

## Open Questions

- Should the pairing dialog also let the owner name the tablet, or keep the default for the POC? (Currently: default name, per task's minimal form.)
- Should specific pairing errors (expired code, catalog threshold) get tailored copy later? (Deferred.)
