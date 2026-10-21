## 1. Network, domain and data

- [x] 1.1 `core:network`:
  - `DeviceSettingsDto`, and `UpdateDeviceSettingsRequestDto` with every field nullable and not encoded when null
  - `DeviceApiService.getSettings(deviceId)` / `updateSettings(deviceId, body)`
  - `DeviceDto.model`
- [x] 1.2 `core:domain`:
  - `DeviceSettings`, `BrightnessMode` (with `UNKNOWN`), the `DeviceSettingsChange` sealed type, `DeviceSettingsError`, `DeviceSettingsException` and `Device.model` (design Decision 2)
  - `DeviceRepository.getSettings` / `updateSettings`
- [x] 1.3 `core:domain`:
  - `GetDeviceSettingsUseCase`
  - `UpdateDeviceSettingsUseCase`, which rejects a PIN that is not 4–6 digits without sending it
- [x] 1.4 `core:data`:
  - Mappers from DTO to domain, and from each change to a body with exactly one field
  - Repository implementation (404 → NotFound, 400/422 → InvalidInput, 503/transport → Network)
- [x] 1.5 `core:testing`: `FakeDeviceRepository` settings support, and fixtures
- [x] 1.6 Tests:
  - mappers: unknown brightness → `UNKNOWN`; each change → its single-field body
  - repository error mapping
  - PIN validation in the use case

## 2. Screen

- [x] 2.1 `TabletSettingsUiState` / `UiEvent` / `UiEffect`:
  - the load states, including not found
  - settings and device
  - `saving: Set<SettingKey>`
  - PIN dialog state: open, the two fields, saving, error, and the remove confirmation
- [x] 2.2 `TabletSettingsViewModel`:
  - Parallel load and retry
  - Optimistic change with per-control rollback and the error effect (design Decision 3)
  - PIN set and remove (design Decision 4)
- [x] 2.3 `TabletSettingsContent` / `Screen`:
  - The groups, built with the design-system settings components
  - The brightness segmented control
  - The kiosk-without-PIN warning
  - Información, the footnote, and the loading, error and not-found states
- [x] 2.4 `ExitPinDialog`: numeric masked fields, Guardar enabled only with matching 4–6 digits, "Quitar PIN" with its confirmation
- [x] 2.5 Navigation:
  - The `device_settings/{deviceId}` route in `features:device-linking`
  - `MainShell` registers it and hides the bottom bar, like Dispositivos
- [x] 2.6 `DeviceCard`: the "Ajustes de la tablet" row under the command buttons, wired through `DeviceLinkingContent`
- [x] 2.7 Strings (es/en) and previews: loaded, without PIN, saving, error, not found, PIN dialog; light and dark
- [x] 2.8 ViewModel tests:
  - load: success, settings failure, 404, and device-list failure (Información hidden)
  - a change sends only its field, and is applied with the server's answer
  - a failure rolls back that control only
  - a control is disabled while saving; two controls save independently
  - PIN: match and length gating, set, remove after confirming, failure keeps the dialog

## 3. Verification

- [x] 3.1 `./gradlew :core:domain:test :core:data:testDebugUnitTest :features:device-linking:testDebugUnitTest :features:device-linking:lintDebug`
- [x] 3.2 Full build: `./gradlew testDebugUnitTest lintDebug assembleDebug`

## QA (manual, run by QA)

- Dispositivos → "Ajustes de la tablet" opens the screen with the tablet's current values and its Información
- Toggle each switch and change the brightness. Within a couple of minutes the tablet applies it (kiosk mode, voice assistant, idle showcase, brightness)
- Set a PIN, then leave kiosk mode on the tablet with that PIN. Remove the PIN → the tablet can exit without one, and the warning shows on the phone
- Airplane mode: a toggle goes back and the error shows. The PIN dialog keeps the digits
- Light and dark, large font
