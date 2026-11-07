# merchant-profile Specification

## Purpose
TBD - created by archiving change add-merchant-profile-settings. Update Purpose after archive.
## Requirements
### Requirement: Mi perfil hub behind the Perfil tab

Selecting the Perfil tab of the bottom navigation SHALL show the "Mi perfil" screen, which replaces the previous placeholder. The screen SHALL show, top to bottom:
1. A gradient header with the title "Mi perfil" and a notifications bell.
2. An identity card with an initials avatar, full name, email, and a role · store-count line. The card has an edit action.
3. A subscription card.
4. A menu card with the rows Datos personales, Notificaciones, Seguridad, Configuración, Dispositivos and Ayuda.
5. A "Cerrar sesión" action.

The bottom navigation bar SHALL remain visible on Mi perfil.

Mi perfil SHALL load its blocks once each time it is shown. Coming back to it within 30 seconds of the last load SHALL NOT load again, except when coming back from Datos personales, Notificaciones or Dispositivos, which SHALL always reload, because they change what the hub shows. A retry action SHALL always reload.

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

#### Scenario: Opening Mi perfil loads once
- **WHEN** the merchant opens the Perfil tab
- **THEN** `GET /api/v1/users/me` and the notification preferences are each requested once

#### Scenario: Back from Ayuda
- **WHEN** the merchant opens Ayuda and comes back within 30 seconds
- **THEN** no request is sent

#### Scenario: Back from Notificaciones
- **WHEN** the merchant opens Notificaciones, turns off a toggle and comes back
- **THEN** the hub reloads and the Notificaciones row shows the new count

#### Scenario: Back after a while
- **WHEN** the merchant comes back to Mi perfil more than 30 seconds after the last load
- **THEN** the hub reloads

### Requirement: Menu rows show live subtitles

Each menu row SHALL show an icon, a title, a subtitle and a trailing chevron:
- **Datos personales**: "Nombre, correo y teléfono" — what the screen holds; it shows no store data.
- **Notificaciones**: "N de M alertas activas", counting only the alerts the backend supports: M is how many are supported and N how many of those are on.
- **Seguridad**: "Contraseña, huella y sesiones".
- **Configuración**: "Idioma de la app". Prices are not configured in the app, so the subtitle does not mention them.
- **Dispositivos**: the linked and active tablets, plural-aware and in the feminine ("tablet" is feminine in Spanish): "N vinculada(s) (M activa(s))".
- **Ayuda**: "Soporte por WhatsApp y guías".

While a live subtitle is loading or has failed, the row SHALL show its static fallback subtitle rather than a number.

#### Scenario: Notification count subtitle
- **WHEN** the backend supports "Nuevo pedido" and "Pedido por expirar", both on, and the five other toggles are on but unsupported
- **THEN** the Notificaciones row subtitle reads "2 de 2 alertas activas"

#### Scenario: A supported alert turned off
- **WHEN** "Nuevo pedido" is turned off and the merchant returns to Mi perfil
- **THEN** the Notificaciones row subtitle reads "1 de 2 alertas activas"

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

### Requirement: Edit personal data

The Datos personales screen SHALL show, from `GET /api/v1/users/me`:
- A gradient top bar with back and the title "Datos personales".
- A card with the initials avatar and a "Cambiar foto" action marked *Próximamente*.
- A card with the fields "Nombre completo", "Correo" and "Teléfono", and a **Guardar cambios** button.

"Nombre completo" and "Teléfono" SHALL be editable. "Correo" SHALL be shown disabled, with a note that it cannot be changed because it is the account's login, and SHALL never be sent. The phone SHALL be edited as its 9 digits, with the `+51` prefix shown by the app; the field SHALL accept digits only and at most 9 of them.

The name is valid when, after trimming, it has 2 to 150 characters. The phone is valid when it has exactly 9 digits and starts with 9. An empty phone SHALL be valid: when the profile had a phone, emptying the field removes it on save, and the field SHALL say so ("Se quitará tu celular al guardar"); when it had none, it means "still none". The phone's hint SHALL read "Celular de 9 dígitos (opcional)". Invalid values SHALL show an error under their field.

