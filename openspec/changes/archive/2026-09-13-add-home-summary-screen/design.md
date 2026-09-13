## Context

The staff app is a multi-module Clean Architecture + MVI codebase (`docs/android/android-frontend-standards.md`). `settings.gradle.kts` already declares `:core:{common,network,domain,data,ui,testing,notifications}` and `:features:{auth,product-inventory,product-register,product-detail,device-linking,order-requests}`. Inicio is the only top-level destination still implemented as a composable inside `:app` (`app/.../ui/home/HomePlaceholderScreen.kt`), which violates the module rule — the real screen must be a feature module.

Two things make this change unusual:

1. **The data arrived mid-change.** The screen was first built against a canned source because the backend exposed no sales-summary, "productos no encontrados" or "productos para reponer" endpoint. Fase 17 (`store-home-dashboard`, `docs/android/api-contract.md` §17) added them, and the shape they ship differs from what the mock assumed: the headline is the period in progress rather than the sum of the chart, the variation is a nullable decimal, and the suggestions come from two services. The port absorbed the source swap; the model did not survive unchanged.
2. **One value on the screen is real.** The pending-order count already exists — `OrderRequestsViewModel` derives it from a `size=1`, `status=PENDING` query and keeps it in its own `OrderRequestsUiState.pendingCount`. Home's bell and the Solicitudes bottom-nav tab both need the same number, and the bottom bar lives in `:app`, outside any feature module.

Existing DS material this screen reuses: `QhathiaColors.headerGradient` / `onHeaderGradient` (already used by `OrderRequestsHeader`), `QhathiaColors.success`, `Dimens`, `QhathiaTypography` (Urbanist variable font), `QhathiaBottomBar` / `QhathiaBottomBarItem`.

Figma source: `Qhatia-DS` file `uLhEDY8ZMxdNiQZU4uYA1V`, node `1954-6799`. Tokens read from the node: `--primitive-blue-800 #0b1b28` and `--primitive-blue-700 #10283c` (summary card and bar fills), `--primitive-blue-500 #1c4466`, `--primitive-blue-50 #ebf3f9`, `--color-brand-accent #ff4500`, `--color-status-success #22c55e`, `--primitive-neutral-{0,100,200}`, `Urbanist/H1 32/40 Bold`, `H4 16/20 SemiBold`, `Body/MD 14/20`, `Body/SM 12/18`, `Label/{SM,MD,LG}`, `Caption 10/14`.

## Goals / Non-Goals

**Goals:**
- Ship the Inicio summary screen as `:features:home`, pixel-faithful to node `1954-6799` in light and dark.
- Keep the data source strictly behind a domain port, so the presentation layer never sees DTOs, services or which backend answered.
- Make the pending-order count a single app-wide source of truth feeding three consumers (Home bell, bottom-nav tab, Solicitudes filter label) that can never disagree.
- Keep `:app` thin: it wires `homeGraph()` and reads the badge count; it holds no summary logic.

