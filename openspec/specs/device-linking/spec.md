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
- **Identity**: a tablet icon, the device `name` (falling back to "Tablet" when absent), and below it a status line: "Activo" in success colour, "Inactivo" in neutral colour, or "Aún no se conecta" in neutral colour when the tablet has never reported (`lastPingAt` absent). When the tablet reported its `model`, the status line SHALL also show it ("Activo · Lenovo Tab M11"), so two tablets can be told apart.
- **Catálogo visible**: "<visibleProducts> productos" — the products the tablet actually shows — and, when `activeProducts` is larger, a line "<activeProducts − visibleProducts> no se muestran (agotados, pocas unidades o a consultar)"; when the backend does not send `visibleProducts`, "<activeProducts> productos" alone. Below it, "Actualizado <relative time>", where the time is the most recent of `lastSyncAt` and `lastPingAt`. The line is shown whether the tablet is connected or not, uses whichever of the two is present when only one is, and is omitted when both are absent.
- **Batería**: "<batteryLevel>%" with a battery glyph, or "—" when `batteryLevel` is null.
- **Conexión**: a label and a matching icon from `networkStatus`, never the raw value:

  | `networkStatus` | Label | Icon |
  |---|---|---|
  | `WIFI` | "Wi-Fi" | Wi-Fi |
  | `MOBILE`, `CELLULAR`, `LTE`, `4G`, `5G` | "Datos móviles" | cell signal |
  | `ETHERNET` | "Cable" | cable |
  | `OFFLINE` | "Sin conexión", or "En línea" while the tablet is active | Wi-Fi off / globe |
  | `ONLINE` | "En línea" | globe |
  | `LIMITED` | "Limitada" | limited Wi-Fi |
  | anything else | "Otra red" | globe |
  | absent | "—" | globe |
- **Tienda**: the `storeName`.
- **Clientes hoy**: "—" with a *Próximamente* tag under the label. The tag SHALL never wrap.
- **Actions**: "Actualizar catálogo" (primary) and "Reiniciar" (outlined), side by side, or stacked at full width when the font scale is large, so neither label is cut.

A tablet SHALL be considered active when its status is ACTIVE and `lastPingAt` is within the last 10 minutes. The card SHALL NOT show a separate "Último reporte" caption under battery and connection; the "Actualizado" line in Catálogo visible replaces it.

While the screen is visible, the app SHALL silently re-read `GET /api/v1/devices` every 30 seconds, and every 15 seconds while any command is in flight or a restart is waiting for the tablet. Coming back to the screen (from Ajustes, or from the background) SHALL re-read at once, the catalog count included. When an "Actualizar catálogo" command reaches `ACKED`, the catalog count SHALL be re-read too. A silent re-read SHALL replace the cards without a loading indicator, and a failed one SHALL keep what is shown. Nothing SHALL be read while the screen is not visible.

#### Scenario: Store has paired devices

- **WHEN** the Dispositivos screen loads and the store has one or more paired devices
- **THEN** one card per device is shown with its name, status, visible catalog, battery, connection, store and "Clientes hoy" as *Próximamente*

#### Scenario: Recently seen tablet

- **WHEN** a tablet is ACTIVE, its `lastPingAt` is 3 minutes ago and its `lastSyncAt` is 20 minutes ago
- **THEN** its card shows "Activo" and "Actualizado hace 3 min" under the product count

#### Scenario: Sync more recent than ping

- **WHEN** a tablet's `lastSyncAt` is 5 minutes ago and its `lastPingAt` is 12 minutes ago
- **THEN** its card shows "Actualizado hace 5 min" under the product count

#### Scenario: Stale tablet

- **WHEN** a tablet's `lastPingAt` is 2 hours ago and its `lastSyncAt` is absent
- **THEN** its card shows "Inactivo" and "Actualizado hace 2 horas" under the product count, and no "Último reporte" caption

#### Scenario: Tablet that never reported

- **WHEN** a tablet's `lastPingAt` and `lastSyncAt` are both absent
- **THEN** its status reads "Aún no se conecta" and the Catálogo visible box shows only the product count, with no "Actualizado" line

#### Scenario: Model next to the status

- **WHEN** an active tablet reported the model "Lenovo Tab M11"
- **THEN** its status line reads "Activo · Lenovo Tab M11"

#### Scenario: Wired tablet

- **WHEN** a tablet reports `networkStatus` `ETHERNET`
- **THEN** its Conexión tile reads "Cable" with a cable icon

#### Scenario: The card follows the tablet's reports

- **WHEN** the screen stays open and the tablet reports again
- **THEN** within 30 seconds the card shows the new report, without leaving the screen