**Guardar cambios** SHALL be enabled only when at least one editable value differs from the loaded profile, every value is valid, and no save is in progress. Saving SHALL send `PUT /api/v1/users/me` with only the changed fields, the name trimmed, and `"phone": ""` for a removed phone. The keyboard's Done action on the phone field SHALL save when **Guardar cambios** is enabled. While saving, the fields and the button SHALL be disabled and the button SHALL show progress.

On success, the screen SHALL show the values returned by the server, confirm with a snackbar, and update the stored session's name, so that the Inicio greeting and the Mi perfil header use the new name without signing in again. On failure, the typed values SHALL be kept and a message SHALL be shown: one asking to review the data for a `422`, one about connectivity for a network failure, and a generic one otherwise.

With unsaved changes, the system back and the top bar's back SHALL ask "¿Descartar cambios?" with "Descartar" and "Seguir editando". "Seguir editando" SHALL close the dialog and keep the typed values; "Descartar" SHALL go back. With no unsaved changes, back SHALL go back at once. While saving, back SHALL do nothing.

The screen SHALL show a loading state while fetching and an error state with retry when the profile cannot be loaded.

#### Scenario: Fields displayed
- **WHEN** the profile has `fullName` "Carla Mendoza", `email` "carla.mendoza@gmail.com" and `phone` "999888777"
- **THEN** the name field shows "Carla Mendoza", the email "carla.mendoza@gmail.com" disabled, and the phone field "999888777" with the "+51" prefix
- **AND** Guardar cambios is disabled

#### Scenario: Save a new name
- **WHEN** the merchant changes the name to "  Carla Mendoza Ruiz " and taps Guardar cambios
- **THEN** `PUT /api/v1/users/me` is sent with `{ "fullName": "Carla Mendoza Ruiz" }` and no other field
- **AND** the screen shows the saved name, a confirmation snackbar appears, and Inicio greets "Carla"

#### Scenario: Save a new phone
- **WHEN** the merchant changes the phone to "987654321" and taps Guardar cambios
- **THEN** `PUT /api/v1/users/me` is sent with `{ "phone": "987654321" }` and no other field

#### Scenario: Invalid phone
- **WHEN** the merchant types "88765432"
- **THEN** the phone field shows an error asking for 9 digits starting with 9, and Guardar cambios is disabled

#### Scenario: Name too short
- **WHEN** the merchant leaves the name as "C"
- **THEN** the name field shows an error and Guardar cambios is disabled

#### Scenario: Removing a saved phone
- **WHEN** the profile had a phone and the merchant empties the field
- **THEN** the field shows "Se quitará tu celular al guardar" and no error, and Guardar cambios is enabled
- **AND** saving sends `{ "phone": "" }` and no other field, and after the `200` the field stays empty

#### Scenario: No phone yet
- **WHEN** the profile has no phone and the merchant only changes the name
- **THEN** the empty phone field shows no error and the save sends only `fullName`

#### Scenario: Email is never sent
- **WHEN** any save is sent
- **THEN** its body contains no `email` field

#### Scenario: Server rejects the data
- **WHEN** the save fails with `422`
- **THEN** the typed values stay and a message asks the merchant to review the data

#### Scenario: Offline save
- **WHEN** the save fails for lack of connectivity
- **THEN** the typed values stay and a connectivity message is shown

#### Scenario: Change photo not available
- **WHEN** the merchant taps "Cambiar foto"
- **THEN** nothing is uploaded and the action is presented as *Próximamente*

#### Scenario: Back with unsaved changes
- **WHEN** the merchant has changed the phone and presses back (system or top bar)
- **THEN** "¿Descartar cambios?" is shown
- **AND** "Seguir editando" keeps the typed phone, and "Descartar" returns to Mi perfil

#### Scenario: Back with no changes
- **WHEN** nothing was changed and the merchant presses back
- **THEN** the app returns to Mi perfil without asking

#### Scenario: Saving from the keyboard
- **WHEN** the values are valid and changed and the merchant taps Done on the phone field
- **THEN** the save is sent

