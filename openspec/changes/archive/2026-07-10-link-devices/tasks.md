## 1. Domain layer (`core:domain`)

- [x] 1.1 Add `Device` domain model (`model/Device.kt`) with `id`, `name`, `status`, and any list fields shown in the UI (e.g. `appVersion`, `linkedAt`).
- [x] 1.2 Add `PairDeviceError` type (`model/PairDeviceError.kt`) — generic error for the POC, mirroring `RegisterProductError`.
- [x] 1.3 Add `DeviceRepository` port (`repository/DeviceRepository.kt`) with `suspend fun getDevices(): Result<List<Device>>` and `suspend fun pairDevice(code: String): Result<Unit>`.
- [x] 1.4 Add `GetDevicesUseCase` (`usecase/GetDevicesUseCase.kt`) delegating to `DeviceRepository.getDevices()`.
- [x] 1.5 Add `PairDeviceUseCase` (`usecase/PairDeviceUseCase.kt`) delegating to `DeviceRepository.pairDevice(code)`.
- [x] 1.6 Add unit tests for both use cases (fakes from `core:testing`).

## 2. Network layer (`core:network`)

- [x] 2.1 Add `DeviceDto` (`dto/DeviceDtos.kt`) matching `GET /api/v1/devices` items, plus `PairDeviceRequestDto` and `PairDeviceResponseDto` for `POST /api/v1/devices/pair`, all `@Serializable` with `@SerialName`.
- [x] 2.2 Add `DeviceApiService` (`service/DeviceApiService.kt`) on the authenticated client with `getDevices(): NetworkResult<List<DeviceDto>>` and `pairDevice(body): NetworkResult<PairDeviceResponseDto>`, using `runCatching { ... }.toNetworkResult()`.
- [x] 2.3 Provide `DeviceApiService` in `NetworkModule` using `@AuthenticatedClient`.
- [x] 2.4 Add a service test for `DeviceApiService` (mirror `AuthApiServiceTest`).

## 3. Data layer (`core:data`)

- [x] 3.1 Add `DeviceMappers.kt` — `DeviceDto.toDomain()`.
- [x] 3.2 Add `DeviceRepositoryImpl` (`repository/DeviceRepositoryImpl.kt`) mapping `NetworkResult → Result`; build the `PairDeviceRequestDto` (decision `APPROVE`, platform `ANDROID`, default `deviceName`, `appVersion`) and map a successful approve to `Result.success(Unit)`.
- [x] 3.3 Bind `DeviceRepository` to `DeviceRepositoryImpl` in `DataModule` (`@Binds @Singleton`).
- [x] 3.4 Add repository unit tests (success, empty list, HTTP error, network error, pair success/failure).

## 4. Create `features/device-linking` module

- [x] 4.1 Create the module directory `features/device-linking/src/main/java/com/qhathia/mobile/staff/feature/devicelinking/`.
- [x] 4.2 Add `features/device-linking/build.gradle.kts` from the Feature module template (Compose + Hilt + KSP; deps on `core:domain`, `core:ui`, `core:common`).
- [x] 4.3 Register `include(":features:device-linking")` in `settings.gradle.kts`.
- [x] 4.4 Add `features/device-linking/src/main/AndroidManifest.xml` (`<manifest />`).

## 5. Feature presentation (MVI)

- [x] 5.1 Add `model/DeviceLinkingUiState.kt` (`devices`, `isLoading`, `errorMessage`, `isPairDialogVisible`, `pairCode`, `isPairing`, `pairErrorMessage`).
- [x] 5.2 Add `model/DeviceLinkingUiEvent.kt` (`Load`/`Retry`, `OpenPairDialog`, `DismissPairDialog`, `PairCodeChanged`, `SubmitPair`).
- [x] 5.3 Add `ui/DeviceLinkingViewModel.kt` (`@HiltViewModel`): load devices on init; on `SubmitPair` call `PairDeviceUseCase`, on success close dialog + reload list, on failure set `pairErrorMessage`.
- [x] 5.4 Add `ui/DeviceLinkingContent.kt` — bold top-left "Dispositivos vinculados" title, loading/error/list/empty states, floating + button; `@Preview` for light and dark.
- [x] 5.5 Add the near-full-screen pair dialog composable (code input + submit; submit disabled when code is blank; shows inline error).
- [x] 5.6 Add `ui/DeviceLinkingScreen.kt` — stateful composable binding the ViewModel and hoisting `onEvent`.
- [x] 5.7 Add `res/values/strings.xml` (title, empty-state message, dialog labels, error text).
- [x] 5.8 Add `navigation/DeviceLinkingNavigation.kt` — `DEVICE_LINKING_ROUTE` const + `NavGraphBuilder.deviceLinkingGraph(onBack)` extension.

## 6. Wire into the app shell (`:app`)

- [x] 6.1 Add `implementation(project(":features:device-linking"))` to `app/build.gradle.kts`.
- [x] 6.2 In `MainShell`, register `deviceLinkingGraph(...)` inside the inner NavHost (non-top-level route → bottom bar hidden automatically).
- [x] 6.3 Give the Profile placeholder an entry point (link-terminal action) that navigates to `DEVICE_LINKING_ROUTE`.

## 7. Verification

- [x] 7.1 `./gradlew :features:device-linking:test :core:domain:test :core:data:test :core:network:test` pass.
- [x] 7.2 `./gradlew assembleDebug` builds and `./gradlew lint` is clean.
- [ ] 7.3 Manual check: Profile → open Linked Devices (no bottom bar) → empty state shown when no devices → + opens dialog → invalid code shows error, valid code closes dialog and reloads list. _(Requires an emulator/device — not runnable in this environment; ViewModel logic is covered by `DeviceLinkingViewModelTest`.)_