#### Scenario: A tablet unlinked elsewhere

- **WHEN** a tablet is unlinked from another phone while Dispositivos is open
- **THEN** its card disappears within 30 seconds

#### Scenario: Loading the list

- **WHEN** the Dispositivos screen is loading the devices
- **THEN** the screen shows a loading indicator until the result arrives

#### Scenario: Loading the list fails

- **WHEN** loading the device list fails
- **THEN** the screen shows an error state that allows the owner to retry

#### Scenario: Catalog count fails

- **WHEN** the devices load but `GET /api/v1/catalog/count` fails
- **THEN** the cards are shown with "—" as the visible catalog count

#### Scenario: Products the tablet does not show
- **WHEN** `GET /api/v1/catalog/count` answers `activeProducts` 50 and `visibleProducts` 46
- **THEN** the Catálogo visible box reads "46 productos" and "4 no se muestran (agotados, pocas unidades o a consultar)"

#### Scenario: Every product is shown
- **WHEN** `activeProducts` and `visibleProducts` are both 46
- **THEN** the box reads "46 productos" and no "no se muestran" line

#### Scenario: Older backend
- **WHEN** the answer has `activeProducts` 50 and no `visibleProducts`
- **THEN** the box reads "50 productos" and no "no se muestran" line

#### Scenario: The count follows a new product
- **WHEN** the merchant creates a product and comes back to Dispositivos
- **THEN** the catalog count is re-read and the box shows the new number

#### Scenario: The count follows a catalog refresh
- **WHEN** an "Actualizar catálogo" command turns `ACKED` during a re-read
- **THEN** the catalog count is re-read

### Requirement: Empty state

When the store has no paired devices, the Dispositivos screen SHALL show a message indicating there are no tablets yet. It SHALL invite the owner to use "Vincular otro dispositivo", and that action SHALL remain available.

#### Scenario: Store has no paired devices

- **WHEN** the Dispositivos screen loads and the store has no paired devices
- **THEN** the screen shows an empty-state message inviting the owner to link a tablet
- **AND** the "Vincular otro dispositivo" action remains available

### Requirement: Pair a new device via code

The Dispositivos screen SHALL provide a "+ Vincular otro dispositivo" text action below the device cards. It SHALL open a near-full-screen dialog with:
- the pairing code field;
- a "Nombre de la tablet" field, pre-filled with the first "Tablet N" (N = 1, 2, …) not used by a tablet of the store, editable, at most 40 characters;
- "Vincular" and "Rechazar".

**Code field.** It SHALL uppercase what is typed, drop any character outside the pairing alphabet `23456789ABCDEFGHJKMNPQRSTUVWXYZ` (so O, 0, I, 1 and L never appear), and stop at 5 characters. A helper SHALL read "5 caracteres, sin O, 0, I, L ni 1 · <n>/5". "Vincular" and "Rechazar" SHALL be enabled only with exactly 5 characters and no request in flight, and the keyboard's Done action SHALL submit.

**Approving.** "Vincular" SHALL call `POST /api/v1/devices/pair` with `decision = APPROVE`, the code, and the typed name as `deviceName` (omitted when blank). It SHALL NOT send an app version or a placeholder name. The `200` only means approved. The dialog SHALL then show "Esperando a que la tablet confirme…" and poll `GET /api/v1/devices/pairing-session/{sessionId}/status` every 2 seconds, for up to 30 seconds. While waiting it SHALL NOT be dismissable.
- When the status reports `tokenExchanged`, the dialog SHALL close, "Tablet «<nombre>» vinculada" SHALL be shown, and the device list SHALL be read again, and once more 3 seconds later so the new card shows the first heartbeat.
- When 30 seconds pass without the exchange, the dialog SHALL say "La tablet no recibió la aprobación. Revisa que siga mostrando el código y que tenga internet." and offer:
  - "Deshacer vinculación", which unlinks the approved device (`POST /api/v1/devices/{deviceId}/unlink`). Success or `404` closes the dialog, shows "Vinculación deshecha. Pide un código nuevo en la tablet." and reads the list again; a network failure keeps the dialog with "Sin conexión. Inténtalo nuevamente.".
  - "Seguir esperando", which polls for another 30 seconds.

**Rejecting.** "Rechazar" SHALL call `POST /api/v1/devices/pair` with `decision = REJECT` and the code. On success the dialog SHALL close and "Rechazaste la vinculación. La tablet mostrará un aviso." SHALL be shown.

**Errors.** A failed approve or reject SHALL keep the dialog open with the code, and show a message chosen by the backend's `errorCode`, not by the HTTP status:

