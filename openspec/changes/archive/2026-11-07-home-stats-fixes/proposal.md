## Why

The 2026-11-03 QA regression (`docs/handoff/android-regresion-2026-11-03.md` §3.2 and §3.3) finds that Inicio and "Resumen de ventas" still show designed-but-dead controls and say less than the backend now sends (Fase 41 and 42):

- **F-56** — "Agregar" (Productos no encontrados) and "Reabastecer" (Productos para reponer) do nothing (S:INI-013).
- **F-89** — "Compartir" in Resumen de ventas does nothing (S:EST-003).
- **F-64** — Two quick taps on back in Resumen de ventas pop Inicio too and leave a blank screen (S:EST-061). Detalle, Registro and Dispositivos have the same unguarded pop.
- **F-100** — Ranking shows only 3 products and "Generados por tus 3 productos principales", with no "S/ X de S/ Y" and no mark on deleted products, although the backend sends `periodRevenue`, `productsSold`, `items[].active` and takes `limit`.
- **F-95** — "Lo que necesita atención" says "productos sin ventas" for what the backend counts as "con stock sin ventas", and never reloads after a restock.
- **F-76** — "Última actualización a las 17:04" with the phone set to 12 hours; also in Solicitudes and in the "Por hora" range.
- **F-57** — The Inicio bell and the badged Solicitudes tab lose their click action and role for screen readers (S:INI-062).
- **F-75 (Inicio part)** — Opening Inicio sends `GET /orders?status=PENDING&size=1` twice: Inicio and the shell's reconcile ask at the same time (S:INI-004).
- **Unmet searches (Fase 41)** — Each group now has `variants` (top 3 terms) and `personCountEstimated` (the count is searches, not people); Inicio shows neither.

F-94 ("Catálogo visible") and the time format on the Dispositivos card go to a separate change, `devices-catalog-count`, because those files changed in changes 4 and 5.

## What Changes

- **Agregar / Reabastecer open the right screen (F-56).** "Reabastecer" opens that product's Detalle and "Agregar" opens Registro with "Nombre" set to the term, both over Inventario, so saving lands on Inventario with its snackbar. The prefilled name does not count as an unsaved change.
- **Compartir (F-89).** It opens Android's share sheet with a plain-text summary of the tab on screen (period, totals, top sellers or activity). It is disabled while that tab has nothing loaded or is loading. No customer data is shared.
- **Back once (F-64).** The shell pops a pushed screen only while it is still the one on top, and only navigates from a resumed screen, so a second tap never pops Inicio or Inventario. Resumen de ventas also emits its exit once.
- **Ranking says what it shows (F-100).** Up to 5 top sellers (`limit=5`). The caption reads "Top 5 de 11 productos · S/ 2,385 de S/ 2,849" when the backend sends `productsSold` and `periodRevenue`; a product removed since shows "· Eliminado". Below the range, "Actualizado a las 5:04 p. m." from `asOf`.
- **"Con stock sin ventas" and reload on return (F-95).** The caption reads "Inmovilizados en los N productos con stock sin ventas". Coming back to Resumen de ventas reloads the tab on screen, keeping its figures until the new ones arrive.
- **The phone's 12/24-hour setting (F-76).** Inicio's "Última actualización", Solicitudes' header and the "Por hora" range use the phone's setting and the app's language: "5:04 p. m." or "17:04".
- **Bell and tab are controls (F-57).** The bell is announced as a button and the badged Solicitudes tab as a tab, both with their click action; every tab carries the tab role and its selected state.
- **One count request at a time (F-75).** A count refresh that starts while another is in flight shares its answer. A push, or a confirm or reject, always gets a request that started after it, so a new order is never missed.
- **Variants and estimated counts.** A missing-product card lists the other terms shoppers used ("También: soldadora eléctrica, soldadora inverter"), and shows "~3" with "búsquedas" when the backend estimates the count.

## Capabilities

### New Capabilities

_None._

### Modified Capabilities

- `home-summary`: "Greet the merchant and show data freshness" (12/24 h), "Reach pending orders from the Home bell" (button role), "Suggest products that could increase sales" (Agregar / Reabastecer navigate; variants; estimated counts); new "Announce the Solicitudes tab as a tab". The suggestions delta builds on `home-missing-products-examples-and-other-requests`, which must be archived first.
- `sales-statistics`: "Open the sales statistics from Inicio and return" (back once), "Label the period being shown" (12/24 h, "Actualizado a las"), "Rank the products that sell the most" (limit, caption, Eliminado), "Surface the products that need attention" (wording, reload on return). "Show the top bar with an inert share action" is replaced by "Share the tab on screen".
- `product-registration`: new "Prefill the name from a missing-product suggestion".
- `order-requests`: "Surface the pending-order count across the app" (concurrent refreshes share one request). This delta builds on the version in `refresh-on-return`, which must be archived first.

## Impact

- **`app`** — `MainShell` (Home's two new callbacks, guarded pop and navigate).
- **`core:ui`** — `QhathiaBottomBar` (tab role and click action), `component/time/TimeOfDay.kt` (12/24-hour formatter).
- **`core:network`** — `TopSellersDto` (`periodRevenue`, `productsSold`), `TopSellerItemDto.active`, `SalesRankingDto.asOf`, `getRanking(limit)`; `UnmetSearchGroupDto` (`variants`, `personCountEstimated`).
- **`core:domain`** — `SalesRanking`, `TopSellingProduct`, `MissingProductSuggestion`, `OrderRepository.refreshPendingOrderCount(reuseInFlight)`, `HandleOrderPushUseCase`.
- **`core:data`** — `OrderRepositoryImpl` (single flight), `StoreInsightsMappers`, the insights mapper.
- **`core:testing`** — `FakeOrderRepository`, fixtures.
- **`features:home`**, **`features:sales-statistics`**, **`features:product-register`** (name argument), **`features:order-requests`** (header time).
- **No new endpoint.** `GET /dashboard/ranking?limit=` and the Fase 41/42 fields.
- **Branch:** cut from `feature/refresh-on-return` (change 3).
