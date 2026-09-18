## ADDED Requirements

### Requirement: Mi perfil hub behind the Perfil tab

Selecting the Perfil tab of the bottom navigation SHALL show the "Mi perfil" screen, which replaces the previous placeholder. The screen SHALL show, top to bottom:
1. A gradient header with the title "Mi perfil" and a notifications bell.
2. An identity card with an initials avatar, full name, email, and a role · store-count line. The card has an edit action.
3. A subscription card.
4. A menu card with the rows Datos personales, Notificaciones, Seguridad, Configuración, Dispositivos and Ayuda.
5. A "Cerrar sesión" action.

The bottom navigation bar SHALL remain visible on Mi perfil.

#### Scenario: Merchant opens Perfil
- **WHEN** the merchant taps the Perfil tab
- **THEN** the Mi perfil screen is shown with the header, identity card, subscription card, menu and "Cerrar sesión"
- **AND** the bottom navigation bar is visible with Perfil selected

#### Scenario: Identity card content
- **WHEN** `GET /api/v1/users/me` returns `fullName` "Carla Mendoza", `email` "carla.mendoza@gmail.com", `role` OWNER and three stores
- **THEN** the card shows the avatar "CM", the name, the email and the line "Dueño(a) · 3 tiendas"

#### Scenario: Single store
- **WHEN** the user has exactly one store
- **THEN** the role line reads "<role> · 1 tienda"

#### Scenario: Profile fails to load
- **WHEN** loading the profile fails
- **THEN** the identity card shows the name and email already held in the session
- **AND** an inline retry action is offered

### Requirement: Menu rows show live subtitles

Each menu row SHALL show an icon, a title, a subtitle and a trailing chevron:
- **Datos personales**: "Tu cuenta y los datos de tu tienda".
- **Notificaciones**: "N de M alertas activas", from `activeCount` and `totalCount` of the notification preferences.
- **Seguridad**: "Contraseña, huella y sesiones".
- **Configuración**: "Idioma y precios".
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

### Requirement: Navigation from the hub

Tapping a menu row, the identity card's edit action, or the subscription card SHALL push the corresponding screen over Mi perfil:
- Menu rows open Datos personales, Notificaciones, Seguridad, Configuración, Dispositivos or Ayuda.
- The edit action opens Datos personales.
- The subscription card opens Mi suscripción.

Pushed screens SHALL hide the bottom navigation bar. Going back from any of them, by top-bar chevron or system back, SHALL return to Mi perfil. The bell SHALL switch to the Solicitudes tab, exactly as the Inicio bell does.

#### Scenario: Open a sub-screen
- **WHEN** the merchant taps the Seguridad row
- **THEN** the Seguridad screen is shown without the bottom navigation bar

#### Scenario: Back returns to Mi perfil
- **WHEN** the merchant is on Datos personales, Notificaciones, Seguridad, Configuración, Dispositivos, Ayuda or Mi suscripción and goes back
- **THEN** Mi perfil is shown with the bottom navigation bar visible

#### Scenario: Bell opens Solicitudes
- **WHEN** the merchant taps the bell on Mi perfil
- **THEN** the app switches to the Solicitudes tab

### Requirement: Role gating on the hub

Subscription and device management SHALL be offered only to users whose role is OWNER or ADMIN. For a SELLER:
- The subscription card and the Dispositivos row SHALL NOT be shown.
- The subscription and device endpoints SHALL NOT be called.

#### Scenario: Seller opens Perfil
- **WHEN** a user with role SELLER opens Mi perfil
- **THEN** neither the subscription card nor the Dispositivos row is shown
- **AND** neither `GET /api/v1/subscriptions/current` nor `GET /api/v1/devices` is requested

### Requirement: Subscription summary card

For OWNER and ADMIN, the subscription card SHALL be built from `GET /api/v1/subscriptions/current` and show:
- The plan badge (plan type) and a status dot with label ("Activo" for ACTIVE).
- The title "Mi suscripción" and "S/ <priceMonthlyPen> al mes".
- "Se renueva el <endsAt>", with the date in `es-PE`.
- A progress bar from `startedAt` to `endsAt`, labelled "Inicio: <date>" and "Renovación: <date>".

When `endsAt` is null, the renewal line and the progress bar SHALL be hidden. When the endpoint answers `404`, the card SHALL read "No tienes una suscripción activa" and still open Mi suscripción.

#### Scenario: Active subscription
- **WHEN** the subscription is PRO, ACTIVE, S/ 89.00, started 2025-08-15 and ending 2026-09-15
- **THEN** the card shows "PLAN PRO", "Activo", "S/ 89 al mes", "Se renueva el 15 de setiembre de 2026" and a progress bar at the elapsed fraction of that period

