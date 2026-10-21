## Context

Inicio (`:features:home`) already renders a sales summary card from `StoreInsightsRepository` → `GET /dashboard/sales`, with its `Ver más` action inert. The ClickUp task ([Estadisticas de comercio](https://app.clickup.com/t/86e38fgh6)) asks for the detail behind it, designed in Figma `Qhatia-DS` nodes `2238-30891` (Resumen), `2238-31151` (Ranking), `2238-31411` (Actividad) and `2238-31712` (filters expanded).

What the design reads from, per `docs/android/api-contract.md` §17 (all OWNER/SELLER/ADMIN, `granularity` = `MONTHLY|WEEKLY|DAILY|HOURLY`):

| Tab | Endpoint | Category filter |
|---|---|---|
| Resumen | `GET /dashboard/sales?granularity&buckets&categoryId` — `current` carries `salesCount`, `averageTicket`, `unitsSold` | yes |
| Ranking | `GET /dashboard/ranking?granularity&categoryId` — `topSellers`, `needsAttention` | yes |
| Actividad | `GET /dashboard/activity?granularity` — `uniqueSessions`, `productViews` + deltas | **no** |
| Filter options | `GET /api/v1/categories` (already wrapped by `GetCategoriesUseCase`) | — |

Constraints that shape the design:
- Feature modules may depend only on `:core:domain`, `:core:ui`, `:core:common` — never on each other (`android-frontend-standards.md`, Dependency Graph Rules). The dark summary card, bar chart and insight strip the Resumen tab needs are today `internal` to `:features:home`.
- `:core:ui` has no dependency on `:core:domain` (Compose + Material only), and the requester chose to keep it that way: shared components take UI models, not domain types.
- `MainShell` already hides the bottom bar on any destination that is not one of the four tabs, which is exactly the "no navigation bar" requirement.

Decided with the requester: new module `:features:sales-statistics`; shared pieces extracted to `:core:ui` (not duplicated); "Lo que necesita atención" rows show the immobilized amount; the category selector is hidden on Actividad; share is inert.

## Goals / Non-Goals

**Goals:**
- Ship "Resumen de ventas" as `:features:sales-statistics`, faithful to the four Figma nodes in light and dark.
- One implementation of the summary card, chart and insight, used by both Inicio and the new screen.
- Each tab loads only when shown and only again when its own inputs changed.
- Inicio's `Ver más` opens the screen on the period Inicio is showing.

**Non-Goals:**
- Share action (icon renders, does nothing).
- Navigating to earlier periods from the date label — the API has no period offset; the label is informational.
- Opening a product from a ranking row.
- Moving Inicio's in-card period tabs onto the new segmented control — Inicio keeps its own tabs; only card, chart and insight are shared.
- Any backend change.

## Decisions

### Decision 1 — Module and package layout

New Gradle module `:features:sales-statistics` (feature template: `android.library` + `kotlin.compose` + `ksp` + `hilt`, `namespace = com.qhathia.mobile.staff.feature.salesstatistics`, `compileSdk 36`, `minSdk 26`), depending on `:core:domain`, `:core:ui`, `:core:common`, plus Coil for ranking thumbnails — the same set `:features:home` uses.

```
features/sales-statistics/src/main/java/com/qhathia/mobile/staff/feature/salesstatistics/
├── model/       SalesStatisticsUiState.kt · SalesStatisticsUiEvent.kt · SalesStatisticsUiEffect.kt
│                StatisticsTab.kt · TabContent.kt
├── ui/          SalesStatisticsScreen.kt (stateful) · SalesStatisticsContent.kt (stateless)
│                StatisticsTopBar.kt · PeriodRangeLabel.kt · FiltersPanel.kt
│                SummaryTab.kt · RankingTab.kt · RankingProductRow.kt · ActivityTab.kt
│                StatisticsFormatters.kt · SalesStatisticsPreviewData.kt
└── navigation/  SalesStatisticsNavigation.kt
```

Dependency flow: `:app → :features:sales-statistics → :core:{domain,ui,common}`; `:core:data → :core:{domain,network}`.

### Decision 2 — What moves to `:core:ui`, and how it stays domain-free

Into `core/ui/.../component/sales/`:

```kotlin
enum class SalesPeriodUi { MONTHLY, WEEKLY, DAILY, HOURLY }   // mirror of the domain enum

@Immutable
data class SalesSummaryCardModel(
    val period: SalesPeriodUi,
    val total: BigDecimal,
    val deltaPercent: Double?,
    val buckets: ImmutableList<SalesBucketModel>,        // label + amount
    val highlightedBucketIndex: Int,
    val highlightedDeltaPercent: Double?,
)

@Composable fun QhathiaSalesSummaryCard(model: SalesSummaryCardModel?, modifier, header: @Composable ColumnScope.() -> Unit = {})
@Composable fun QhathiaSalesBarChart(...)       // unchanged behaviour
@Composable fun QhathiaSalesInsightStrip(...)   // unchanged behaviour
@Composable fun QhathiaKpiTile(icon: ImageVector, label, value)   // Material icons already bundled in :core:ui
@Composable fun <T> QhathiaSegmentedControl(options, selected, label, onSelect, colors)  // tabs + period filter
```

The card resolves its own strings (period label, "vs. semana anterior", insight variants) and formatting (`S/ 1,690`, `+24%`) from `:core:ui` resources and helpers — moved as-is from `HomeFormatters` / `features/home/res/values/strings.xml`. The `header` slot lets Inicio keep its in-card `PeriodTabs`; the statistics screen passes nothing.

Each feature maps `SalesSummary → SalesSummaryCardModel` with a ~10-line mapper. `:core:ui` gains `kotlinx-collections-immutable`.

*Alternatives rejected:*
- *`:core:ui` depends on `:core:domain`* — less code (components would take `SalesSummary` directly), but the requester chose to keep the design system free of domain types.
- *Duplicate the components in the new feature* — two copies of the chart and insight rules (minimum bar height, empty treatment, no-comparison sentence) that would drift.
- *Put the screen inside `:features:home`* — reuses everything with no refactor but breaks "every new feature is its own module".

`:features:home` then deletes `SalesSummaryCard`, `SalesBarChart`, `SalesInsightStrip`, the percent/soles helpers and their strings, and renders `QhathiaSalesSummaryCard(model, header = { PeriodTabs(...) })`. `HomeViewModel`, `HomeUiState` and their tests do not change; Inicio's previews are the regression check.

### Decision 3 — Domain and data extensions

```kotlin
// SalesSummary gains
val periodStart: Instant?, val periodEnd: Instant?   // bounds of the period in progress
val kpis: SalesKpis                                   // of the period in progress
data class SalesKpis(val salesCount: Int, val averageTicket: BigDecimal, val unitsSold: Int)

data class SalesRanking(
    val periodStart: Instant?, val periodEnd: Instant?,
    val topSellersRevenue: BigDecimal, val topSellers: List<TopSellingProduct>,        // id, name, imageUrl, revenue, unitsSold
    val immobilizedTotal: BigDecimal, val needsAttention: List<StalledProduct>,       // id, name, imageUrl, stock, price, immobilizedAmount
)

data class StoreActivity(
    val periodStart: Instant?, val periodEnd: Instant?,
    val uniqueSessions: Int, val uniqueSessionsDeltaPercent: Double?,
    val productViews: Int, val productViewsDeltaPercent: Double?,
)

interface StoreInsightsRepository {
    suspend fun getSalesSummary(period: SalesPeriod, categoryId: String? = null): Result<SalesSummary>
    suspend fun getSuggestions(): Result<StoreSuggestions>
    suspend fun getSalesRanking(period: SalesPeriod, categoryId: String?): Result<SalesRanking>
    suspend fun getStoreActivity(period: SalesPeriod): Result<StoreActivity>
}
```

New use cases `GetSalesRankingUseCase`, `GetStoreActivityUseCase`; `GetSalesSummaryUseCase` gains `categoryId: String? = null` so Inicio's call site is unchanged.

`DashboardApiService` gains `categoryId` on `getSales`, plus `getRanking(granularity, categoryId)` and `getActivity(granularity)` with `SalesRankingDto` / `StoreActivityDto`. `SalesPeriodDto` already declares `salesCount`, `averageTicket`, `unitsSold`, `periodStart`, `periodEnd`.

Mapping rules (in `StoreInsightsMappers`):
- ISO timestamps parse to `Instant?`; an unparseable bound is `null` and the label is simply not rendered.
- Deltas keep `null` and drop non-finite values, as for the summary.
- `needsAttention` is sorted by `immobilizedAmount` descending, so "first row" means "most money tied up" regardless of backend order; both lists are de-duplicated by product id (they feed keyed rows).
- `getSalesSummary` keeps `buckets=4`; `categoryId` is sent only when non-null.

### Decision 4 — MVI contract and loading strategy

```kotlin
enum class StatisticsTab { SUMMARY, RANKING, ACTIVITY }

@Immutable data class TabContent<T>(val data: T? = null, val isLoading: Boolean = false, val isError: Boolean = false)

@Immutable data class SalesStatisticsUiState(
    val selectedTab: StatisticsTab = StatisticsTab.SUMMARY,
    val period: SalesPeriod = SalesPeriod.WEEKLY,
    val categories: ImmutableList<Category> = persistentListOf(),
    val selectedCategoryId: String? = null,            // null = "Todos"
    val isFiltersExpanded: Boolean = false,
    val summary: TabContent<SalesSummary> = TabContent(),
    val ranking: TabContent<SalesRanking> = TabContent(),
    val activity: TabContent<StoreActivity> = TabContent(),
)

sealed interface SalesStatisticsUiEvent {
    data class SelectTab(val tab: StatisticsTab)
    data class SelectPeriod(val period: SalesPeriod)
    data class SelectCategory(val categoryId: String?)
    data object ToggleFilters
    data object Retry
    data object Back
    data object Share                                   // no-op today
}
sealed interface SalesStatisticsUiEffect { data object NavigateBack }
```

A single cancellable load job keyed on the active tab and its query, where `query` is `period + categoryId` for Resumen and Ranking and `period` alone for Actividad. The ViewModel remembers, per tab, the query its data was loaded for:

- **Switching tab** loads that tab only if its data belongs to a different query — flicking Resumen ↔ Ranking does not refetch.
- **Changing a filter** clears the active tab's data and loads it (stale figures under a new filter would read as the new filter's figures). Other tabs are marked stale and reload when shown.
- **Changing the category while on Actividad** reloads nothing (its query did not change).
- **Retry after a failure on the same query** keeps any data already on screen and sets `isError` — the stale-beats-blank rule Inicio uses.
- Starting a load cancels the one in flight, so a slow response for a query the merchant already left never lands; the abandoned tab stops its spinner and loads again when shown. Because the network layer turns cancellation into a failed result, the job checks it is still active before publishing.

The initial period comes from the navigation argument (`SavedStateHandle`); a missing or unknown value falls back to `WEEKLY`. Categories load once at start through `GetCategoriesUseCase`; a failure leaves only `Todos` and raises no error (the filter is optional, the figures are not).

*Alternative rejected:* loading all three tabs up front — three requests to show one tab, and two of them repeated on every filter change.

### Decision 5 — Date label

`PeriodRangeLabel` renders the bounds of the active tab's loaded data, in `America/Lima` (the zone the backend cuts on) and `es-PE`:

| Period | Example |
|---|---|
| WEEKLY | `24 – 30 de marzo` · across months `30 de marzo – 5 de abril` · across years with years |
| MONTHLY | `marzo 2026` |
| DAILY | `30 de marzo` |
| HOURLY | `30 de marzo, 14:00 – 15:00` (start + 1 h) |

The contract does not say whether `periodEnd` is exclusive. The formatter uses `periodEnd − 1 ns` for the last day, which is correct for both an exclusive midnight and an inclusive `23:59:59.999`. No bounds → no label.

### Decision 6 — Navigation

`SalesStatisticsNavigation.kt` exposes `SALES_STATISTICS_ROUTE = "sales-statistics/{period}"`, `fun salesStatisticsRoute(period: SalesPeriod)` and `fun NavGraphBuilder.salesStatisticsGraph(onBack: () -> Unit)`.

- `:features:home`: `Ver más` becomes enabled and emits `HomeUiEffect.NavigateToSalesStatistics(period)`; `homeGraph` gains `onNavigateToSalesStatistics: (SalesPeriod) -> Unit`.
- `:app` `MainShell`: registers `salesStatisticsGraph(onBack = { navController.popBackStack() })` and fulfils Inicio's callback with a plain `navigate(salesStatisticsRoute(period))` — a push, not a tab switch, so back returns to Inicio. The bottom bar hides itself because the route is not a top-level destination.
- The top-bar back chevron emits `NavigateBack`; system back pops the same entry.

### Decision 7 — Filters panel and tabs UI

- `Filtros` is a card header with a chevron; collapsed by default, expanded state kept in `UiState` so it survives recomposition and tab switches.
- Expanded: `QhathiaSegmentedControl` for the period, then — except on Actividad — `Selecciona la categoría de producto` with an outlined field anchoring a `DropdownMenu` listing `Todos` and the categories (plain `DropdownMenu` rather than `ExposedDropdownMenuBox`, whose anchor API is still shifting between Material 3 releases).
- Resumen / Ranking / Actividad use the same segmented control on the neutral track shown in the design.

### Decision 8 — Ranking and activity presentation

- **Lo que más vende**: summary box in success colours with the increase glyph, `S/ <topSellersRevenue>` and `Generados por tus N productos principales` (plural-aware, N = rows). Rows: rank badge (success container, 1-based), 48 dp thumbnail, name (2 lines), `N vendidos` (plural), revenue right-aligned.
- **Lo que necesita atención**: summary box in error colours with the decrease glyph, `S/ <immobilizedTotal>` and `Generados por los N productos no vendidos`. Rows: alert glyph — error colour on the first row (most immobilized), warning colour on the rest — thumbnail, name, `N unidades sin vender` (plural), immobilized amount right-aligned.
- An empty list shows a short message inside its card instead of rows (`Aún no hay ventas en este periodo.` / `Todo tu inventario se movió en este periodo.`).
- **Actividad en la tablet**: two stats split by a divider, each with glyph, number, label and a delta pill (`+ 12% vs semana pasada`, success; negative in error with the decrease glyph; no pill when the delta is null). Zeros render as zeros: per the contract, a tablet that emits no events yields `0`, not an error.
- Rows sit inside the scrolling `LazyColumn` as plain `Column`s — the lists are short (the design shows three) and they never scroll independently.
- Thumbnails via Coil with explicit size and the neutral placeholder already used on Inicio.
- Accessibility: tabs and period pills carry `selected` semantics; each ranking row merges into one node ("1. Taladro percutor Bosch GSB, 34 vendidos, S/ 1,870"); share and back are `IconButton`s with content descriptions.

## Risks / Trade-offs

- **[Refactoring Inicio to reach shared components]** → Regressions in a shipped screen. Mitigation: the moved components keep their behaviour and parameters; `HomeViewModel` and its tests are untouched; Inicio's light/dark previews and a manual pass guard the visuals; the refactor is its own task group, verified before the new feature is built on it.
- **[`periodEnd` semantics are unspecified]** → An off-by-one day in the label. Mitigation: `periodEnd − 1 ns` is right for both readings; verify against the live API during the manual pass.
- **[Ranking list length is not bounded by the contract]** → A store with many stalled products produces a long card. Mitigation: rows are cheap non-lazy items inside the screen's `LazyColumn`; if the backend returns more than the design implies, a display cap is a presentation change.
- **[Design shows unit price where the data is immobilized money]** → The screen will not match the Figma figure for that column. Accepted: the requester chose the immobilized amount so rows add up to the card total.
- **[Activity figures depend on kiosk instrumentation]** → Stores whose tablet does not emit events see `0` everywhere. Mitigation: render zeros normally, not as an error — the contract calls this expected.
- **[`SalesPeriodUi` mirrors a domain enum]** → Two enums to keep in step. Mitigation: exhaustive `when` in each mapper, so a new period fails to compile rather than silently falling through.

## Open Questions

- Is `periodEnd` exclusive? (Handled either way; confirm to simplify the formatter.)
- Does `GET /dashboard/ranking` cap its lists? The design implies three per card.
- How does the backend label hourly buckets? (Affects the chart's bar labels on the Resumen tab, as on Inicio.)
