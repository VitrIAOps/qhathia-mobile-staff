# notification-preferences Specification

## Purpose
TBD - created by archiving change add-merchant-profile-settings. Update Purpose after archive.
## Requirements
### Requirement: Show the seven notification toggles

The Notificaciones screen SHALL load `GET /api/v1/notifications/preferences` and show a gradient top bar with back and the title "Notificaciones", followed by three groups:
- **Pedidos**: "Nuevo pedido" (`newOrder`); "Pedido por expirar" (`orderExpiring`) with the subtitle "Aviso cuando queden 2 min".
- **Tablet**: "Tablet desconectada" (`tabletDisconnected`), "Producto agotado en vitrina" (`productOutOfStock`), "Batería baja" (`lowBattery`).
- **Tu negocio**: "Reporte semanal" (`weeklyReport`) with the subtitle "Cada lunes por WhatsApp"; "Sugerencias para vender más" (`sellingSuggestions`).

Each toggle SHALL reflect the value returned. The screen SHALL be available to OWNER, SELLER and ADMIN.

#### Scenario: Preferences loaded
- **WHEN** the endpoint returns `lowBattery` false and every other toggle true
- **THEN** "Batería baja" is off and the other six toggles are on

#### Scenario: Loading
- **WHEN** the preferences are being fetched
- **THEN** a loading indicator is shown instead of the groups

#### Scenario: Load fails
- **WHEN** the request fails
- **THEN** an error state with a retry action is shown

### Requirement: Save each toggle individually

Changing a toggle SHALL update it on screen immediately and send `PUT /api/v1/notifications/preferences` containing only that field. On success, the screen SHALL adopt the full set returned by the response. On failure, the toggle SHALL revert to its previous value and a non-blocking error message SHALL be shown.

While a save for a toggle is in flight, that toggle SHALL NOT accept another change. Other toggles SHALL remain usable.

#### Scenario: Toggle saved
- **WHEN** the merchant turns off "Batería baja"
- **THEN** the switch shows off immediately
- **AND** the request body is `{ "lowBattery": false }`

#### Scenario: Save fails
- **WHEN** the merchant turns off "Batería baja" and the request fails
- **THEN** the switch returns to on and an error message is shown

#### Scenario: Concurrent toggles
- **WHEN** a save for "Batería baja" is in flight and the merchant turns off "Reporte semanal"
- **THEN** a separate request with only `weeklyReport` is sent

### Requirement: Keep the hub count consistent

After a successful save, the "N de M alertas activas" subtitle on Mi perfil SHALL reflect the new `activeCount` when the merchant returns to Mi perfil.

#### Scenario: Count updated after returning
- **WHEN** the merchant turns off one toggle successfully and goes back
- **THEN** the Notificaciones row reads one fewer active alert

### Requirement: Accessible toggles

Each toggle row SHALL be a single accessible node with switch semantics. Its label SHALL be the title plus subtitle, and its state on or off.

#### Scenario: Screen reader on a toggle
- **WHEN** a screen reader focuses "Pedido por expirar"
- **THEN** it is announced as a switch labelled "Pedido por expirar, Aviso cuando queden 2 min" with its current state

