## Why

After tapping "Actualizar catálogo" or "Reiniciar", the button stays disabled until the tablet confirms or the command expires (10 minutes server-side). The card only says "Enviado", so the owner doesn't know how long to wait and thinks the app is stuck, especially today, when the kiosk doesn't send heartbeats yet and every command waits the full 10 minutes.

Separately, the "Último reporte hace 16 min" caption sits between the tiles and the buttons and only appears when the tablet is disconnected. The owner reads the card top-down, so how fresh the tablet's data is belongs next to the product count.

## What Changes

- The status line of an in-flight command (`PENDING` / `DELIVERED`) shows when it can be retried: "Catálogo: Enviado · Si la tablet no responde, podrás reintentar a las 15:42".
- The time is the command's `expiresAt`, which the backend already returns. It is a fixed text: no per-second countdown.
- The app does not estimate when the next heartbeat arrives. The contract doesn't define a heartbeat interval, and a heartbeat doesn't re-enable the button by itself.
- `DeviceCommand` (domain) gains `expiresAt`, mapped from the existing DTO field.
- If `expiresAt` is missing, the line stays "Enviado" as today.
- The "Último reporte hace X" caption moves into the Catálogo visible box, under the product count, and reads "Actualizado hace X".
- The time is the most recent of `lastSyncAt` (last catalog download) and `lastPingAt` (last report). If only one exists, that one is used.
- That line is shown whether the tablet is connected or not, and is omitted only when both are absent.
- It replaces the current "Actualizado <lastSyncAt>" line, so the card shows a single "Actualizado" line.

## Capabilities

### New Capabilities

_None._

### Modified Capabilities

- `device-linking`: the "Remote commands to a tablet" requirement adds the retry time to the in-flight status; the "List of linked devices" requirement moves the last-report time under the product count as "Actualizado".

## Impact

- `:core:domain`: `DeviceCommand` gains `expiresAt: Instant?`.
- `:core:data`: `DeviceCommandDto.toDomain()` maps `expiresAt`.
- `:core:testing`: `TestFixtures.deviceCommand` accepts `expiresAt`.
- `:features:device-linking`: `CommandStatusLine` and `CatalogBox` in `DeviceCard.kt`, removal of the "Último reporte" caption, string changes (es and en), a time formatter.
- API: no changes. `expiresAt` is already in the response of `POST` / `GET /api/v1/devices/{deviceId}/commands`.
