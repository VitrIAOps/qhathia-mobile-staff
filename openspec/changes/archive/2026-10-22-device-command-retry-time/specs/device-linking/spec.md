## MODIFIED Requirements

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