| `errorCode` / status | Message |
|---|---|
| `INVALID_PAIRING_TOKEN` (or another `400`) | "Ese código no existe. Revisa que sea el que muestra la tablet." |
| `PAIRING_TOKEN_EXPIRED` | "El código venció. En la tablet toca «Generar nuevo código»." |
| `PAIRING_TOKEN_ALREADY_USED` | "Ese código ya se usó. Pide uno nuevo en la tablet." |
| `PAIRING_TOKEN_CANCELLED` | "La tablet descartó ese código. Ingresa el que muestra ahora." |
| `CATALOG_THRESHOLD_NOT_MET` / `409` | "Tu catálogo aún no tiene suficientes productos con nombre, precio y foto para vincular una tablet." |
| `503` | "No pudimos verificar tu catálogo. Inténtalo en unos minutos." |
| no connection | "Sin conexión. Inténtalo nuevamente." |

#### Scenario: Owner opens the pairing dialog

- **WHEN** the owner taps "Vincular otro dispositivo" and the store has "Tablet 1" and "Tablet 3"
- **THEN** a near-full-screen dialog opens with an empty code field, the name field pre-filled with "Tablet 2", and "Vincular" and "Rechazar" disabled

#### Scenario: Typing the code

- **WHEN** the owner types "rwn0za"
- **THEN** the field shows "RWNZA"

#### Scenario: Ambiguous characters

- **WHEN** the owner types "o0i1l"
- **THEN** the field stays empty

#### Scenario: Submit disabled without five characters

- **WHEN** the code field holds 4 characters
- **THEN** "Vincular" and "Rechazar" are disabled and no request is sent

#### Scenario: The typed name is sent

- **WHEN** the owner types the name "Mostrador" and approves a valid code
- **THEN** the pairing request carries `deviceName: "Mostrador"` and no `appVersion`

#### Scenario: Successful pairing

- **WHEN** the owner approves a valid code and the tablet exchanges it within 30 seconds
- **THEN** the dialog closes, "Tablet «Mostrador» vinculada" is shown, and the device list is read again

#### Scenario: The tablet does not respond

- **WHEN** the owner approves a valid code and 30 seconds pass without the exchange
- **THEN** the dialog says "La tablet no recibió la aprobación…" and offers "Deshacer vinculación" and "Seguir esperando"

#### Scenario: Undoing the pairing

- **WHEN** the owner taps "Deshacer vinculación"
- **THEN** the approved device is unlinked, the dialog closes, "Vinculación deshecha…" is shown and no new card stays in the list

#### Scenario: Keep waiting

- **WHEN** the owner taps "Seguir esperando" and the tablet then exchanges the code
- **THEN** the dialog closes and "Tablet «…» vinculada" is shown

#### Scenario: Rejecting a code

- **WHEN** the owner types a valid code and taps "Rechazar"
- **THEN** the request carries `decision: REJECT`, the dialog closes and "Rechazaste la vinculación…" is shown

#### Scenario: Expired code

- **WHEN** the approve answers `400` with `errorCode` `PAIRING_TOKEN_EXPIRED`
- **THEN** the dialog stays open and says "El código venció. En la tablet toca «Generar nuevo código»."

#### Scenario: Failed pairing

- **WHEN** the owner submits and the pairing request fails
- **THEN** the dialog stays open and shows the message for that error
- **AND** the owner can correct the code and submit again

### Requirement: Remote commands to a tablet

"Actualizar catálogo" and "Reiniciar" SHALL send `POST /api/v1/devices/{deviceId}/commands` with `REFRESH_CATALOG` or `RESTART_APP` respectively. "Reiniciar" SHALL first ask for confirmation, explaining that it restarts the kiosk app on the tablet.

Each card SHALL show the latest known state of each command under its actions. For "Actualizar catálogo":

| Status | Shown |
|---|---|
| `PENDING`, `DELIVERED` | "Enviado · Si la tablet no responde, podrás reintentar a las <hora>", the time following the phone's 12/24-hour setting ("5:04 p. m." or "17:04"), or "Enviado" when the command has no `expiresAt` |
| `ACKED` | "Listo" |
| `FAILED` | "Falló: <resultDetail>", or "Falló" when there is no detail |
| `EXPIRED` | "No se pudo entregar: la tablet no se conectó en 10 min. Revisa que esté encendida y con internet." |

For "Reiniciar", an acknowledgement does not prove the tablet came back, so the line SHALL follow the tablet's reports:

