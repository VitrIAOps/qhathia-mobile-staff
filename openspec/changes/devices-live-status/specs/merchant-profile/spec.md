## MODIFIED Requirements

### Requirement: Menu rows show live subtitles

Each menu row SHALL show an icon, a title, a subtitle and a trailing chevron:
- **Datos personales**: "Nombre, correo y teléfono" — what the screen holds; it shows no store data.
- **Notificaciones**: "N de M alertas activas", from `activeCount` and `totalCount` of the notification preferences.
- **Seguridad**: "Contraseña, huella y sesiones".
- **Configuración**: "Idioma de la app". Prices are not configured in the app, so the subtitle does not mention them.
- **Dispositivos**: the linked and active tablets, plural-aware and in the feminine ("tablet" is feminine in Spanish): "N vinculada(s) (M activa(s))".
- **Ayuda**: "Soporte por WhatsApp y guías".

While a live subtitle is loading or has failed, the row SHALL show its static fallback subtitle rather than a number.

#### Scenario: Notification count subtitle
- **WHEN** the preferences report `activeCount` 6 and `totalCount` 7
- **THEN** the Notificaciones row subtitle reads "6 de 7 alertas activas"

#### Scenario: Linked tablets subtitle
- **WHEN** the store has three linked tablets and one is active
- **THEN** the Dispositivos row subtitle reads "3 vinculadas (1 activa)"

#### Scenario: Single tablet subtitle
- **WHEN** the store has one linked tablet and it is active
- **THEN** the Dispositivos row subtitle reads "1 vinculada (1 activa)"

#### Scenario: Subtitle source fails
- **WHEN** loading the notification preferences fails
- **THEN** the Notificaciones row subtitle reads "Alertas de pedidos y tablet"

#### Scenario: Configuración subtitle does not mention prices
- **WHEN** the merchant opens Mi perfil
- **THEN** the Configuración row subtitle reads "Idioma de la app"

#### Scenario: Datos personales subtitle
- **WHEN** the merchant opens Mi perfil
- **THEN** the Datos personales row subtitle reads "Nombre, correo y teléfono"