#### Scenario: No subscription
- **WHEN** `GET /api/v1/subscriptions/current` returns `404`
- **THEN** the card reads "No tienes una suscripción activa"

### Requirement: Sign out from Mi perfil

Mi perfil SHALL end with a "Cerrar sesión" action in the error colour. Tapping it SHALL ask for confirmation, then run the existing sign-out flow (see `session-management`).

#### Scenario: Sign out confirmed
- **WHEN** the merchant taps "Cerrar sesión" and confirms
- **THEN** the sign-out flow runs and the app returns to the login screen

### Requirement: Datos personales is read-only

The Datos personales screen SHALL show, from `GET /api/v1/users/me`:
- A gradient top bar with back and the title "Datos personales".
- A card with the initials avatar and a "Cambiar foto" action marked *Próximamente*.
- A card with the fields "Nombre completo", "Correo" and "Teléfono".

All fields SHALL be read-only, and there SHALL be no save action. The phone SHALL be shown as "+51 999 888 777", or "—" when absent. The screen SHALL show a loading state while fetching, and an error state with retry on failure.

#### Scenario: Fields displayed
- **WHEN** the profile has `fullName` "Carla Mendoza", `email` "carla.mendoza@gmail.com" and `phone` "999888777"
- **THEN** the fields show "Carla Mendoza", "carla.mendoza@gmail.com" and "+51 999 888 777"
- **AND** none of them can be edited

#### Scenario: Missing phone
- **WHEN** the profile has no phone
- **THEN** the Teléfono field shows "—"

#### Scenario: Change photo not available
- **WHEN** the merchant taps "Cambiar foto"
- **THEN** nothing is uploaded and the action is presented as *Próximamente*

### Requirement: Mi suscripción screen

The Mi suscripción screen (OWNER/ADMIN) SHALL show a plan card with:
- The plan badge, status, "S/ <price> al mes" and the progress bar.
- A checklist of features derived from the plan: "Hasta N productos", or "Productos ilimitados" when `productLimit` is null.
- One line for each true flag: "Estadísticas avanzadas", "Productos con video", "Actualización masiva de precios", "Soporte prioritario".

Below the card, the sections "Método de pago", "Historial de pago" and the action "Cancelar mi suscripción" SHALL be shown as *Próximamente*, with no payment data. A `404` SHALL show an empty state "No tienes una suscripción activa". Other failures SHALL show an error state with retry.

#### Scenario: Features from flags
- **WHEN** the plan has `productLimit` 200, `hasAdvancedAnalytics` true and every other flag false
- **THEN** the checklist shows exactly "Hasta 200 productos" and "Estadísticas avanzadas"

#### Scenario: Payment sections not available
- **WHEN** the Mi suscripción screen is shown
- **THEN** "Método de pago", "Historial de pago" and "Cancelar mi suscripción" are shown as *Próximamente* and trigger no request

### Requirement: Ayuda screen

The Ayuda screen SHALL show:
- **Preguntas frecuentes**: an accordion with four questions whose answers are local static text, each expanding and collapsing on tap:
  - "¿Cómo vinculo una tablet nueva?"
  - "¿De dónde salen las sugerencias de IA?"
  - "¿Puedo usar la app sin internet?"
  - "¿Cómo cambio los precios que ve el cliente?"
- **Guías rápidas**: "Primeros pasos con tu tablet vitrina", "Cómo armar combos que se venden" and "Leer tu reporte de ventas", each marked *Próximamente*.
- A footer "Versión <installed versionName> · Términos y privacidad", where "Términos y privacidad" is marked *Próximamente*.

#### Scenario: Expand a question
- **WHEN** the merchant taps "¿Puedo usar la app sin internet?"
- **THEN** its answer is shown below it and the chevron points up
- **AND** tapping it again collapses the answer

#### Scenario: Version footer
- **WHEN** the installed app version is 2.4.1
- **THEN** the footer reads "Versión 2.4.1 · Términos y privacidad"

### Requirement: Próximamente treatment

Every element whose data or action has no backend support SHALL keep its place in the layout. Such an element SHALL be non-interactive (or show a short "Próximamente" message when tapped), carry a visible *Próximamente* tag, use "—" in place of missing values, and trigger no network request. Its accessibility label SHALL include "Próximamente".

#### Scenario: Screen reader on an unavailable action
- **WHEN** a screen reader focuses "Cancelar mi suscripción"
- **THEN** it is announced with "Próximamente" and as disabled

### Requirement: Light and dark appearance

Every screen in this capability SHALL render with design-system tokens in both light and dark themes, and SHALL provide light and dark previews.

#### Scenario: Dark theme
- **WHEN** the device uses dark theme
- **THEN** Mi perfil, Datos personales, Mi suscripción and Ayuda render with dark tokens and stay legible
