## 1. Create the `:features:sales-statistics` module

- [x] 1.1 Create the module directories: `features/sales-statistics/src/main/java/com/qhathia/mobile/staff/feature/salesstatistics/{model,ui,navigation}`, `features/sales-statistics/src/main/res/values`, `features/sales-statistics/src/test/java/com/qhathia/mobile/staff/feature/salesstatistics/ui`
- [x] 1.2 Write `features/sales-statistics/build.gradle.kts` from the Feature-module template in `docs/android/android-frontend-standards.md` (`android.library` + `kotlin.compose` + `ksp` + `hilt`, `namespace = "com.qhathia.mobile.staff.feature.salesstatistics"`, `compileSdk 36`, `minSdk 26`, `buildFeatures.compose = true`, JUnit Platform); dependencies mirrored from `features/home/build.gradle.kts` (`:core:domain`, `:core:ui`, `:core:common`, Compose BOM, lifecycle, navigation-compose, `kotlinx.immutable`, Coil, Hilt + `ksp(kotlin.metadata.jvm)`, JUnit5/MockK/Turbine test set)
- [x] 1.3 Register `include(":features:sales-statistics")` in `settings.gradle.kts`
- [x] 1.4 Add the minimal `features/sales-statistics/src/main/AndroidManifest.xml` (`<manifest />`)
- [x] 1.5 Confirm the empty module builds: `./gradlew :features:sales-statistics:assembleDebug`

## 2. Extract the shared sales-summary components to `:core:ui`

- [x] 2.1 Add `kotlinx.immutable` to `core/ui/build.gradle.kts`
- [x] 2.2 Add `SalesPeriodUi`, `SalesSummaryCardModel` and `SalesBucketModel` under `core/ui/.../component/sales/`
- [x] 2.3 Move the soles and percent formatting helpers (`toSolesLabel`, `PercentTrend`, `toSignedPercentLabel`, `toPercentMagnitudeLabel`) from `HomeFormatters` into `:core:ui`, with their tests moved from `HomeFormattersTest`
- [x] 2.4 Move the summary strings (period labels, current-period labels, delta, previous-period phrases, insight variants, empty summary, chart content description) from `features/home/src/main/res/values/strings.xml` to `core/ui/src/main/res/values/strings.xml` under a `sales_` prefix
- [x] 2.5 Move `SalesBarChart` and `SalesInsightStrip` to `:core:ui` as `QhathiaSalesBarChart` and `QhathiaSalesInsightStrip`, driven by the UI models; keep the minimum bar height, highlight and no-comparison behaviour; light/dark previews
- [x] 2.6 Move `SalesSummaryCard` to `:core:ui` as `QhathiaSalesSummaryCard(model, header slot)`; keep the empty-window treatment and the null-delta rule; light/dark previews (default, declining, empty)
- [x] 2.7 Add `QhathiaKpiTile(icon, label, value)` and `QhathiaSegmentedControl` (tab-role selectable per option, track colour parameter) with light/dark previews; add the `successContainer`, `segmentTrack` and `filterTrack` colour tokens; glyphs come from the Material icons already bundled (share, trends, bag, receipt, inventory, tablet, person, search, alert) rather than traced drawables
- [x] 2.8 Refactor `:features:home` onto the shared components: add a `SalesSummary → SalesSummaryCardModel` mapper, render `QhathiaSalesSummaryCard(model, header = { PeriodTabs(...) })`, delete the moved composables, helpers and strings
- [ ] 2.9 Verify Inicio is unchanged: `./gradlew :features:home:testDebugUnitTest :features:home:lintDebug`, and compare Inicio's previews before and after

## 3. Domain layer — `:core:domain`

- [x] 3.1 Extend `SalesSummary` with `periodStart: Instant?`, `periodEnd: Instant?` and `kpis: SalesKpis(salesCount, averageTicket, unitsSold)`
- [x] 3.2 Add `SalesRanking`, `TopSellingProduct`, `StalledProduct` and `StoreActivity` models (money as `BigDecimal`, deltas as `Double?`)
- [x] 3.3 Extend `StoreInsightsRepository`: `getSalesSummary(period, categoryId: String? = null)`, `getSalesRanking(period, categoryId)`, `getStoreActivity(period)`
- [x] 3.4 Add `categoryId: String? = null` to `GetSalesSummaryUseCase`; add `GetSalesRankingUseCase` and `GetStoreActivityUseCase`

## 4. Network layer — `:core:network`

- [x] 4.1 Add `SalesRankingDto` (`topSellers`, `needsAttention` and their item DTOs) and `StoreActivityDto` to `DashboardDtos.kt`
- [x] 4.2 `DashboardApiService`: add `categoryId` to `getSales` (sent only when non-null), add `getRanking(granularity, categoryId)` and `getActivity(granularity)`

## 5. Data layer — `:core:data`

- [x] 5.1 Extend `toSalesSummary` with the period bounds (ISO → `Instant?`, unparseable → null) and the KPIs of `current`
- [x] 5.2 Add `toSalesRanking` (money to `BigDecimal`, both lists de-duplicated by product id, `needsAttention` sorted by `immobilizedAmount` descending) and `toStoreActivity` (null and non-finite deltas → null)
- [x] 5.3 Extend `StoreInsightsRepositoryImpl` with the category on the summary and the ranking and activity calls

## 6. Test doubles — `:core:testing`

- [x] 6.1 Extend `FakeStoreInsightsRepository` with settable ranking and activity results, a failure mode, and the recorded `(period, categoryId)` of every summary, ranking and activity request
- [x] 6.2 Add fixtures for `SalesKpis`, `SalesRanking` and `StoreActivity`, and extend `salesSummary` with bounds and KPIs

