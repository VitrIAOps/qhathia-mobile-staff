## MODIFIED Requirements

### Requirement: Greet the merchant and show data freshness

The Inicio tab SHALL open on a gradient header (DS `headerGradient`) carrying the greeting `Hola <nombre>`, where `<nombre>` is the first name taken from the authenticated session's `user.fullName`, and — once a load has succeeded — the line `Última actualización a las <hh:mm>` showing the device-local wall-clock time of that load, in the app's language and following the phone's 12/24-hour setting. The name MUST come from the authenticated session, never from the summary data.

#### Scenario: Greeting uses the session's first name
- **WHEN** the merchant `Maria Lopez` opens the Inicio tab
- **THEN** the header reads `Hola Maria`

#### Scenario: Freshness line appears after the first successful load
- **WHEN** the summary loads successfully at 23:50 device time
- **THEN** the header shows `Última actualización a las 11:50 p. m.` with the phone set to 12 hours

#### Scenario: No freshness line before any successful load
- **WHEN** the screen is still loading and no load has yet succeeded
- **THEN** no `Última actualización` line is rendered

#### Scenario: Missing session name degrades gracefully
- **WHEN** no session name is available
- **THEN** the greeting renders without a name and no placeholder name is shown

#### Scenario: Freshness line with the phone on 24 hours
- **WHEN** the summary loads successfully at 17:04 device time and the phone uses the 24-hour format
- **THEN** the header shows `Última actualización a las 17:04`

#### Scenario: Freshness line with the phone on 12 hours
- **WHEN** the summary loads successfully at 17:04 device time, the app is in Spanish and the phone uses the 12-hour format
- **THEN** the header shows `Última actualización a las 5:04 p. m.`

### Requirement: Reach pending orders from the Home bell

The header SHALL include a notification bell control that navigates to the Solicitudes tab. The navigation MUST behave as a tab switch — reusing the bottom bar's navigation options — rather than pushing a second Solicitudes destination on top of Inicio. The bell SHALL display a count badge when there is at least one pending order, and no badge when there are none. The badge count MUST be the same value shown on the Solicitudes bottom-navigation tab.

#### Scenario: Bell navigates to Solicitudes
- **WHEN** the merchant taps the bell
- **THEN** the Solicitudes tab is shown and the bottom bar marks Solicitudes as the selected tab

#### Scenario: Badge shows the pending count
- **WHEN** there are 2 pending orders
- **THEN** the bell displays a badge reading `2`

#### Scenario: No pending orders, no badge
- **WHEN** there are no pending orders
- **THEN** the bell displays no badge

#### Scenario: Bell is announced with its pending count
- **WHEN** a screen reader focuses the bell while 2 orders are pending
- **THEN** the control is announced as a single button naming both the action and the pending count, and it can be activated from the screen reader

### Requirement: Suggest products that could increase sales

The Inicio tab SHALL show a `Sugerencias para vender más` section containing three horizontally-scrollable rows:

- **Productos no encontrados** — one card per kiosk search group that found no product in the store's line of business (`groups` of `GET /api/v1/insights/unmet-searches`), showing the number of **distinct people** who searched for it (`personCount`), the canonical term (`term`, first letter capitalised) as the product name, a secondary line with how shoppers asked for it, and an `Agregar` button.
- **También te piden** — one card per group that shoppers asked for but is outside the store's line of business (`otherRequests` of the same response), with the same contents as a "Productos no encontrados" card but **without** any action button.
- **Productos para reponer** — one card per product below the restock threshold (`GET /api/v1/dashboard/restock`), showing its image, its name, a warning chip reading `Solo queda N` (or its plural) and a `Reabastecer` button.

The secondary line SHALL show the group's most frequent example (`examples[].rawText` with the highest `count`), trimmed and in quotes, and SHALL be omitted when there is no example or when it equals the term once case, accents and punctuation are ignored.