**Non-Goals:**
- No use of `GET /dashboard/ranking` or `GET /dashboard/activity` — those feed the "Resumen de ventas" detail behind `Ver más`, which is not built here.
- No `Combos recomendados` row (hidden in the Figma node).
- No navigation from `Agregar`, `Reabastecer` or `Ver más` — they render and do nothing (requester's decision; there is no create-from-search flow, no restock flow and no report screen).
- No change to how notification taps route: a push still switches to Solicitudes, now from Inicio as the start destination.
- No push-driven live refresh of the badge; it refreshes on the events listed under **Decision 3**.

## Decisions

### Decision 1 — API behind a domain port, implemented in `core:data`

`core:domain` declares the port and the models; `core:data` provides `StoreInsightsRepositoryImpl` over two `core:network` services — `DashboardApiService` (biz: `/dashboard/sales`, `/dashboard/restock`) and `InsightsApiService` (vo: `/insights/unmet-searches`). Both services run on the existing `@AuthenticatedClient`: vo-api sits behind the same nginx host as biz, so there is no second base URL.

```kotlin
// core:domain
enum class SalesPeriod { MONTHLY, WEEKLY, DAILY, HOURLY }

data class SalesBucket(val label: String, val amount: BigDecimal)

data class SalesSummary(
    val period: SalesPeriod,
    val total: BigDecimal,                 // revenue of the period in progress (`current.revenue`)
    val deltaPercent: Double?,             // vs the previous period; null = no base to compare
    val buckets: List<SalesBucket>,        // 4 buckets, oldest → newest, the last is in progress
    val highlightedBucketIndex: Int,       // the tallest bucket, -1 when nothing sold
    val highlightedDeltaPercent: Double?,  // tallest bucket vs the one charted before it
    val generatedAt: Instant,              // client time of the successful fetch
)

interface StoreInsightsRepository {
    suspend fun getSalesSummary(period: SalesPeriod): Result<SalesSummary>
    suspend fun getSuggestions(): Result<StoreSuggestions>
}
```

*Why a port?* The screen was built before the endpoints existed. With the port, `HomeViewModel` and every test written against `FakeStoreInsightsRepository` survived the swap from canned data to the API untouched.

*Why `Result<T>`?* Every failure — HTTP status, transport, either suggestion source — reaches the ViewModel as one failed `Result`, which feeds the existing "stale data plus an error" rule.

`highlightedBucketIndex` and `highlightedDeltaPercent` are computed in the mapper, not in the composable: which bucket is "the best one" and how it compares with the bucket before it is a data statement (the insight strip quotes it in prose), not a rendering detail. The backend does not send the bucket-to-bucket variation, so the mapper derives it from the two amounts; it is null when the best bucket is the first charted or the preceding one sold nothing.

*Percentages stay `Double?`.* The backend sends `24.3`; rounding to `24%` is a presentation decision and lives in `HomeFormatters`, which decides the sign before rounding so `-0.4` never renders as `+0%`. Null is kept end to end because the contract uses it to mean "no base" — any number substituted for it would print a variation that did not happen.

*Four buckets for every period.* The chart is designed for four bars at 393dp; the backend defaults (6/4/7/12) would not fit. The request always sends `buckets=4`.

*Amounts:* `BigDecimal` matches how money is already modelled in `core:domain` (`Order.total`). Formatting to `S/ 5,770` is a presentation concern and lives in the feature module.

### Decision 2 — How the contract maps onto the screen

- **Period label** (`Esta semana`, `Hoy`…) is a string resource keyed on `SalesPeriod`, not data: the backend only sends `current.label` (`Sem 13`), which names the bucket, not the period.
- **Empty treatment** only when no bucket in the window sold. The period in progress is routinely `0` early in the week while the weeks before it sold; hiding the chart then would look like an outage.
- **Suggestions** issue restock and unmet-searches concurrently and fail together: one `Result`, no ViewModel change, and a half-refreshed pair of rows is never shown.
- **Productos no encontrados**: `personCount` (distinct people — the contract's primary metric, labelled `Personas`), the most frequent raw search as the name (capitalised, falling back to the canonical `term`), and `term` as the stable `LazyRow` key. Blank terms are dropped and repeated ids de-duplicated, because a duplicate key crashes `LazyRow`.
- **Productos para reponer**: `productId` as id, `stock` as the remaining count, a blank `imageUrl` treated as none.

### Decision 3 — The pending count moves into `OrderRepositoryImpl` as a cached, observable value

```kotlin
// core:domain — OrderRepository gains
val pendingOrderCount: Flow<Int>
suspend fun refreshPendingOrderCount(): Result<Int>
```

`OrderRepositoryImpl` becomes `@Singleton` and holds a `MutableStateFlow<Int>`. It is updated by:
- `refreshPendingOrderCount()` — an explicit `status=PENDING&size=1` query,
- `getOrders(...)` when the query *was* the pending query (the Solicitudes screen already issues it, so its refresh feeds the badge for free),
- `confirmOrder` / `rejectOrder` on success — the count is re-fetched, so acting on an order updates both badges immediately.

Consumers: `ObservePendingOrderCountUseCase` (a `Flow<Int>`) and `RefreshPendingOrderCountUseCase`. `MainShellViewModel` in `:app` collects the first for the bottom-bar badge and calls the second on entering the shell; `HomeViewModel` collects the first for the bell badge and refreshes it with the summary; `OrderRequestsViewModel` drops its private `pendingCount` bookkeeping and reads the same flow, so `Pendientes (N)` and the two badges are the same number by construction.

**Alternatives rejected:**
- *Each consumer fetches its own count.* Three `size=1` requests and three values that disagree for as long as they are out of step — the badge saying `2` while the filter says `1` is exactly the bug this design exists to prevent.
- *A separate `PendingOrderCountStore` singleton in `core:data`.* One more type that must be kept in sync with the repository that already owns order state. The repository *is* the place order counts change.
- *Passing the count down into `homeGraph(count)`.* `NavHost` remembers its graph and does not re-run the builder when a captured value changes, so the badge would go stale. Home reads the flow itself.

### Decision 4 — Module and package layout

New Gradle module `:features:home` (feature template from the standards — Android library + Compose + Hilt + KSP2, `namespace = com.qhathia.mobile.staff.feature.home`, `minSdk 26`, `compileSdk 36`):

```
features/home/src/main/java/com/qhathia/mobile/staff/feature/home/
├── model/    HomeUiState.kt · HomeUiEvent.kt · HomeUiEffect.kt
├── ui/       HomeScreen.kt (stateful) · HomeContent.kt (stateless)
│             HomeHeader.kt · SalesSummaryCard.kt · SalesBarChart.kt
│             PeriodTabs.kt · SalesInsightStrip.kt
│             MissingProductCard.kt · RestockProductCard.kt
│             SuggestionSection.kt · HomePreviewData.kt
└── navigation/HomeNavigation.kt   (HOME_ROUTE + homeGraph())
```

`HOME_ROUTE` moves out of `MainShell`'s `private const val` into `HomeNavigation.kt`, matching `ORDER_REQUESTS_ROUTE` and every other feature. `:app` deletes `ui/home/HomePlaceholderScreen.kt` and depends on `:features:home`.

The feature depends only on `:core:domain`, `:core:ui`, `:core:common` — no feature-to-feature edge, per the dependency graph rules.

### Decision 5 — Which pieces belong to `core:ui`

Into `core:ui` (shared, reusable, no business meaning):
- `QhathiaBadge` — the classic count bubble (brand accent circle, white `Caption` digits, `99+` cap above 99, `minSize 16.dp`).
- `badgeCount: Int?` parameter on `QhathiaBottomBarItem`, drawn as an overlay anchored to the glyph's top-end corner. Null (the default) renders exactly what renders today, so the other three tabs are untouched.
- Tokens for the dark navy summary surface and its bar fills, added to `QhathiaColors` following the existing light/dark pattern.
- Vector drawables: `ic_bell`, `ic_sparks`, `ic_search`, `ic_alert`, `ic_star`.

Staying in the feature (single-use, carries screen meaning): the summary card, the period tabs, the bar chart, the insight strip and both suggestion card types.

### Decision 6 — The bar chart is a Compose layout, not a canvas

Four bars, a value label above each and a category label below. `Row` of `Column`s, each bar a `Box` with `weight` proportional to `amount / maxAmount` inside a fixed-height slot, rounded `SpaceSm` on top. Reasons: text labels stay real `Text` nodes (so they scale with font size and are readable by TalkBack and by Compose UI tests), and no `Canvas` drawing code has to be maintained for four rectangles. The chart's content description announces the period total and the highlighted bucket, so the whole chart is one meaningful node rather than four unlabeled ones.

The tallest bucket renders in `brand accent` with an accent value label; the others use the navy bar fill — exactly the Figma treatment, driven by `highlightedBucketIndex`.

### Decision 7 — The greeting name comes from the real session

`Hola Maria` is not mocked: `ObserveSessionUseCase` already exposes `Session.user.fullName`. `HomeViewModel` takes the first whitespace-separated token. A null session (a race during logout) renders the greeting without a name (`Hola`) rather than a placeholder name — the screen is about to be torn down anyway.

`Última actualización a las HH:mm` is the local wall-clock time of the last successful load, formatted with `DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)` — the same helper `OrderRequestsHeader` already uses. It renders only once a load has succeeded.

### Decision 8 — MVI contract

```kotlin
@Immutable
data class HomeUiState(
    val merchantFirstName: String? = null,
    val selectedPeriod: SalesPeriod = SalesPeriod.WEEKLY,
    val summary: SalesSummary? = null,
    val missingProducts: ImmutableList<MissingProductSuggestion> = persistentListOf(),
    val restockProducts: ImmutableList<RestockSuggestion> = persistentListOf(),
    val pendingOrderCount: Int = 0,
    val isLoading: Boolean = false,
    val isError: Boolean = false,
    val lastUpdatedAt: Instant? = null,
)

sealed interface HomeUiEvent {
    data object Refresh : HomeUiEvent
    data class SelectPeriod(val period: SalesPeriod) : HomeUiEvent
    data object OpenNotifications : HomeUiEvent
}

sealed interface HomeUiEffect {
    data object NavigateToOrderRequests : HomeUiEffect
}
```

A failed load keeps the previously loaded summary and timestamp on screen and sets `isError` — the same "stale data beats a blank screen" rule `OrderRequestsUiState` already documents. `Semanal` is the default period, matching the Figma selection.

Navigation is a `UiEffect` collected by `HomeScreen` and raised to `homeGraph(onNavigateToOrderRequests = …)`, which `MainShell` fulfils with the same tab-switch navigation options the bottom bar uses (`popUpTo(startDestination) { saveState }`, `launchSingleTop`, `restoreState`) — tapping the bell must land on the Solicitudes tab exactly as tapping the tab does, not push a second copy on top of Inicio.

### Decision 9 — Accessibility and DS conformance

- The bell is an `IconButton` (48dp target) with content description `Notificaciones`; when a badge is present the description includes the count, so TalkBack announces "Notificaciones, 2 pendientes" as one node instead of reading a bare number.
- The badge on the bottom-nav tab is `clearAndSetSemantics`-merged into the tab item, for the same reason.
- Period tabs carry `selected` semantics, matching `QhathiaBottomBarItem`.
- Both horizontal card rows are `LazyRow`s with stable `key`s.
- The screen scrolls vertically (`LazyColumn`) — at 393×1142 the design already exceeds a typical viewport, and it must survive large font scales.
- `@Preview` in light and dark for `HomeContent` and each non-trivial component, per the Definition of Done.

## Risks / Trade-offs

- **Kiosk activity not instrumented** → `unmet-searches` counts people by `sessionId`, which searches recorded before Fase 17 lack, so early `personCount`s are underestimated. Nothing to do client-side; noted so the figures are not mistaken for a bug.
- **`OrderRepositoryImpl` becomes stateful and `@Singleton`** → It currently has no state, so a stale count could outlive a session (e.g. after logout, before the next login). Mitigated by resetting the cached count to `0` when `refreshPendingOrderCount()` fails with an auth error, and by the shell refreshing on every entry — a logged-out shell is not composed.
- **Three consumers, one flow, mock-free** → If `OrderRequestsViewModel` is refactored to read the shared flow but its own load path stops populating it, the badge freezes. Mitigated by making `getOrders` itself update the cache when the query is the pending one, so the count follows the data rather than the caller.
- **Bucket labels are backend text** → `Sem 13`, and whatever the hourly granularity names its buckets, are rendered verbatim under the bars and in the insight sentence. A label longer than a bar's width is clipped to one line; if the backend's hourly labels prove too long, shortening them is a mapper change.
- **Bar chart with `weight`-proportional bars** → A near-zero bucket collapses to an invisible sliver. Mitigated with a minimum bar height so every bucket stays visible and tappable-looking, and a zero-total period renders the empty treatment instead of four zero-height bars.
- **Urbanist at `H1 32/40` inside a fixed-height card** → Large system font scales can overflow the headline. Mitigated by letting the card height be intrinsic rather than fixed, and capping the headline at one line with `autoSize`-style downscaling off — the amount wraps the card, not the other way round.
