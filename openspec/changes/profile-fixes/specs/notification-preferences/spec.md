## MODIFIED Requirements

### Requirement: Show the seven notification toggles

The Notificaciones screen SHALL load `GET /api/v1/notifications/preferences` and show a gradient top bar with back and the title "Notificaciones", followed by three groups:
- **Pedidos**: "Nuevo pedido" (`newOrder`); "Pedido por expirar" (`orderExpiring`) with the subtitle "Aviso N minutos antes de que venza", N being `orderExpiringWarningMinutes` from the response.
- **Tablet**: "Tablet desconectada" (`tabletDisconnected`), "Producto agotado en vitrina" (`productOutOfStock`), "Batería baja" (`lowBattery`).
- **Tu negocio**: "Reporte semanal" (`weeklyReport`) with the subtitle "Cada lunes por WhatsApp"; "Sugerencias para vender más" (`sellingSuggestions`).

The response's `supported` map SHALL say which toggles the backend actually sends. A supported toggle SHALL reflect the value returned and be changeable. An unsupported toggle SHALL show its title with "Próximamente", no switch and no subtitle, and SHALL NOT be changeable; its stored value SHALL be kept, so it becomes a normal toggle as soon as the backend supports it. When `supported` is missing or empty, "Nuevo pedido" and "Pedido por expirar" SHALL count as supported and the rest as not. When `orderExpiringWarningMinutes` is missing, 10 SHALL be used. The screen SHALL be available to OWNER, SELLER and ADMIN.

#### Scenario: Preferences loaded
- **WHEN** the endpoint returns `newOrder` false, every other toggle true, and only `newOrder` and `orderExpiring` supported
- **THEN** "Nuevo pedido" is off and "Pedido por expirar" is on
- **AND** the other five rows show "Próximamente" with no switch

#### Scenario: Warning minutes from the backend
- **WHEN** the endpoint returns `orderExpiringWarningMinutes` 10
- **THEN** "Pedido por expirar" reads "Aviso 10 minutos antes de que venza"

#### Scenario: A preference becomes supported
- **WHEN** the endpoint returns `lowBattery` supported
- **THEN** "Batería baja" shows a working switch with its stored value

#### Scenario: Supported map missing
- **WHEN** the response has no `supported` map
- **THEN** only "Nuevo pedido" and "Pedido por expirar" have a switch

#### Scenario: Loading
- **WHEN** the preferences are being fetched
- **THEN** a loading indicator is shown instead of the groups

#### Scenario: Load fails
- **WHEN** the request fails
- **THEN** an error state with a retry action is shown

### Requirement: Keep the hub count consistent

After a successful save, the "N de M alertas activas" subtitle on Mi perfil SHALL reflect the new count of supported alerts that are on when the merchant returns to Mi perfil.

#### Scenario: Count updated after returning
- **WHEN** the merchant turns off one toggle successfully and goes back
- **THEN** the Notificaciones row reads one fewer active alert

### Requirement: Accessible toggles

Each supported toggle row SHALL be a single accessible node with switch semantics. Its label SHALL be the title plus subtitle, and its state on or off. An unsupported row SHALL be a single disabled node announced as "<title>, próximamente".

#### Scenario: Screen reader on a toggle
- **WHEN** a screen reader focuses "Pedido por expirar"
- **THEN** it is announced as a switch labelled "Pedido por expirar, Aviso 10 minutos antes de que venza" with its current state

#### Scenario: Screen reader on an unsupported row
- **WHEN** a screen reader focuses "Tablet desconectada" while it is unsupported
- **THEN** it is announced as "Tablet desconectada, próximamente", disabled
