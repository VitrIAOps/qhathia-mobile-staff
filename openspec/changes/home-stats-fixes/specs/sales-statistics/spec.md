## MODIFIED Requirements

### Requirement: Open the sales statistics from Inicio and return

The "Resumen de ventas" screen SHALL be opened from the `Ver más` action of the Inicio summary card and SHALL start on the period Inicio was showing. It MUST be shown without the bottom navigation bar. Going back — through the top-bar back control or the system back gesture — MUST return to Inicio, once: further taps on back while the screen is leaving MUST NOT leave Inicio. Coming back to the screen (from the background or a pushed screen) SHALL reload the tab on screen, keeping its figures until the new ones arrive.

#### Scenario: Ver más opens the screen on Inicio's period
- **WHEN** the Inicio card shows `Mensual` and the merchant taps `Ver más`
- **THEN** the "Resumen de ventas" screen opens with `Mensual` selected in its filters

#### Scenario: No bottom navigation bar
- **WHEN** the "Resumen de ventas" screen is shown
- **THEN** the bottom navigation bar is not displayed

#### Scenario: Back returns to Inicio
- **WHEN** the merchant taps the back control or uses system back
- **THEN** the Inicio tab is shown with the bottom navigation bar and Inicio selected

#### Scenario: Unknown period argument falls back to weekly
- **WHEN** the screen is opened without a valid period
- **THEN** `Semanal` is selected

#### Scenario: Double tap on back
- **WHEN** the merchant taps back twice quickly
- **THEN** Inicio is shown with the bottom navigation bar, never a blank screen

#### Scenario: Coming back reloads
- **WHEN** the merchant leaves the app with Ranking open and comes back
- **THEN** the Ranking tab is requested again and its figures stay on screen until the answer arrives

### Requirement: Label the period being shown

Above the tabs the screen SHALL show the calendar range of the period the active tab's data covers, taken from the backend's period bounds and expressed in `America/Lima` time and Spanish: a week as `24 – 30 de marzo` (naming both months when the week spans two, and years when it spans two), a month as `marzo 2026`, a day as `30 de marzo`, and an hour as `30 de marzo, 14:00 – 15:00`, the times following the phone's 12/24-hour setting (`30 de marzo, 2:00 p. m. – 3:00 p. m.` on 12 hours). When no bounds are available the label MUST NOT be rendered. On Ranking, when the backend sends the time its figures were computed (`asOf`), a line `Actualizado a las <hora>` SHALL follow the range.

#### Scenario: Weekly range
- **WHEN** the weekly data covers 24 to 30 March
- **THEN** the label reads `24 – 30 de marzo`

#### Scenario: Week spanning two months
- **WHEN** the weekly data covers 30 March to 5 April
- **THEN** the label reads `30 de marzo – 5 de abril`

#### Scenario: Monthly range
- **WHEN** the monthly data covers March 2026
- **THEN** the label reads `marzo 2026`

#### Scenario: No bounds, no label
- **WHEN** the active tab has no data loaded yet
- **THEN** no date label is rendered

#### Scenario: Hourly range on 12 hours
- **WHEN** the hourly data covers 14:00 to 15:00 on 30 March and the phone uses the 12-hour format
- **THEN** the label reads `30 de marzo, 2:00 p. m. – 3:00 p. m.`

#### Scenario: Ranking freshness
- **WHEN** the ranking answer has `asOf` 17:04 device time and the phone uses the 24-hour format
- **THEN** the line `Actualizado a las 17:04` follows the range

### Requirement: Rank the products that sell the most

The Ranking tab SHALL request up to 5 top sellers and show a `Lo que más vende` card with a highlighted total in the DS success colours reading `S/ <total>` (the revenue of the listed products) and a caption. When the backend sends how many products sold in the period (`productsSold`) and the store's revenue for the period (`periodRevenue`), the caption SHALL read `Top N de M productos · S/ <total> de S/ <period revenue>`; otherwise it SHALL read `Generados por tus N productos principales` (N being the number of products listed). One row per top-selling product SHALL follow, with its position badge, thumbnail, name, `N vendidos` (singular for one) and revenue. A product that has been deleted since SHALL stay in the list and its name SHALL be followed by `· Eliminado`, also in its accessible text. When there are no top sellers the card SHALL show only its title and an empty message: no total, no trend icon, no caption and no rows.

#### Scenario: Top sellers card
- **WHEN** three products generated S/ 4,090, the backend sends no `productsSold` nor `periodRevenue`, and the first sold 34 units for S/ 1,870
- **THEN** the card shows `S/ 4,090`, `Generados por tus 3 productos principales` and a first row with badge `1`, its name, `34 vendidos` and `S/ 1,870`

