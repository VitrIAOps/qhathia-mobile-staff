## Why

The owner can see each tablet in Dispositivos and send it two commands, but cannot change how the kiosk behaves: whether it locks into kiosk mode, the PIN that lets staff leave it, the brightness, the voice assistant, the idle showcase or catalog auto-sync. Today that means walking up to the tablet. The backend has supported all six settings since Fase 21 (`GET/PUT /api/v1/devices/{deviceId}/settings`).

Until now the settings could not take effect, because the tablet never sent the heartbeat that carries `settingsVersion`. Discovery implements the heartbeat as of 2026-10-21, so a change made here now reaches the tablet within a couple of minutes. The 2026-10-18 triage listed "PIN del kiosko" as an incomplete screen.

## What Changes

- Each tablet card in Dispositivos gains an **Ajustes de la tablet** action. It opens a new screen for that tablet, with four groups:
  - **Pantalla del cliente:**
    - "Modo Vitrina en reposo" (switch)
    - "Asistente por voz" (switch)
    - "Brillo": Automático / Bajo / Medio / Alto
  - **Seguridad:**
    - "Modo kiosco" (switch)
    - "PIN de salida": shows whether a PIN is set. It opens a dialog to set or change it (4–6 digits, typed twice), and to remove it when one is set
  - **Catálogo:** "Sincronizar automáticamente" (switch)
  - **Información** (read-only): name, model, serial number, linked since, app version and store
- Switches and brightness save immediately, like Notificaciones. The control changes at once and goes back to its previous value with a message if saving fails. A note says changes reach the tablet within a few minutes.
- **The PIN is never shown.** The backend only says whether one is set. Removing it asks for confirmation, because a tablet in kiosk mode without a PIN can be exited by anyone.
- The screen is only reachable from Dispositivos, which is already OWNER/ADMIN only. That matches the endpoint's roles.

## Capabilities

### New Capabilities

- `tablet-settings`: the Ajustes de la tablet screen. It reads and changes one tablet's remote settings and exit PIN, and shows its information.

### Modified Capabilities

- `device-linking`: each tablet card offers the entry to Ajustes de la tablet.

## Impact

- **`core:network`:**
  - `DeviceApiService.getSettings` / `updateSettings`
  - `DeviceSettingsDto` / `UpdateDeviceSettingsRequestDto`
  - `DeviceDto.model`
- **`core:domain`:**
  - `DeviceSettings` and `BrightnessMode`, and `Device.model`
  - `DeviceRepository.getSettings` / `updateSettings(deviceId, DeviceSettingsChange)`
  - New `GetDeviceSettingsUseCase` and `UpdateDeviceSettingsUseCase`
- **`core:data`:** mappers and the repository implementation. `422` maps to invalid input.
- **`core:testing`:** fake and fixtures.
- **`features:device-linking`:**
  - The new destination `device_settings/{deviceId}`: screen, ViewModel, state and PIN dialog
  - The card action and navigation
  - Strings (es/en), previews and tests
- **`app`:** `MainShell` registers the destination. Like Dispositivos, it has no bottom bar.
- **API:** `GET/PUT /api/v1/devices/{deviceId}/settings`, and `GET /api/v1/devices` for the Información section. Both are in production.
- **Not in scope:**
  - Renaming the tablet (`PATCH /devices/{id}`)
  - Per-tablet "Clientes hoy"
  - The `language` setting
