## Context

- **Dispositivos** (`features:device-linking`) lists the store's tablets. Each `DeviceCard` shows connection and telemetry, with two command buttons. Mi perfil only offers the screen to OWNER/ADMIN.
- **Backend (§21):**
  - `GET /devices/{id}/settings` returns the six settings plus `exitPinSet` and `settingsVersion`. On a tablet linked before Fase 21, the first read creates the defaults.
  - `PUT` is **partial**: absent fields are untouched. It returns the full set with the change applied.
  - `exitPin` takes 4–6 digits and is write-only. Removing it needs `removeExitPin: true`.
  - `422` means a bad PIN or a bad `brightnessMode`. `404` means the tablet is not in this store.
- **Figma:** `Qhatia-DS` has no node for this screen. The groups and labels come from the mockup described in the API contract (§21 table). The layout reuses the design system's settings components (`QhathiaSettingsSection`, `QhathiaSwitchRow`, `QhathiaNavigationRow`, `QhathiaSegmentedControl`, `QhathiaGradientTopBar`), as Notificaciones and Seguridad do.

## Goals / Non-Goals

**Goals:**
- Read and change one tablet's six settings from the phone, with the same behaviour as Notificaciones.
- Set, change and remove the exit PIN without ever displaying it.
- Show the tablet's identity (Información).

**Non-Goals:**
- Renaming the tablet, the `language` setting, and per-tablet stats.

## Decisions

### 1. Where it lives

- The screen joins `features:device-linking`, the Dispositivos feature, under a new route `device_settings/{deviceId}`.
  - Only the id crosses navigation. This follows the Product Detail pattern.
  - The screen reloads what it needs on its own.
- **Alternative rejected:** a new module. The screen is a sub-screen of Dispositivos and shares its domain (`Device`, `DeviceRepository`) and strings.

### 2. Domain

- **Settings model:** `DeviceSettings(idleShowcaseEnabled, voiceAssistantEnabled, brightnessMode, kioskMode, exitPinSet, autoSyncEnabled)`.
  - `BrightnessMode` is `AUTO | LOW | MEDIUM | HIGH`, plus `UNKNOWN` so an unexpected server value does not break parsing.
- **Change model:** `DeviceSettingsChange` is a sealed type, one per write, matching the partial `PUT`:
  - `IdleShowcase(Boolean)`, `VoiceAssistant(Boolean)`, `Brightness(BrightnessMode)`, `KioskMode(Boolean)`, `AutoSync(Boolean)`
  - `SetExitPin(String)`, `RemoveExitPin`
  - The data layer maps each one to a body with exactly one field, so two quick toggles never overwrite each other.
- **Repository:** `getSettings(deviceId): Result<DeviceSettings>` and `updateSettings(deviceId, change): Result<DeviceSettings>`.
  - Failures carry `DeviceSettingsException(DeviceSettingsError.NotFound | InvalidInput | Network | Unknown)`.
- `UpdateDeviceSettingsUseCase` validates the PIN (4–6 digits) before sending, so a bad PIN never costs a request.
- `Device.model` is added, mapped from `DeviceDto.model`. It is optional, because a tablet reports it in its heartbeat.

### 3. Immediate saves, as in Notificaciones

- A switch or brightness change updates the screen at once, then sends the `PUT`.
  - **On success:** the server's full answer replaces the settings, so `exitPinSet` and the other values stay true to the server.
  - **On failure:** only that control goes back to its previous value, and a snackbar reads "No se pudo guardar el ajuste".
- A control is disabled while its own request is in flight, so one control never has two writes racing. The other controls stay usable.
- **Alternative rejected:** a form with Guardar. Every other settings screen in Mi perfil saves on toggle, and these are independent switches.

### 4. Exit PIN

- **The row** reads "PIN de salida", with the subtitle "Configurado" or "Sin PIN". Tapping it opens `ExitPinDialog`.
- **The dialog:**
  - Two numeric fields: "Nuevo PIN" and "Repite el PIN". They are masked, and the keyboard is numeric.
  - "Guardar" is enabled when both fields hold the same 4–6 digits.
  - When a PIN is set, a destructive "Quitar PIN" button is shown too.
  - On success the dialog closes, and the snackbar reads "PIN actualizado" or "PIN quitado".
  - On failure the dialog stays open, the digits are kept, and an error is shown.
- **Removing the PIN** asks first: "¿Quitar el PIN? Cualquiera podrá salir del modo kiosco en esta tablet."
- **Hint:** with kiosk mode on and no PIN, the kiosk row's subtitle warns "Sin PIN, cualquiera puede salir del modo kiosco".

### 5. Screen states and Información

- **Loading:** the settings and the tablet itself load in parallel. The tablet comes from `GET /devices`, found by id.
  - A settings failure shows the error state with a retry.
  - A `404` (the tablet was unlinked) shows "Esta tablet ya no está vinculada a tu tienda" and nothing else.
- **Información rows:**
  - Nombre, Modelo, N.º de serie, Vinculada desde (date), Versión de la app, Tienda
  - A missing value shows "—"
  - If the device list fails, the section is hidden. The settings stay usable.
- **Footnote:** "Los cambios llegan a la tablet en unos minutos", because the tablet picks them up on its next heartbeat.

### 6. Entry point

`DeviceCard` gets a `QhathiaNavigationRow`-style row, "Ajustes de la tablet", under the command buttons. It is always enabled, even with the tablet offline: the settings are stored and applied when it reconnects.

## Risks / Trade-offs

- **[Risk] A change made while the tablet is offline seems to do nothing.** → The footnote says changes arrive within minutes. The card already shows the connection state.
- **[Risk] Removing the PIN leaves the kiosk open.** → There is an explicit confirmation, plus the hint on the kiosk row.
- **[Trade-off] No Figma node.** → The screen is built from design-system settings components, so a later design pass only changes the layout.

## Open Questions

_None._
