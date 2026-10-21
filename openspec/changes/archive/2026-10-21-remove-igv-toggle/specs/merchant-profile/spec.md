## MODIFIED Requirements

### Requirement: Menu rows show live subtitles

Each menu row SHALL show an icon, a title, a subtitle and a trailing chevron:
- **Datos personales**: "Tu cuenta y los datos de tu tienda".
- **Notificaciones**: "N de M alertas activas", from `activeCount` and `totalCount` of the notification preferences.
- **Seguridad**: "Contraseña, huella y sesiones".
- **Configuración**: "Idioma de la app". Prices are not configured in the app, so the subtitle does not mention them.
- **Dispositivos**: the number of tablets currently connected, plural-aware.
- **Ayuda**: "Soporte por WhatsApp y guías".

While a live subtitle is loading or has failed, the row SHALL show its static fallback subtitle rather than a number.

#### Scenario: Notification count subtitle
- **WHEN** the preferences report `activeCount` 6 and `totalCount` 7
- **THEN** the Notificaciones row subtitle reads "6 de 7 alertas activas"

#### Scenario: Connected tablets subtitle
- **WHEN** the store has one tablet considered connected
- **THEN** the Dispositivos row subtitle reads "1 tablet conectada"

#### Scenario: Subtitle source fails
- **WHEN** loading the notification preferences fails
- **THEN** the Notificaciones row subtitle reads "Alertas de pedidos y tablet"

#### Scenario: Configuración subtitle does not mention prices
- **WHEN** the merchant opens Mi perfil
- **THEN** the Configuración row subtitle reads "Idioma de la app"
