## MODIFIED Requirements

### Requirement: List of linked devices

The Dispositivos screen SHALL show a gradient top bar with back and the title "Dispositivos". It SHALL retrieve the tablets paired to the owner's store from `GET /api/v1/devices` and the active catalog size from `GET /api/v1/catalog/count`, requested in parallel.

Each tablet SHALL be rendered as a card with:
- **Identity**: a tablet icon, the device `name` (falling back to "Tablet" when absent), and below it a status line: "Activo" in success colour, "Inactivo" in neutral colour, or "Aún no se conecta" in neutral colour when the tablet has never reported (`lastPingAt` absent). When the tablet reported its `model`, the status line SHALL also show it ("Activo · Lenovo Tab M11"), so two tablets can be told apart.
- **Catálogo visible**: "<activeProducts> productos" and, below it, "Actualizado <relative time>", where the time is the most recent of `lastSyncAt` and `lastPingAt`. The line is shown whether the tablet is connected or not, uses whichever of the two is present when only one is, and is omitted when both are absent.
- **Batería**: "<batteryLevel>%" with a battery glyph, or "—" when `batteryLevel` is null.
- **Conexión**: the `networkStatus` label, where WIFI → "Wi Fi", MOBILE → "Datos móviles", other values as received, and absent → "—".
- **Tienda**: the `storeName`.
- **Clientes hoy**: "—" with a *Próximamente* tag.
- **Actions**: "Actualizar catálogo" (primary) and "Reiniciar" (outlined).

A tablet SHALL be considered active when its status is ACTIVE and `lastPingAt` is within the last 10 minutes. The card SHALL NOT show a separate "Último reporte" caption under battery and connection; the "Actualizado" line in Catálogo visible replaces it.

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

#### Scenario: Loading the list

- **WHEN** the Dispositivos screen is loading the devices
- **THEN** the screen shows a loading indicator until the result arrives

#### Scenario: Loading the list fails

- **WHEN** loading the device list fails
- **THEN** the screen shows an error state that allows the owner to retry

#### Scenario: Catalog count fails

- **WHEN** the devices load but `GET /api/v1/catalog/count` fails
- **THEN** the cards are shown with "—" as the visible catalog count

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
