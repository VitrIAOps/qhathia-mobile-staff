# home-summary Specification

## Purpose

Defines the Inicio tab of the Qhathia staff app — the start destination of the authenticated shell: a greeting header with the data-freshness line and a notification bell badged with the app-wide pending-order count, a period-switchable sales summary card (headline revenue of the period in progress, its variation, a four-bucket chart and an insight sentence) served from `GET /api/v1/dashboard/sales`, and the "Sugerencias para vender más" rows — products shoppers searched for and did not find (`GET /api/v1/insights/unmet-searches`) and products to restock (`GET /api/v1/dashboard/restock`) — with loading, stale-data-plus-error recovery, and design-system conformance in light and dark mode.
## Requirements
### Requirement: Open the app on Inicio

The authenticated shell SHALL use Inicio as its start destination. Entering the app — after a successful login or with a restored session — MUST show the Inicio tab with its bottom-navigation item selected. Switching tabs from Inicio SHALL keep the existing tab-switch behaviour (one copy per tab, state restored).

#### Scenario: Login lands on Inicio
- **WHEN** the merchant logs in successfully
- **THEN** the Inicio tab is shown and the bottom bar marks Inicio as the selected tab

#### Scenario: Restored session lands on Inicio
- **WHEN** the merchant reopens the app with a valid saved session
- **THEN** the Inicio tab is shown and the bottom bar marks Inicio as the selected tab

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

### Requirement: Show the sales summary for a selectable period

The Inicio tab SHALL show a summary card offering four periods — **Mensual**, **Semanal**, **Diario** and **Por hora** — with **Semanal** selected when the screen opens. The card SHALL display the label of the period in progress (e.g. `Esta semana`), the revenue of the period in progress formatted as Peruvian soles (`S/ 1,690`), and the variation against the previous comparable period, rendered in the DS success colour when it is zero or positive and in the DS error colour when negative. The variation is a decimal percentage rounded for display; when the backend reports no comparison base (`deltaPercent: null`) the variation line MUST be omitted rather than shown as `+100%` or `0%`. Selecting a period MUST replace the label, total, variation, chart and insight with that period's data.

#### Scenario: Semanal is the default period
- **WHEN** the merchant opens the Inicio tab
- **THEN** the `Semanal` tab is shown as selected and the weekly summary is displayed

#### Scenario: Total and variation are rendered
- **WHEN** the week in progress has revenue 1690 with a variation of 24.3%
- **THEN** the card shows `S/ 1,690` and `+24% vs. semana anterior` in the success colour

#### Scenario: Negative variation
- **WHEN** the week in progress has a variation of -6.2%
- **THEN** the card shows `-6% vs. semana anterior` in the error colour

#### Scenario: No comparison base
- **WHEN** the previous week sold nothing and the backend reports `deltaPercent: null`
- **THEN** the card shows the total and no variation line

#### Scenario: Period in progress has not sold yet
- **WHEN** the week in progress has revenue 0 but earlier weeks in the chart sold
- **THEN** the card shows `S/ 0` together with the chart, not the empty treatment

#### Scenario: Switching period reloads the card
- **WHEN** the merchant selects `Mensual`
- **THEN** the card shows the monthly label, total, variation, chart and insight, and `Mensual` is shown as selected

#### Scenario: Selected period survives a refresh
- **WHEN** the merchant has selected `Diario` and the screen refreshes
- **THEN** the daily data is reloaded and `Diario` remains selected

### Requirement: Chart the period's buckets

The summary card SHALL render one bar per bucket of the selected period, each labelled with its amount above the bar and its name below it. The app SHALL request four buckets for every period, the last of which is the period in progress; bucket names are the labels the backend returns. The bucket with the highest amount MUST be emphasised in the brand accent colour, including its amount label, while the remaining bars use the card's bar fill. Every bar MUST remain visible regardless of how small its amount is relative to the highest one. When no bucket of the window sold anything, the card SHALL show the empty treatment instead of the chart.

#### Scenario: Bars are drawn per bucket
- **WHEN** the weekly summary has buckets `Sem 1` 1240, `Sem 2` 1480, `Sem 3` 1360 and `Sem 4` 1690
- **THEN** four bars are rendered, each showing its amount above it and its name below it

#### Scenario: Highest bucket is emphasised
- **WHEN** `Sem 4` holds the highest amount of the period
- **THEN** the `Sem 4` bar and its amount label are rendered in the brand accent colour and the other three are not

#### Scenario: A very small bucket stays visible
- **WHEN** one bucket's amount is a tiny fraction of the highest one
- **THEN** its bar is still rendered at a minimum visible height

#### Scenario: Chart is announced as one node
- **WHEN** a screen reader reaches the chart
- **THEN** it announces the period total and the highest bucket rather than reading each bar separately

### Requirement: Summarise the period in an insight strip

Below the chart the card SHALL show an insight strip with a star glyph and a sentence naming the highest bucket, its amount and its variation against the preceding bucket of the chart. The variation is computed by the app from the two buckets; when the highest bucket is the first one charted, or the preceding bucket sold nothing, the sentence MUST omit the comparison.

#### Scenario: Insight names the best bucket
- **WHEN** `Sem 13` generated 1690, which is 24.3% more than `Sem 12`
- **THEN** the strip reads `Sem 13 generó S/ 1,690, 24% más que la semana anterior.`

#### Scenario: Best bucket has no comparison base
- **WHEN** the highest bucket is the first bucket of the chart
- **THEN** the strip reads `<bucket> generó <amount>.` with no comparison