When a group lists other terms shoppers used (`variants`, besides the group's own term), the card SHALL show them as a line `También: <term>, <term>` (at most three). When the backend marks the count as estimated (`personCountEstimated`), the card SHALL show `~N` with the label `Búsquedas` (singular for one) instead of `Personas`, and SHALL be announced as approximately N searches.

`Reabastecer` SHALL open that product's Detalle, and `Agregar` SHALL open the Registro form with `Nombre` set to the card's product name. Both SHALL be opened over the Inventario tab, so that saving returns to Inventario with its confirmation, and back from them returns to Inventario. A row with no items MUST be omitted rather than rendered empty, and the section heading SHALL be shown whenever at least one row has items.

#### Scenario: Missing-product card contents
- **WHEN** 9 distinct people searched for the term `taladro`, most often as `taladro percutor bosch`
- **THEN** a card shows `9`, `Personas`, `Taladro`, the line `“taladro percutor bosch”` and an `Agregar` button

#### Scenario: Example that adds nothing is omitted
- **WHEN** the most frequent example of the term `amoladora` is `Amoladora.`
- **THEN** the card shows `Amoladora` and no secondary line

#### Scenario: Group without examples
- **WHEN** a group has no examples
- **THEN** its card shows the count and the term, and no secondary line

#### Scenario: Requests outside the line of business
- **WHEN** the response's `otherRequests` holds `pintura` asked for by 3 people
- **THEN** a `También te piden` row shows a card with `3`, `Personas` and `Pintura`, and the card has no button

#### Scenario: No requests outside the line of business
- **WHEN** `otherRequests` is empty or absent
- **THEN** the `También te piden` row is not rendered

#### Scenario: Only requests outside the line of business
- **WHEN** `groups` and the restock list are empty but `otherRequests` is not
- **THEN** the `Sugerencias para vender más` heading and the `También te piden` row are shown, and the other two rows are not

#### Scenario: Restock card contents
- **WHEN** `Taladro Percutor Bosch GSB 13 RE` has 1 unit left
- **THEN** a card shows its image, its name, a `Solo queda 1` warning chip and a `Reabastecer` button

#### Scenario: Restock card for a product with no stock left
- **WHEN** a product in the restock row has 0 units left
- **THEN** its warning chip reads `Sin existencias` instead of `Solo quedan 0`

#### Scenario: Reabastecer opens the product
- **WHEN** the merchant taps `Reabastecer` on the card of `Cinta LED solar exterior`
- **THEN** the Inventario tab is selected and that product's Detalle opens, loaded with `GET /api/v1/products/{id}` for its id

#### Scenario: Agregar opens Registro with the name
- **WHEN** the merchant taps `Agregar` on the card of `Soldadora`
- **THEN** the Inventario tab is selected and the Registro form opens with `Nombre` reading `Soldadora`
- **AND** saving the product returns to Inventario with the registration confirmation

#### Scenario: Other terms shoppers used
- **WHEN** the group `soldadora` has the variants `soldadora`, `soldadora electrica` and `soldadora inverter`
- **THEN** its card shows the line `También: soldadora electrica, soldadora inverter`

#### Scenario: Estimated count
- **WHEN** a group has `personCount` 3 and `personCountEstimated` true
- **THEN** its card shows `~3` and `Búsquedas` instead of `Personas`

#### Scenario: Rows scroll horizontally
- **WHEN** a row holds more cards than fit on screen
- **THEN** the row scrolls horizontally and the rest of the screen does not move with it

#### Scenario: Empty row is omitted
- **WHEN** there are no products to restock
- **THEN** the `Productos para reponer` row and its heading are not rendered

## ADDED Requirements

### Requirement: Announce the Solicitudes tab as a tab

Every bottom-navigation tab SHALL be announced with the tab role and its selected state, and SHALL keep its click action for screen readers. The Solicitudes tab with a pending-count badge SHALL be one node that names the tab and the count, with the tab role, its selected state and its click action.

#### Scenario: Badged Solicitudes tab
- **WHEN** a screen reader focuses the Solicitudes tab while 1 order is pending
- **THEN** it is announced as `Solicitudes, 1 pendiente`, as a tab, and it can be activated from the screen reader

#### Scenario: Other tabs
- **WHEN** a screen reader focuses the Inventario tab
- **THEN** it is announced as a tab with its selected state
