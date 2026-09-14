## Why

The Inicio tab — the first thing a merchant sees after login and the tab the bottom bar opens on by default in the DS — is still `HomePlaceholderScreen`, a centered "Home" label. Every other tab (Inventario, Solicitudes, Perfil) is real, so the app's landing surface is the only one that answers no operational question at all.

The DS defines Inicio as the **pantalla de resumen**: how much the store sold in the period, whether it is up or down against the previous one, and what to do next (products shoppers searched for but the store does not carry, products about to run out). The backend exposes that since Fase 17 (`store-home-dashboard`), so the screen reads it through a domain port backed by those endpoints.

The one number on that screen that *is* real today is the pending-order count. It currently lives only inside the Solicitudes screen, so a merchant sitting on Inicio has no way to know an order is waiting. This change lifts it to an app-wide value and shows it as a badge in two places.

## What Changes

- Replace `HomePlaceholderScreen` with a real **`features:home`** module rendering the Figma design (`Qhatia-DS`, node `1954-6799`).
- **Gradient header** (DS `headerGradient`): `Hola <nombre>` taken from the real session's `user.fullName` (first name only), the `Última actualización a las HH:mm` freshness line, and a **bell icon**.
- **Bell** navigates to the Solicitudes tab and carries a **classic count badge** when there are pending orders.
- **Resumen card** (dark navy, `CardRadius`): a four-way period selector (**Mensual · Semanal · Diario · Por hora**, `Semanal` selected on open), the period label, the headline amount (`S/ 5,770`), the delta line against the previous period in success green, a **bar chart** whose tallest bar is highlighted in brand accent with its value label above each bar, and an **insight strip** with a star glyph summarising the best bucket. Switching the period swaps the whole card contents.
- **Sugerencias para vender más** section with two horizontally-scrollable card rows:
  - **Productos no encontrados** — search-count badge (`9 Búsquedas`), product name, `Agregar` button.
  - **Productos para reponer** — product image, name, a `Solo queda N` warning chip, `Reabastecer` button.
  - The design's third row, **Combos recomendados**, is `hidden` in Figma and is **out of scope**.
- **Real data** (`api-contract.md` §17, Fase 17): `StoreInsightsRepository` is a domain port implemented in `core:data` against `GET /api/v1/dashboard/sales`, `GET /api/v1/dashboard/restock` and `GET /api/v1/insights/unmet-searches` (vo-api, same nginx host). The screen was first built against a canned source while the backend did not exist; that mock has been replaced.
- **Pending-order count becomes app-wide**: `OrderRepository` gains an observable, cached pending count kept fresh by every order query and by every confirm/reject. It feeds (a) the Home bell badge, (b) a new badge on the **Solicitudes bottom-nav tab**, and (c) the existing `Pendientes (N)` filter label, which now reads from the same source instead of its own local copy.
- **`core:ui`** gains a `QhathiaBadge` count bubble and a `badgeCount` parameter on `QhathiaBottomBarItem`.

**Decided with the requester, and deliberately not in this change:**
- The `Agregar`, `Reabastecer` and `Ver más` actions **render per the design but are inert** — there is no product-creation-from-search flow, no restock flow and no detailed-report screen for `Ver más` yet. Only the bell navigates. (Restock items now carry a real `productId`, so wiring `Reabastecer` to product detail is a cheap follow-up.)
- The shell's start destination **becomes Inicio**: entering the app — from login or a restored session — lands on the summary with the Inicio tab selected, instead of on Inventario.
- The "Productos no encontrados" count shows **distinct people** (`personCount`, labelled `Personas`) rather than raw searches — the contract's primary metric. This deviates from the Figma label `Búsquedas`.

## Capabilities

### New Capabilities
- `home-summary`: the Inicio tab — greeting header with the freshness line and notification bell, the period-switchable sales summary card with its bar chart and insight, and the "sugerencias para vender más" rows, served from the dashboard and insights endpoints.

### Modified Capabilities
- `order-requests`: the pending-order count is promoted from screen-local state to an app-wide observable value, and is surfaced outside the Solicitudes screen — as a badge on the Solicitudes bottom-nav tab and on the Home bell — while the existing `Pendientes (N)` label keeps its behaviour by reading the same source.

## Impact

- **New module:** `features:home` (Android library, Compose + Hilt) — `HomeScreen`, `HomeContent`, `HomeViewModel`, MVI contract, `homeGraph()`. Registered in `settings.gradle.kts`, consumed by `:app`.
- **`core:domain`:** `SalesSummary`, `SalesPeriod`, `SalesBucket`, `MissingProductSuggestion`, `RestockSuggestion` models; `StoreInsightsRepository` port; `GetSalesSummaryUseCase`, `GetSalesSuggestionsUseCase`. `OrderRepository` gains `pendingOrderCount: Flow<Int>` and `refreshPendingOrderCount()`; new `ObservePendingOrderCountUseCase` and `RefreshPendingOrderCountUseCase`.
- **`core:network`:** `DashboardApiService`, `InsightsApiService` and their DTOs.
- **`core:data`:** `StoreInsightsRepositoryImpl` and `StoreInsightsMappers` bound in `DataModule`; `OrderRepositoryImpl` becomes `@Singleton` and holds the pending-count cache, updated by `getOrders`, `confirmOrder` and `rejectOrder`.
- **`core:ui`:** `QhathiaBadge`; `badgeCount` on `QhathiaBottomBarItem`; a `summaryCard` surface token pair for the dark navy card, plus the bar-chart fills. New `ic_bell`, `ic_sparks`, `ic_search`, `ic_alert` and `ic_star` drawables.
- **`core:testing`:** `FakeStoreInsightsRepository`; `FakeOrderRepository` extended with the pending-count stream.
- **`:app`:** `HomePlaceholderScreen` is deleted; `MainShell` swaps it for `homeGraph()`, reads the pending count for the bottom-bar badge, and gains a `MainShellViewModel`. `:app` depends on the new feature module.
- **APIs consumed:** `GET /api/v1/dashboard/sales`, `GET /api/v1/dashboard/restock`, `GET /api/v1/insights/unmet-searches` (§17). The badge reuses `GET /api/v1/orders?status=PENDING&size=1` (§9), already called by the Solicitudes screen.