| Restart state | Shown |
|---|---|
| `PENDING` | the same "Enviado…" text |
| `DELIVERED`, or `ACKED` with no heartbeat (`lastPingAt`) after `ackedAt` | "Reiniciando… esperando a que la tablet vuelva a reportar" |
| `ACKED` with a heartbeat after `ackedAt` | "Listo", in success colour |
| `ACKED`, no heartbeat 3 minutes after `ackedAt` | "La tablet no volvió a reportar tras el reinicio. Revísala.", in error colour |
| `FAILED`, `EXPIRED` | as for the catalog |

`<HH:MM>` SHALL be the command's `expiresAt` as returned by the backend, shown in the phone's local time zone and short time format. It SHALL be fixed text, not a countdown. The app SHALL NOT estimate the tablet's next heartbeat.

A command still `PENDING` or `DELIVERED` 30 seconds after its `expiresAt` SHALL be shown and treated as expired, even before the server reports it.

While a command of that type is in flight, its button SHALL be disabled; "Reiniciar" SHALL also stay disabled while the restart is waiting for the tablet. It SHALL be enabled again once the restart is "Listo" or "no volvió a reportar". A failed request SHALL show an error message and leave the button enabled.

#### Scenario: Refresh catalog sent

- **WHEN** the owner taps "Actualizar catálogo" and the API returns `202` with status `PENDING` and `expiresAt` 10 minutes after `requestedAt`
- **THEN** the card shows "Enviado · Si la tablet no responde, podrás reintentar a las <expiresAt in local time>" and "Actualizar catálogo" is disabled

#### Scenario: In-flight command without expiry

- **WHEN** an in-flight command is returned without `expiresAt`
- **THEN** the card shows "Enviado" with no retry time

#### Scenario: Finished command hides the retry time

- **WHEN** a poll returns the command as `ACKED`, `FAILED` or `EXPIRED`
- **THEN** the retry time is no longer shown and the line shows that status's text

#### Scenario: Restart acknowledged, tablet not back yet

- **WHEN** a restart is `ACKED` and the tablet has not reported since `ackedAt`
- **THEN** the line reads "Reiniciando… esperando a que la tablet vuelva a reportar" and "Reiniciar" is disabled

#### Scenario: Tablet back after the restart

- **WHEN** a restart is `ACKED` and the tablet's `lastPingAt` is after `ackedAt`
- **THEN** the line reads "Listo" and "Reiniciar" is enabled

#### Scenario: Tablet not back after the restart

- **WHEN** a restart is `ACKED` and 3 minutes pass with no report after `ackedAt`
- **THEN** the line reads "La tablet no volvió a reportar tras el reinicio. Revísala." in error colour and "Reiniciar" is enabled

#### Scenario: Restart requires confirmation

- **WHEN** the owner taps "Reiniciar"
- **THEN** a confirmation dialog is shown, and the command is sent only after confirming

#### Scenario: Command request fails

- **WHEN** the command request fails with a network error
- **THEN** an error message is shown and the button stays enabled

#### Scenario: Retry time on a 12-hour phone
- **WHEN** a command expires at 17:04 local time, the app is in Spanish and the phone uses the 12-hour format
- **THEN** the card reads "… podrás reintentar a las 5:04 p. m."

### Requirement: Follow command outcome

While the screen is visible and a tablet has a command in flight or a restart waiting for its report, the app SHALL re-read that tablet's commands with `GET /api/v1/devices/{deviceId}/commands` every 15 seconds, together with the device list. Each read SHALL replace that tablet's shown commands with the most recent command of each type. It SHALL stop once nothing is in flight or waiting, or the screen is no longer visible.

On opening the screen, the app SHALL load each device's recent commands, so that an in-flight or recently finished command is shown. A finished command SHALL be shown for 10 minutes counted from when it finished (`ackedAt` for `ACKED` and `FAILED`, `expiresAt` for `EXPIRED`), and hidden after that.

#### Scenario: Tablet acknowledges

- **WHEN** a `REFRESH_CATALOG` is `PENDING` and a poll returns it as `ACKED`
- **THEN** the card shows "Listo", "Actualizar catálogo" is enabled again, and polling of that command stops

#### Scenario: Command expires

- **WHEN** a command is `PENDING` and a poll after its deadline returns it as `EXPIRED`
- **THEN** within 15 seconds the card shows "No se pudo entregar…" and the button is enabled to retry

#### Scenario: Expired command after reopening

- **WHEN** the owner reopens Dispositivos 5 minutes after a command expired
- **THEN** the card still shows "No se pudo entregar…"

#### Scenario: Old finished command

- **WHEN** a command finished more than 10 minutes ago
- **THEN** its line is not shown

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

