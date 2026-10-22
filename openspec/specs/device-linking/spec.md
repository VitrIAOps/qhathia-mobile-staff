# device-linking Specification

## Purpose

Defines how a store owner links and manages the tablets (terminals) paired to their store in the Qhathia staff app: the entry point from the Profile section, the full-screen Linked Devices screen that lists paired devices from the Devices API, the empty state, and pairing a new device via a code.
## Requirements
### Requirement: Entry point to device linking from Profile

The system SHALL provide the "Dispositivos" row in the Mi perfil menu, shown to OWNER and ADMIN only, which opens the Dispositivos screen. Opening the Dispositivos screen SHALL hide the bottom navigation bar so the screen is presented full-screen.

#### Scenario: Owner opens linked devices from Profile

- **WHEN** the owner taps the Dispositivos row in Mi perfil
- **THEN** the app navigates to the Dispositivos screen
- **AND** the bottom navigation bar is not shown on that screen

#### Scenario: Owner returns from linked devices

- **WHEN** the owner navigates back from the Dispositivos screen
- **THEN** the app returns to Mi perfil with the bottom navigation bar visible

### Requirement: List of linked devices

The Dispositivos screen SHALL show a gradient top bar with back and the title "Dispositivos". It SHALL retrieve the tablets paired to the owner's store from `GET /api/v1/devices` and the active catalog size from `GET /api/v1/catalog/count`, requested in parallel.

Each tablet SHALL be rendered as a card with:
- **Identity**: a tablet icon, the device `name` (falling back to "Tablet" when absent), and a connection status: "Conectada" in success colour, or "Desconectada" in neutral colour.
- **Catálogo visible**: "<activeProducts> productos" and, below it, "Actualizado <relative time>", where the time is the most recent of `lastSyncAt` and `lastPingAt`. The line is shown whether the tablet is connected or not, uses whichever of the two is present when only one is, and is omitted when both are absent.
- **Batería**: "<batteryLevel>%" with a battery glyph, or "—" when `batteryLevel` is null.
- **Conexión**: the `networkStatus` label, where WIFI → "Wi Fi", MOBILE → "Datos móviles", other values as received, and absent → "—".
- **Tienda**: the `storeName`.
- **Clientes hoy**: "—" with a *Próximamente* tag.
- **Actions**: "Actualizar catálogo" (primary) and "Reiniciar" (outlined).

A tablet SHALL be considered connected when its status is ACTIVE and `lastPingAt` is within the last 10 minutes. The card SHALL NOT show a separate "Último reporte" caption under battery and connection; the "Actualizado" line in Catálogo visible replaces it.

#### Scenario: Store has paired devices

- **WHEN** the Dispositivos screen loads and the store has one or more paired devices
- **THEN** one card per device is shown with its name, connection status, visible catalog, battery, connection, store and "Clientes hoy" as *Próximamente*

#### Scenario: Recently seen tablet

- **WHEN** a tablet is ACTIVE, its `lastPingAt` is 3 minutes ago and its `lastSyncAt` is 20 minutes ago
- **THEN** its card shows "Conectada" and "Actualizado hace 3 min" under the product count

#### Scenario: Sync more recent than ping

- **WHEN** a tablet's `lastSyncAt` is 5 minutes ago and its `lastPingAt` is 12 minutes ago
- **THEN** its card shows "Actualizado hace 5 min" under the product count

#### Scenario: Stale tablet

- **WHEN** a tablet's `lastPingAt` is 2 hours ago and its `lastSyncAt` is absent
- **THEN** its card shows "Desconectada" and "Actualizado hace 2 horas" under the product count, and no "Último reporte" caption

#### Scenario: Tablet that never reported

- **WHEN** a tablet's `lastPingAt` and `lastSyncAt` are both absent
- **THEN** the Catálogo visible box shows only the product count, with no "Actualizado" line

#### Scenario: Loading the list

- **WHEN** the Dispositivos screen is loading the devices
- **THEN** the screen shows a loading indicator until the result arrives

#### Scenario: Loading the list fails

- **WHEN** loading the device list fails
- **THEN** the screen shows an error state that allows the owner to retry

#### Scenario: Catalog count fails

- **WHEN** the devices load but `GET /api/v1/catalog/count` fails
- **THEN** the cards are shown with "—" as the visible catalog count

### Requirement: Empty state

When the store has no paired devices, the Dispositivos screen SHALL show a message indicating there are no tablets yet. It SHALL invite the owner to use "Vincular otro dispositivo", and that action SHALL remain available.

#### Scenario: Store has no paired devices

- **WHEN** the Dispositivos screen loads and the store has no paired devices
- **THEN** the screen shows an empty-state message inviting the owner to link a tablet
- **AND** the "Vincular otro dispositivo" action remains available

### Requirement: Pair a new device via code

