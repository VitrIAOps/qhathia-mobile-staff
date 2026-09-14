## 1. Create the `:features:home` module

- [x] 1.1 Create the module directories: `features/home/src/main/java/com/qhathia/mobile/staff/feature/home/{model,ui,navigation}`, `features/home/src/main/res/values`, `features/home/src/test/java/com/qhathia/mobile/staff/feature/home/ui`
- [x] 1.2 Write `features/home/build.gradle.kts` from the Feature-module template in `docs/android/android-frontend-standards.md` — `android.library` + `kotlin.compose` + `ksp` + `hilt`, `namespace = "com.qhathia.mobile.staff.feature.home"`, `compileSdk 36`, `minSdk 26`, `buildFeatures.compose = true`, `testOptions { unitTests.all { it.useJUnitPlatform() } }`; deps mirrored from `features/order-requests/build.gradle.kts` (`:core:domain`, `:core:ui`, `:core:common`, Compose BOM, lifecycle, navigation-compose, Hilt + `ksp(kotlin.metadata.jvm)`, `kotlinx.immutable`, Coil for the restock thumbnails, JUnit5/MockK/Turbine test set)
- [x] 1.3 Register `include(":features:home")` in `settings.gradle.kts`
- [x] 1.4 Add the minimal `features/home/src/main/AndroidManifest.xml` (`<manifest />`)
- [x] 1.5 Confirm the empty module builds: `./gradlew :features:home:assembleDebug`

## 2. Shared design-system additions in `:core:ui`

- [x] 2.1 Add the summary-card tokens to `QhathiaPalette` + `QhathiaColors` following the existing light/dark accessor pattern: card surface (`#0b1b28`), bar fill (`#10283c`), insight-strip surface, on-card muted text, and the period-tab track/selected fills
- [x] 2.2 Add `QhathiaBadge` — brand-accent circle, white `Caption` digits, `minSize 16.dp`, values above 99 rendered as `99+`; previews in light and dark
- [x] 2.3 Add an optional `badgeCount: Int?` parameter to `QhathiaBottomBarItem`, drawn as an overlay anchored to the glyph's top-end corner and merged into the item's semantics; default `null` MUST render today's output unchanged
- [x] 2.4 Add the vector drawables `ic_bell`, `ic_sparks`, `ic_search`, `ic_alert`, `ic_star` to `core/ui/src/main/res/drawable` (traced from Figma node `1954-6799`, 24dp / 16dp viewports matching their use)
- [x] 2.5 Extend `ComponentPreviews.kt` with a bottom bar showing a badged Solicitudes tab

## 3. Domain layer — `:core:domain`

- [x] 3.1 Add the insight models: `SalesPeriod` (`MONTHLY`, `WEEKLY`, `DAILY`, `HOURLY`), `SalesBucket(label, amount)`, `SalesSummary(period, periodLabel, total, deltaPercent, buckets, highlightedBucketIndex, generatedAt)`, `MissingProductSuggestion(id, name, searchCount)`, `RestockSuggestion(id, name, imageUrl, remainingStock)`, `StoreSuggestions(missingProducts, restockProducts)` — amounts as `BigDecimal`, matching `Order.total`
- [x] 3.2 Add the `StoreInsightsRepository` port: `suspend fun getSalesSummary(period: SalesPeriod): Result<SalesSummary>` and `suspend fun getSuggestions(): Result<StoreSuggestions>`
- [x] 3.3 Add `GetSalesSummaryUseCase` and `GetSalesSuggestionsUseCase`
- [x] 3.4 Extend `OrderRepository` with `val pendingOrderCount: Flow<Int>` and `suspend fun refreshPendingOrderCount(): Result<Int>`
- [x] 3.5 Add `ObservePendingOrderCountUseCase` (returns the flow) and `RefreshPendingOrderCountUseCase`

## 4. Data layer — `:core:data`