#### Scenario: Share of the period
- **WHEN** 5 products are listed with S/ 2,385, `productsSold` is 11 and `periodRevenue` is 2848.50
- **THEN** the caption reads `Top 5 de 11 productos · S/ 2,385 de S/ 2,849`

#### Scenario: A deleted product
- **WHEN** a listed product has `active` false
- **THEN** its row shows its name followed by `· Eliminado`

#### Scenario: Singular units
- **WHEN** a top seller sold 1 unit
- **THEN** its row reads `1 vendido`

#### Scenario: No top sellers
- **WHEN** nothing sold in the period
- **THEN** the card shows its title and an empty message, and no `S/ 0`, no caption and no rows

### Requirement: Surface the products that need attention

The Ranking tab SHALL show a `Lo que necesita atención` card with a highlighted total in the DS error colours reading `S/ <total immobilized>`, followed by one row per active product with stock that did not sell in the period, ordered by immobilized amount from highest to lowest. The total is the money immobilized in the listed products (`stock × price`), not revenue, and its caption MUST say so: it SHALL read `Inmovilizados en los N productos con stock sin ventas` (`Inmovilizados en el producto con stock sin ventas` for one), where N is the number of listed products. When the backend reports a total count of products without sales larger than N, the caption SHALL read `Inmovilizados en N de los M productos con stock sin ventas`, where M is that total count, which counts active products with stock and no confirmed sales in the period. The caption MUST NOT use the wording `Generados`, which the revenue card uses. Each row SHALL show an alert glyph — error colour on the first row, warning colour on the others — thumbnail, name, `N unidades sin vender` (singular for one) and the product's immobilized amount (`stock × price`). When there are none the card SHALL show only its title and an empty message: no total, no trend icon, no caption and no rows.

#### Scenario: Needs-attention card
- **WHEN** three products hold S/ 4,020 of unsold stock, the backend reports a total count of 3, and one of them has 34 units immobilizing S/ 3,400
- **THEN** the card shows `S/ 4,020`, `Inmovilizados en los 3 productos con stock sin ventas`, and that product's row reads `34 unidades sin vender` and `S/ 3,400`

#### Scenario: More products without sales than listed
- **WHEN** the card lists 3 products and the backend reports a total count of 17
- **THEN** the caption reads `Inmovilizados en 3 de los 17 productos con stock sin ventas`

#### Scenario: Total count missing
- **WHEN** the card lists 3 products and the backend does not send a total count
- **THEN** the caption reads `Inmovilizados en los 3 productos con stock sin ventas`

#### Scenario: A single product
- **WHEN** the card lists one product and the total count is 1
- **THEN** the caption reads `Inmovilizados en el producto con stock sin ventas`

#### Scenario: Rows ordered by immobilized money
- **WHEN** the backend returns products immobilizing S/ 50, S/ 620 and S/ 100
- **THEN** the rows are shown in the order S/ 620, S/ 100, S/ 50 and only the first uses the error colour

#### Scenario: Nothing needs attention
- **WHEN** every product with stock sold in the period
- **THEN** the card shows its title and an empty message, and no `S/ 0`, no caption and no rows

#### Scenario: A restock shows on return
- **WHEN** the caption reads `… de los 46 productos con stock sin ventas`, a product without stock is restocked elsewhere, and the merchant comes back to the screen
- **THEN** the caption reads `… de los 47 productos con stock sin ventas`

## ADDED Requirements

### Requirement: Share the tab on screen

The screen SHALL show a gradient top bar (DS `headerGradient`) with a back control, the title `Resumen de ventas` and a share control. The share control SHALL be enabled only while the tab on screen has data for the current filters and is not loading. Tapping it SHALL open the system share sheet with a plain-text summary of that tab: the screen title and period range, then for Resumen the total, the number of sales, the average ticket and the units; for Ranking the top sellers with their revenue; for Actividad the tablet sessions, the products viewed and the confirmed orders. The text MUST NOT contain customer data.

#### Scenario: Top bar contents
- **WHEN** the screen is shown
- **THEN** the top bar shows a back control, `Resumen de ventas` and a share control

#### Scenario: Sharing the summary
- **WHEN** Resumen is loaded for `2 – 8 de noviembre` and the merchant taps share
- **THEN** the system share sheet opens with a text starting `Resumen de ventas · 2 – 8 de noviembre` and the period's total

#### Scenario: Share while loading
- **WHEN** the tab on screen is loading
- **THEN** the share control is disabled and tapping it opens nothing

## REMOVED Requirements

### Requirement: Show the top bar with an inert share action

**Reason**: Share now works (F-89).

**Migration**: Replaced by "Share the tab on screen", which keeps the top bar contents.