The Dispositivos screen SHALL provide a "+ Vincular otro dispositivo" text action below the device cards. It SHALL open a near-full-screen dialog containing a single code input and a submit button. On submit, the system SHALL pair the device by calling `POST /api/v1/devices/pair` with `decision = APPROVE`, the entered pairing code, and the required device metadata (name, platform `ANDROID`, app version).

#### Scenario: Owner opens the pairing dialog

- **WHEN** the owner taps "Vincular otro dispositivo"
- **THEN** a near-full-screen dialog opens with a code input and a submit button

#### Scenario: Successful pairing

- **WHEN** the owner enters a valid pairing code and submits
- **AND** the pairing request succeeds
- **THEN** the dialog closes
- **AND** the device list is reloaded

#### Scenario: Failed pairing

- **WHEN** the owner submits and the pairing request fails
- **THEN** the dialog stays open and shows an error message
- **AND** the owner can correct the code and submit again

#### Scenario: Submit disabled without a code

- **WHEN** the code input is empty
- **THEN** the submit action is disabled

### Requirement: Remote commands to a tablet

"Actualizar catálogo" and "Reiniciar" SHALL send `POST /api/v1/devices/{deviceId}/commands` with `REFRESH_CATALOG` or `RESTART_APP` respectively. "Reiniciar" SHALL first ask for confirmation, explaining that it restarts the kiosk app on the tablet.

Each card SHALL show the latest known state of each command under its actions:

| Status | Shown |
|---|---|
| `PENDING`, `DELIVERED` | "Enviado · Si la tablet no responde, podrás reintentar a las <HH:MM>", or "Enviado" when the command has no `expiresAt` |
| `ACKED` | "Listo" |
| `FAILED` | "Falló: <resultDetail>", or "Falló" when there is no detail |
| `EXPIRED` | "No se pudo entregar" |

`<HH:MM>` SHALL be the command's `expiresAt` as returned by the backend, shown in the phone's local time zone and short time format. It SHALL be fixed text, not a countdown. The app SHALL NOT estimate the tablet's next heartbeat.

While a command of that type is `PENDING` or `DELIVERED`, its button SHALL be disabled. A failed request SHALL show an error message and leave the button enabled.

#### Scenario: Refresh catalog sent

- **WHEN** the owner taps "Actualizar catálogo" and the API returns `202` with status `PENDING` and `expiresAt` 10 minutes after `requestedAt`
- **THEN** the card shows "Enviado · Si la tablet no responde, podrás reintentar a las <expiresAt in local time>" and "Actualizar catálogo" is disabled

#### Scenario: In-flight command without expiry

- **WHEN** an in-flight command is returned without `expiresAt`
- **THEN** the card shows "Enviado" with no retry time

#### Scenario: Finished command hides the retry time

- **WHEN** a poll returns the command as `ACKED`, `FAILED` or `EXPIRED`
- **THEN** the retry time is no longer shown and the line shows that status's text

#### Scenario: Restart requires confirmation

- **WHEN** the owner taps "Reiniciar"
- **THEN** a confirmation dialog is shown, and the command is sent only after confirming

#### Scenario: Command request fails

- **WHEN** the command request fails with a network error
- **THEN** an error message is shown and the button stays enabled

### Requirement: Follow command outcome

While any command on the screen is `PENDING` or `DELIVERED` and the screen is visible, the app SHALL poll `GET /api/v1/devices/{deviceId}/commands` for that device every 15 seconds. It SHALL update the shown state from the most recent command of each type, and stop polling once no command is in flight or the screen is no longer visible.

On opening the screen, the app SHALL load each device's recent commands, so that an in-flight or recently finished command is shown. A finished command older than 10 minutes SHALL NOT be shown.

#### Scenario: Tablet acknowledges

- **WHEN** a `REFRESH_CATALOG` is `PENDING` and a poll returns it as `ACKED`
- **THEN** the card shows "Listo", "Actualizar catálogo" is enabled again, and polling stops

#### Scenario: Command expires

- **WHEN** a poll returns the command as `EXPIRED`
- **THEN** the card shows "No se pudo entregar" and the button is enabled to retry

#### Scenario: Leaving the screen stops polling

- **WHEN** the owner navigates back while a command is in flight
- **THEN** no further command polls are made

### Requirement: Entry to a tablet's settings

Each tablet card in Dispositivos SHALL offer an "Ajustes de la tablet" action below its command buttons. It SHALL open the Ajustes de la tablet screen for that tablet. The action SHALL be available whether or not the tablet is connected, since settings are stored and applied when it next reports.

#### Scenario: Opening a tablet's settings
- **WHEN** the owner taps "Ajustes de la tablet" on a tablet's card
- **THEN** the Ajustes de la tablet screen opens for that tablet

#### Scenario: Disconnected tablet
- **WHEN** a tablet's card shows "Desconectada"
- **THEN** "Ajustes de la tablet" is still available