- [x] 4.1 Add `MockStoreInsightsRepository` with canned data for all four periods (weekly matching the Figma node: `Esta semana`, `5770`, `+12`, buckets `Sem 1` 1240 / `Sem 2` 1480 / `Sem 3` 1360 / `Sem 4` 1690, highlight index 3) plus both suggestion rows; compute `highlightedBucketIndex` from the buckets rather than hardcoding it, and add a file-level `TODO(backend)` naming the endpoints it stands in for
- [x] 4.2 Bind `StoreInsightsRepository` to the mock in `DataModule` under a clearly-named `@Binds` so the future swap is a one-line change
- [x] 4.3 Make `OrderRepositoryImpl` `@Singleton` and give it a `MutableStateFlow<Int>` pending-count cache exposed as `pendingOrderCount`
- [x] 4.4 Implement `refreshPendingOrderCount()` as a `status=PENDING&size=1` query that writes `totalElements` into the cache; reset the cache to `0` when it fails with an auth error
- [x] 4.5 Update the cache from `getOrders(...)` when the query was the pending query (so the Solicitudes screen's own load feeds the badge with no extra request), and re-fetch it after a successful `confirmOrder` / `rejectOrder`

## 5. Test doubles — `:core:testing`

- [x] 5.1 Add `FakeStoreInsightsRepository` with settable summary/suggestions results per period, including a failure mode
- [x] 5.2 Extend `FakeOrderRepository` with the `pendingOrderCount` flow and a `refreshPendingOrderCount` that the tests can drive
- [x] 5.3 Add fixtures for `SalesSummary`, `MissingProductSuggestion` and `RestockSuggestion`

## 6. Home feature — MVI contract and ViewModel

- [x] 6.1 Add `HomeUiState` (`@Immutable`; `merchantFirstName`, `selectedPeriod = WEEKLY`, `summary`, `missingProducts`, `restockProducts`, `pendingOrderCount`, `isLoading`, `isError`, `lastUpdatedAt`), `HomeUiEvent` (`Refresh`, `SelectPeriod`, `OpenNotifications`) and `HomeUiEffect` (`NavigateToOrderRequests`)
- [x] 6.2 Add `HomeViewModel`: loads the summary for the selected period and the suggestions, takes the first name from `ObserveSessionUseCase`, collects `ObservePendingOrderCountUseCase` into the state, calls `RefreshPendingOrderCountUseCase` alongside each load, stamps `lastUpdatedAt` on success, and emits `NavigateToOrderRequests` on `OpenNotifications`
- [x] 6.3 Implement the failure rule: a failed load sets `isError` while keeping the previously loaded summary, suggestions and `lastUpdatedAt` on screen; `Refresh` retries with the selected period

## 7. Home feature — UI

- [x] 7.1 `HomeHeader` — `headerGradient` background with rounded bottom corners, `Hola <nombre>`, the `Última actualización a las HH:mm` line (device-locale `FormatStyle.SHORT`, rendered only once `lastUpdatedAt` is set), and the bell `IconButton` with `QhathiaBadge`; content description names the action and the pending count as one node
- [x] 7.2 `PeriodTabs` — the four-pill selector on its track, brand-accent selected pill, `selected` semantics per pill
- [x] 7.3 `SalesBarChart` — `Row` of `Column`s, bar height proportional to `amount / maxAmount` with a minimum visible height, amount label above and bucket label below, highlighted bucket in brand accent; one merged content description announcing the total and the best bucket
- [x] 7.4 `SalesInsightStrip` — star glyph plus the `La <bucket> generó S/ <amount>, <n>% más que la <periodo> anterior.` sentence
- [x] 7.5 `SalesSummaryCard` — dark navy card at `Dimens.CardRadius` composing the tabs, the period label, the `S/ 5,770` headline (`H1`, single line, intrinsic card height), the delta line in success green, the chart and the insight strip; empty treatment when the period total is zero
- [x] 7.6 `MissingProductCard` and `RestockProductCard` per the Figma card specs, including the `9 / Búsquedas` block, the `Solo queda N` warning chip (singular/plural) and the inert `Agregar` / `Reabastecer` buttons; restock images via Coil with explicit size constraints and a neutral placeholder
- [x] 7.7 `SuggestionSection` — heading with its glyph plus a `LazyRow` of cards with stable keys; the section is omitted entirely when its row is empty
- [x] 7.8 `HomeContent` — stateless `LazyColumn` composing header, the `Resumen` heading with the inert `Ver más ›` action, the summary card and both suggestion sections; loading and error/retry treatments
- [x] 7.9 `HomeScreen` — stateful wrapper: `collectAsStateWithLifecycle`, `HomeUiEffect` collection, `onNavigateToOrderRequests` callback
- [x] 7.10 `HomeNavigation.kt` — `const val HOME_ROUTE = "home"` and `fun NavGraphBuilder.homeGraph(onNavigateToOrderRequests: () -> Unit)`
- [x] 7.11 `features/home/src/main/res/values/strings.xml` — every user-facing string, including the plural-aware `Solo queda(n) N`
- [x] 7.12 `HomePreviewData.kt` plus light/dark `@Preview`s for `HomeContent`, `SalesSummaryCard`, `SalesBarChart` and both card types

## 8. Wire it into `:app`

- [x] 8.1 Add `implementation(project(":features:home"))` to `app/build.gradle.kts`
- [x] 8.2 Add `MainShellViewModel` exposing `pendingOrderCount` from `ObservePendingOrderCountUseCase`, refreshing via `RefreshPendingOrderCountUseCase` on entry to the shell
- [x] 8.3 In `MainShell`: drop the local `HOME_ROUTE` constant in favour of the feature's, replace `composable(HOME_ROUTE) { HomePlaceholderScreen() }` with `homeGraph(onNavigateToOrderRequests = …)` using the bottom bar's navigation options (`popUpTo(startDestination) { saveState }`, `launchSingleTop`, `restoreState`), and pass `badgeCount` to the Solicitudes `QhathiaBottomBarItem`
- [x] 8.4 Delete `app/src/main/java/com/qhathia/mobile/staff/ui/home/HomePlaceholderScreen.kt`
- [x] 8.5 Make `HOME_ROUTE` the shell's start destination, so entering the app lands on Inicio with its tab selected (revised decision — initially kept on `INVENTORY_ROUTE`)

## 9. Refactor the Solicitudes count onto the shared source

- [x] 9.1 Remove `OrderRequestsViewModel`'s private pending-count bookkeeping (its dedicated `size=1` query and the local assignment to `pendingCount`) and feed `OrderRequestsUiState.pendingCount` from `ObservePendingOrderCountUseCase`
- [x] 9.2 Verify `Pendientes (N)` still updates after confirm and reject, now via the repository cache
- [x] 9.3 Update the affected `OrderRequestsViewModelTest` cases to drive the count through the fake's flow

## 10. Tests

- [x] 10.1 `HomeViewModelTest`: default period is `WEEKLY`; `SelectPeriod` reloads and keeps the selection across a refresh; success stamps `lastUpdatedAt`; failure sets `isError` while keeping the previous summary; `OpenNotifications` emits `NavigateToOrderRequests`; the greeting takes the session's first name and degrades with no session
- [x] 10.2 `MockStoreInsightsRepositoryTest` (or equivalent in `:core:data`): every period returns a summary whose `highlightedBucketIndex` points at the largest bucket
- [x] 10.3 `OrderRepositoryImpl` tests: `refreshPendingOrderCount` publishes `totalElements`; a pending `getOrders` updates the cache without an extra request; confirm and reject refresh it; an auth failure resets it to `0`
- [x] 10.4 `MainShellViewModelTest`: the badge count follows the shared flow and a refresh is requested on entry
- [x] 10.5 Compose test for `QhathiaBottomBarItem`: no badge at `null`/`0`, `99+` above 99

## 11. Definition of Done

- [x] 11.1 `./gradlew test` passes
- [x] 11.2 `./gradlew lint` passes with no new findings
- [x] 11.3 `./gradlew assembleDebug` builds
- [x] 11.4 Every new screen-level composable has a working light **and** dark `@Preview`
- [x] 11.5 Manual pass against Figma node `1954-6799`: header, period switching, chart highlight, insight sentence, both suggestion rows, bell badge and the Solicitudes tab badge — in light and dark

## 12. Replace the mock with the API (`api-contract.md` §17)

- [x] 12.1 `:core:network` — `DashboardDtos.kt` (`SalesDashboardDto`, `SalesPeriodDto`, `RestockDto`, `RestockItemDto`) and `InsightsDtos.kt` (`UnmetSearchesDto`, `UnmetSearchGroupDto`, `UnmetSearchExampleDto`); `DashboardApiService` (`getSales(granularity, buckets)`, `getRestock(limit)`) and `InsightsApiService` (`getUnmetSearches(days, limit)`) on the authenticated client, provided in `NetworkModule`
- [x] 12.2 `:core:domain` — `SalesSummary`: drop `periodLabel`, `total` documented as the revenue of the period in progress, `deltaPercent: Double?`, new `highlightedDeltaPercent: Double?`; `MissingProductSuggestion.searchCount` → `personCount`
- [x] 12.3 `:core:data` — `StoreInsightsMappers.kt`: money to `BigDecimal`, highlight index (`-1` when nothing sold), highlighted bucket vs preceding bucket delta (null without a base), unmet-search group → suggestion (most frequent raw text, capitalised, term fallback, blank terms dropped, de-duplicated ids), restock item → suggestion
- [x] 12.4 `:core:data` — `StoreInsightsRepositoryImpl` (`buckets=4`, concurrent suggestion requests, all-or-nothing), rebind `DataModule.bindStoreInsightsRepository`, delete `MockStoreInsightsRepository` and its test
- [x] 12.5 `:features:home` — period label and "previous period" phrases as string resources; `Double` percent formatting in `HomeFormatters` (sign decided before rounding, no `-0%`); delta line omitted when null and coloured by sign; insight sentence with up / down / flat / no-comparison variants; empty treatment only when no bucket sold; `Personas` label; previews updated
- [x] 12.6 `:core:testing` — `TestFixtures` and previews aligned with the new model
- [x] 12.7 Tests: `StoreInsightsMappersTest`, `StoreInsightsRepositoryImplTest`, `HomeFormattersTest`; `HomeViewModelTest` still green
- [x] 12.8 `./gradlew test lint assembleDebug` pass
