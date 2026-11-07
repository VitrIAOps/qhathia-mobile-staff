## ADDED Requirements

### Requirement: Readable in dark mode

In dark mode, the inactive bottom-navigation tabs and the "Ver más" action on Inicio SHALL use a colour with a contrast of at least 4.5:1 against the dark background. Light mode SHALL keep its colours.

#### Scenario: Inactive tabs in dark mode
- **WHEN** the phone is in dark mode and Inicio is selected
- **THEN** "Inventario", "Solicitudes" and "Perfil" are clearly readable in the bottom bar

#### Scenario: Ver más in dark mode
- **WHEN** the phone is in dark mode
- **THEN** "Ver más" on Inicio is clearly readable

### Requirement: Complete labels at large font sizes

Single-line labels in shared controls — the bottom-navigation labels, Inicio's period pills, the tabs and period filter of Resumen de ventas, and the "vs …" pills of Actividad — SHALL shrink to fit their space instead of being cut. At a font scale of 1.5 or more, the period pills SHALL use short labels: "Mes", "Sem.", "Día", "Hora" in Spanish and "Month", "Wk.", "Day", "Hour" in English.

#### Scenario: Bottom bar at font scale 2.0
- **WHEN** the system font scale is 2.0
- **THEN** the bottom bar shows "Inicio", "Inventario", "Solicitudes" and "Perfil" in full, each in its own tab

#### Scenario: Period pills at font scale 2.0
- **WHEN** the system font scale is 2.0
- **THEN** Inicio's period pills read "Mes", "Sem.", "Día" and "Hora", none cut

#### Scenario: Normal font size
- **WHEN** the system font scale is 1.0
- **THEN** the pills read "Mensual", "Semanal", "Diario" and "Por hora"

### Requirement: Inventory list clears the floating button and resets on a new filter

The Inventario list SHALL leave enough space under its last row for that row's actions to be scrolled above the floating "add product" button. Changing the category or the availability filter SHALL show the list from its first row. Coming back to the tab SHALL keep the position.

#### Scenario: Last row's actions
- **WHEN** the merchant scrolls to the end of the list
- **THEN** the last row's ⋮ is above the floating button and can be tapped

#### Scenario: New category
- **WHEN** the merchant has scrolled down and selects another category
- **THEN** the list shows its first row

### Requirement: Shared controls announced with name, role and state

Screen readers SHALL announce:
- The discount row as a switch with its state, "Activado" or "Desactivado".
- Each form field in Detalle and Registro with the name of its label ("Precio", "Stock", "Tags"…).
- Each inventory thumbnail as "Foto de <product name>", or "Sin foto" when the product has none.
- Each photo in the photo strip as "Foto <n> de <total>", adding ", principal" for the first.
- Solicitudes' filter chips and Inicio's period pills as tabs in a group, with their selected state.

#### Scenario: Discount switch
- **WHEN** TalkBack focuses the discount row while the discount is on
- **THEN** it is announced as a switch, "Activado"

#### Scenario: Price field
- **WHEN** TalkBack focuses the price field in Detalle
- **THEN** it is announced as "Precio" with its value

#### Scenario: Inventory thumbnail
- **WHEN** TalkBack focuses the thumbnail of "Taladro Bosch"
- **THEN** it is announced as "Foto de Taladro Bosch"

#### Scenario: Photo strip
- **WHEN** TalkBack focuses the first of three photos
- **THEN** it is announced as "Foto 1 de 3, principal"

#### Scenario: Solicitudes chips
- **WHEN** TalkBack focuses "Pendientes (2)" while it is selected
- **THEN** it is announced as a selected tab