#### Scenario: Insight follows the selected period
- **WHEN** the merchant switches to `Mensual`
- **THEN** the strip is rewritten for the best month of that period

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

### Requirement: Serve the summary from the dashboard API

The summary and suggestion data SHALL be obtained through a domain repository port, so that the presentation layer is unaware of where the data comes from. The data layer SHALL satisfy the port from the API (`docs/android/api-contract.md` §17): `GET /api/v1/dashboard/sales?granularity=<period>&buckets=4` for the summary, and `GET /api/v1/dashboard/restock` together with `GET /api/v1/insights/unmet-searches` for the suggestions. The two suggestion requests SHALL run concurrently, and the suggestions load SHALL fail if either of them fails, so the screen keeps its previous rows under the existing failure rule.

#### Scenario: Presentation depends only on the port
- **WHEN** the Home ViewModel loads the summary
- **THEN** it depends only on the domain use cases, never on network or DTO types

#### Scenario: Period maps to the backend granularity
- **WHEN** the merchant selects `Por hora`
- **THEN** the sales endpoint is requested with `granularity=HOURLY` and `buckets=4`

#### Scenario: One suggestion source fails
- **WHEN** the restock request succeeds but the unmet-searches request fails
- **THEN** the suggestions load fails and the previously loaded rows stay on screen with the error indication

### Requirement: Recover from a failed load

The Inicio tab SHALL show a loading indication while data is being fetched and an error state with a retry action when a load fails. A failed load MUST keep any previously loaded summary and its freshness time on screen rather than blanking the screen.

#### Scenario: Loading indication
- **WHEN** the screen is fetching data and nothing has loaded yet
- **THEN** a loading indication is shown in place of the summary card

#### Scenario: Failure keeps stale data
- **WHEN** a refresh fails after a successful load
- **THEN** the previously loaded summary and its `Última actualización` time remain on screen alongside an error indication with a retry action

#### Scenario: Retry re-issues the load
- **WHEN** the merchant taps retry
- **THEN** the summary and suggestions are requested again for the selected period

### Requirement: Render the Inicio tab per the design system

The screen SHALL be built from the shared design-system theme — gradient header, Urbanist type scale, `Dimens` spacing and the DS colour roles — and MUST render correctly in both light and dark mode. The content SHALL scroll vertically so that large font scales and small viewports do not clip it.

#### Scenario: Dark mode
- **WHEN** the device is in dark mode
- **THEN** every surface, text and chart colour resolves through the theme with no hardcoded colour values

#### Scenario: Content scrolls
- **WHEN** the content is taller than the viewport
- **THEN** the screen scrolls vertically and the bottom navigation bar stays fixed

### Requirement: Open the sales statistics from Ver más

The `Resumen` heading of the Inicio tab SHALL show an enabled `Ver más` action that opens the "Resumen de ventas" screen, passing the period currently selected on the summary card. The navigation MUST push the screen on top of Inicio — not switch tabs — so that going back returns to Inicio with its state preserved.

#### Scenario: Ver más opens the statistics screen
- **WHEN** the merchant taps `Ver más` while `Semanal` is selected
- **THEN** the "Resumen de ventas" screen opens with `Semanal` selected

#### Scenario: Back restores Inicio
- **WHEN** the merchant had selected `Diario` on Inicio, opened `Ver más` and went back
- **THEN** Inicio is shown with `Diario` still selected

#### Scenario: Ver más is announced as an action
- **WHEN** a screen reader focuses `Ver más`
- **THEN** it is announced as an enabled button

### Requirement: Refresh Inicio when it returns to the screen

The Inicio tab SHALL re-read its summary, suggestions and pending-order count for the selected period every time it comes back on screen after having been hidden: returning from another bottom-navigation tab, returning from a screen pushed on top of it (such as the sales statistics), or the app returning from the background. This refresh SHALL behave like a manual refresh: it MUST keep the data already on screen while it is in flight and MUST keep the selected period. The first appearance of Inicio SHALL issue a single load, not two.

#### Scenario: Restocked product leaves the restock list
- **WHEN** Inicio lists a product under `Productos para reponer` as `Sin existencias`, the merchant restocks it from Inventario, and switches back to Inicio
- **THEN** Inicio re-reads its data and the product no longer appears as out of stock

#### Scenario: Back from the sales statistics
- **WHEN** the merchant opens the sales statistics from `Ver más` and goes back to Inicio
- **THEN** Inicio re-reads its data for the selected period

#### Scenario: App returns from the background
- **WHEN** the app returns to the foreground with Inicio on screen
- **THEN** Inicio re-reads its data

#### Scenario: Data stays on screen during the refresh
- **WHEN** Inicio refreshes on return and it already shows a summary
- **THEN** the summary and its `Última actualización` time stay visible until the new data arrives, and no full-screen loading state replaces them

#### Scenario: First appearance loads once
- **WHEN** Inicio is shown for the first time after the app shell opens
- **THEN** exactly one load is issued

### Requirement: Announce the Solicitudes tab as a tab

Every bottom-navigation tab SHALL be announced with the tab role and its selected state, and SHALL keep its click action for screen readers. The Solicitudes tab with a pending-count badge SHALL be one node that names the tab and the count, with the tab role, its selected state and its click action.

#### Scenario: Badged Solicitudes tab
- **WHEN** a screen reader focuses the Solicitudes tab while 1 order is pending
- **THEN** it is announced as `Solicitudes, 1 pendiente`, as a tab, and it can be activated from the screen reader

#### Scenario: Other tabs
- **WHEN** a screen reader focuses the Inventario tab
- **THEN** it is announced as a tab with its selected state

