## Why

The Inicio card answers "how is this period going" in one glance, but a merchant who wants to know *why* — which products carry the revenue, which ones are tying up money on the shelf, whether shoppers are even using the tablet — has nowhere to go: `Ver más` renders and does nothing. The backend has served everything that detail needs since Fase 17 (`GET /dashboard/sales`, `/dashboard/ranking`, `/dashboard/activity`), so the screen can ship on real data now.

ClickUp: [Estadisticas de comercio](https://app.clickup.com/t/86e38fgh6).

## What Changes

- **New "Resumen de ventas" screen**, opened from `Ver más` on the Inicio summary card, rendering the Figma design (`Qhatia-DS`, nodes `2238-30891` Resumen, `2238-31151` Ranking, `2238-31411` Actividad, `2238-31712` filters expanded).
  - **Top bar**: back chevron (returns to Inicio), title `Resumen de ventas`, and a **share** icon that renders but has **no action yet**.
  - **No bottom navigation bar** on this screen.
  - **Date label** for the period being shown (e.g. `24 – 30 de marzo`), taken from the backend's period bounds.
  - **Three tabs**: **Resumen**, **Ranking**, **Actividad**.
  - **Collapsible `Filtros` panel**: period selector (**Mensual · Semanal · Diario · Por hora**) and a product-category dropdown (`Todos` + the store's categories). The category selector is **hidden on Actividad**, whose endpoint does not accept a category.
- **Resumen tab**: the dark summary card (headline, variation, four-bar chart, insight) without its own period tabs, plus three KPI tiles — **Num Ventas**, **Ticket prom.**, **Unidades** — from the period in progress.
- **Ranking tab**:
  - **Lo que más vende** — total revenue of the top sellers and one row per product with its rank badge, thumbnail, name, units sold and revenue.
  - **Lo que necesita atención** — total immobilized money and one row per active product that did not sell, with its thumbnail, name, `N unidades sin vender` and its **immobilized amount** (`stock × price`), so the rows add up to the card total.
- **Actividad tab**: **Actividad en la tablet** card with **Clientes usaron la tablet** and **Productos consultados**, each with its variation against the previous period.
- **Inicio**: `Ver más` stops being inert and opens the new screen on the period currently selected on the Inicio card.
- **Shared summary components move to `:core:ui`**: the dark summary card, the bar chart and the insight strip currently private to `:features:home` become design-system components driven by UI models, so both features render the same card without a feature-to-feature dependency. Inicio is refactored onto them with no visible change.

**Decided with the requester:**
- The feature is a new module, `:features:sales-statistics`; shared pieces are extracted to `:core:ui` rather than duplicated.
- "Lo que necesita atención" shows the immobilized amount per product, not the unit price.
- The category filter is hidden on the Actividad tab.
- Share is inert.

## Capabilities

### New Capabilities
- `sales-statistics`: the "Resumen de ventas" detail screen — top bar with back and inert share, the period date label, the Resumen / Ranking / Actividad tabs, the collapsible period + category filters, and each tab's content served from the dashboard sales, ranking and activity endpoints, with loading and error recovery.

### Modified Capabilities
- `home-summary`: `Ver más` on the summary heading navigates to the sales statistics screen, carrying the selected period.

## Impact

- **New module:** `:features:sales-statistics` (Android library, Compose + Hilt) — screen, ViewModel, MVI contract, navigation graph. Registered in `settings.gradle.kts`, consumed by `:app`.
- **`:core:ui`:** new shared sales-summary components (card, bar chart, insight strip, KPI tile, segmented selector) with their UI models and strings.
- **`:features:home`:** summary card, chart and insight replaced by the `:core:ui` components; `Ver más` raises a navigation effect; `homeGraph` gains `onNavigateToSalesStatistics`.
- **`:core:domain`:** `SalesSummary` gains the period bounds and the KPIs of the period in progress; new `SalesRanking` and `StoreActivity` models; `StoreInsightsRepository` gains a category filter on the summary plus `getSalesRanking` and `getStoreActivity`; new use cases.
- **`:core:network`:** `DashboardApiService` gains `categoryId` on sales and the `ranking` and `activity` calls with their DTOs.
- **`:core:data`:** mappers and `StoreInsightsRepositoryImpl` extended accordingly.
- **`:core:testing`:** `FakeStoreInsightsRepository` and fixtures extended.
- **`:app`:** `MainShell` registers the new graph and wires Inicio's `Ver más` to it; the bottom bar is already hidden on non-top-level destinations.
- **APIs consumed:** `GET /api/v1/dashboard/sales` (now with `categoryId`), `GET /api/v1/dashboard/ranking`, `GET /api/v1/dashboard/activity`, `GET /api/v1/categories` (`api-contract.md` §17).