## 7. Sales statistics — MVI contract and ViewModel

- [x] 7.1 Add `StatisticsTab`, `TabContent<T>`, `SalesStatisticsUiState` (`@Immutable`), `SalesStatisticsUiEvent` (`SelectTab`, `SelectPeriod`, `SelectCategory`, `ToggleFilters`, `Retry`, `Back`, `Share`) and `SalesStatisticsUiEffect` (`NavigateBack`)
- [x] 7.2 Add `SalesStatisticsViewModel`: initial period from `SavedStateHandle` (fallback `WEEKLY`), categories loaded once through `GetCategoriesUseCase` (failure → none, no error), a single cancellable load job keyed on the active tab and its query (`period + categoryId` for Resumen and Ranking, `period` for Actividad)
- [x] 7.3 Implement the load rules: a tab refetches only when its loaded query differs; a filter change clears the active tab and marks the others stale; a category change on Actividad reloads nothing; a failed retry for the same query keeps its data and sets `isError`
- [x] 7.4 `Back` emits `NavigateBack`; `Share` is a no-op

## 8. Sales statistics — UI

- [x] 8.1 `StatisticsTopBar` — `headerGradient`, back `IconButton` (`Volver`), title `Resumen de ventas`, share `IconButton` (`Compartir`) with an empty action
- [x] 8.2 `PeriodRangeLabel` + `StatisticsFormatters` — `America/Lima` / `es-PE` range formatting per period, using `periodEnd − 1 ns` for the last day; not rendered without bounds
- [x] 8.3 `FiltersPanel` — collapsible `Filtros` card, period `QhathiaSegmentedControl`, category field anchoring a `DropdownMenu` (`Todos` + categories) hidden on Actividad
- [x] 8.4 `SummaryTab` — `QhathiaSalesSummaryCard` without header plus the three `QhathiaKpiTile`s (`Num Ventas`, `Ticket prom.`, `Unidades`)
- [x] 8.5 `RankingProductRow` + `RankingTab` — `Lo que más vende` and `Lo que necesita atención` cards with their summary boxes, rows (badge / alert glyph, 48 dp Coil thumbnail, name, plural units line, amount), severity colours, empty messages, one merged semantics node per row
- [x] 8.6 `ActivityTab` — `Actividad en la tablet` card, two figures with divider and period-worded delta pills (hidden when null)
- [x] 8.7 `SalesStatisticsContent` — stateless `LazyColumn` under the top bar: date label, tab selector, filters panel, active tab content, per-tab loading and error/retry treatments
- [x] 8.8 `SalesStatisticsScreen` — stateful wrapper: `collectAsStateWithLifecycle`, effect collection, `onBack`
- [x] 8.9 `features/sales-statistics/src/main/res/values/strings.xml` — every user-facing string, with plurals for `vendidos`, `unidades sin vender` and the `N productos` summaries
- [x] 8.10 `SalesStatisticsPreviewData` plus light/dark `@Preview`s for `SalesStatisticsContent` on each tab, filters expanded, loading and error, and for `RankingProductRow`

## 9. Navigation wiring

- [x] 9.1 `SalesStatisticsNavigation.kt` — `SALES_STATISTICS_ROUTE = "sales-statistics/{period}"`, `salesStatisticsRoute(period)`, `NavGraphBuilder.salesStatisticsGraph(onBack)`
- [x] 9.2 `:features:home` — enable `Ver más`, add `HomeUiEvent.OpenSalesStatistics` and `HomeUiEffect.NavigateToSalesStatistics(period)`, collect it in `HomeScreen`, and add `onNavigateToSalesStatistics: (SalesPeriod) -> Unit` to `homeGraph`
- [x] 9.3 Add `implementation(project(":features:sales-statistics"))` to `app/build.gradle.kts`
- [x] 9.4 `MainShell` — register `salesStatisticsGraph(onBack = { navController.popBackStack() })` and fulfil Inicio's callback with `navController.navigate(salesStatisticsRoute(period))`; confirm the bottom bar hides on the new route

## 10. Tests

- [x] 10.1 `StoreInsightsMappersTest`: summary bounds and KPIs; ranking money, de-duplication and needs-attention ordering; activity null and non-finite deltas
- [x] 10.2 `StoreInsightsRepositoryImplTest`: `categoryId` sent only when present; ranking and activity map success, HTTP error and transport error
- [x] 10.3 `SalesStatisticsViewModelTest`: initial period from the argument and its fallback; Resumen loads first; a tab loads on first selection and not again for the same filters; a period change reloads the active tab with the category; a category change on Actividad issues no request; Actividad never sends a category; a failed retry keeps data; a filter change clears old data; categories failure leaves none without error; `Back` emits `NavigateBack`
- [x] 10.4 `StatisticsFormattersTest`: weekly, cross-month, cross-year, monthly, daily and hourly labels; exclusive-midnight and inclusive end bounds give the same label
- [x] 10.5 `HomeViewModelTest`: `OpenSalesStatistics` emits `NavigateToSalesStatistics` with the selected period

## 11. Definition of Done

- [x] 11.1 `./gradlew test` passes
- [x] 11.2 `./gradlew lint` passes with no new findings
- [x] 11.3 `./gradlew assembleDebug` builds
- [ ] 11.4 Every new screen-level composable has working light and dark `@Preview`s
- [ ] 11.5 Manual pass against Figma nodes `2238-30891`, `2238-31151`, `2238-31411` and `2238-31712` in light and dark, plus Inicio unchanged and `Ver más` → back round trip
