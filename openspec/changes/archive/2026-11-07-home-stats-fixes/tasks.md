## 1. One count request at a time (`core:domain`, `core:data`, `core:testing`) — F-75 (Inicio)

- [x] 1.1 `OrderRepository.refreshPendingOrderCount(reuseInFlight: Boolean = true)`; KDoc
- [x] 1.2 `OrderRepositoryImpl`: `Mutex` + generation bumped on success; a waiter reuses the fresh count when `reuseInFlight` and the generation moved; the current body becomes `fetchPendingCount()`; `refreshCountAfterSettling` passes `false` (design decision 8)
- [x] 1.3 `HandleOrderPushUseCase` passes `reuseInFlight = false`; `FakeOrderRepository` records the flag
- [x] 1.4 Tests: `OrderRepositoryImplTest` "concurrent refreshes send one request" (MockEngine counting calls, held answer); "a refresh that must be fresh sends its own request after the one in flight"; "a failed refresh is not shared"; `HandleOrderPushUseCaseTest` asks for a fresh count

## 2. Shared UI (`core:ui`) — F-57, F-76

- [x] 2.1 `QhathiaBottomBarItem`: `selectable(selected, role = Role.Tab, onClick)` for every tab; with a badge, `clearAndSetSemantics { selected; role = Tab; contentDescription; onClick }`; drop the separate `semantics` and `clickable` (design decision 7)
- [x] 2.2 `component/time/TimeOfDay.kt`: `shortTimeFormatter(locale, is24Hour)` and `@Composable rememberShortTimeFormatter()` (design decision 6)

## 3. Ranking data (`core:network`, `core:domain`, `core:data`) — F-100

- [x] 3.1 DTOs: `SalesRankingDto.asOf`, `TopSellersDto.periodRevenue` / `productsSold`, `TopSellerItemDto.active`
- [x] 3.2 `DashboardApiService.getRanking(granularity, categoryId, limit = DEFAULT_RANKING_LIMIT)` (5) with `parameter("limit", limit)`; the repository uses the default, so the domain does not change
- [x] 3.3 Domain: `SalesRanking.periodRevenue: BigDecimal?`, `productsSold: Int?`, `asOf: Instant?`; `TopSellingProduct.active: Boolean = true`; mapper
- [x] 3.4 Mapper tests: the new fields are read; absent fields give null / `active = true`; the service test checks `limit=5`

## 4. Unmet-search variants (`core:network`, `core:domain`, `core:data`)

- [x] 4.1 `UnmetSearchGroupDto`: `variants: List<UnmetSearchVariantDto> = emptyList()` (`term`, `searchCount`), `personCountEstimated: Boolean = false`
- [x] 4.2 `MissingProductSuggestion.variants: List<String> = emptyList()` (other terms than the group's, at most 3, in the backend's order), `personCountEstimated: Boolean = false`; mapper
- [x] 4.3 Mapper tests: the group's own term is dropped from the variants; at most three; the estimated flag is read

## 5. Inicio (`features:home`) — F-56, F-57, F-76, variants

- [x] 5.1 `HomeUiEvent.AddMissingProduct(name)`, `RestockProduct(productId)`; `HomeUiEffect.NavigateToProductRegister(prefillName)`, `NavigateToProductDetail(productId)`; `HomeViewModel` emits them (design decision 1)
- [x] 5.2 `HomeContent`: wire `onAdd` / `onRestock`; drop the "Inert by design" comments and update the KDoc of `MissingProductCard` and `RestockProductCard`
- [x] 5.3 `HomeScreen` and `homeGraph`: `onNavigateToProductRegister(String)`, `onNavigateToProductDetail(String)` with `rememberUpdatedState`; KDoc
- [x] 5.4 `MissingProductCard`: "También: …" line from `variants`; "~N" and the `home_searches_label` plural when `personCountEstimated`, with its accessible text; strings `values` / `values-en`
- [x] 5.5 `HomeHeader`: bell `clearAndSetSemantics { contentDescription; role = Button; onClick }`; "Última actualización" with `rememberShortTimeFormatter()`; remove `HomeFormatters.toLocalTimeLabel`
- [x] 5.6 `HomeViewModelTest`: "restock emits navigate to product detail with the product id"; "add missing emits navigate to register with the name"

## 6. Registro with a name (`features:product-register`) — F-56

- [x] 6.1 `ProductRegisterNavigation`: `PRODUCT_REGISTER_ARG_NAME`, route `product_register?name={name}` (nullable string, default null), `productRegisterRoute(prefillName: String? = null)` with `Uri.encode`; `navigateToProductRegister()` uses it
- [x] 6.2 `ProductRegisterViewModel`: read the name once in `init`, set it as the field and as the dirty baseline
- [x] 6.3 `ProductRegisterViewModelTest`: "a prefilled name is set and is not a change"; "editing after the prefill is a change"

## 7. Resumen de ventas (`features:sales-statistics`) — F-89, F-64, F-100, F-95, F-76

- [x] 7.1 Share: `SalesStatisticsUiState.isShareEnabled`; `SalesStatisticsUiEffect.Share(tab)`; the ViewModel emits it only when enabled; `StatisticsTopBar(shareEnabled)`; KDoc (design decision 2)
- [x] 7.2 `StatisticsShare.kt`: pure `shareText(...)` per tab from resolved strings and formatters; strings `statistics_share_title`, `statistics_share_summary`, `statistics_share_ranking_header`, `statistics_share_ranking_row`, `statistics_share_activity` (`values` / `values-en`)
- [x] 7.3 `SalesStatisticsScreen`: handle `Share` with `ShareCompat.IntentBuilder` and the current state (`rememberUpdatedState`); `LifecycleEventEffect(ON_START)` → `ScreenStarted`
- [x] 7.4 `SalesStatisticsViewModel`: `isLeaving` so `NavigateBack` is emitted once; `ScreenStarted` (first ignored; later ones clear `loadedQueries` and reload the tab on screen keeping its data) (design decision 5)
- [x] 7.5 `RankingTab`: caption "Top N de M productos · S/ X de S/ Y" when both fields arrive (plurals), the current caption otherwise; `TopSellerRow` "· Eliminado" in `onSurfaceVariant` and in its accessible text; "Actualizado a las …" from `asOf` under the range
- [x] 7.6 Needs-attention captions "… productos con stock sin ventas" (`values` / `values-en`)
- [x] 7.7 `periodRangeLabel(..., timeFormatter = HOUR_FORMAT)`; `SalesStatisticsContent` passes `rememberShortTimeFormatter()`
- [x] 7.8 Tests: `StatisticsShareTest` (summary text has the range and the total; ranking rows; activity); `SalesStatisticsViewModelTest` "share while loading emits nothing", "share when loaded emits the tab", "back twice emits once", "screen started reloads the tab on screen keeping its data", "the first screen started loads nothing extra"; `StatisticsFormattersTest` an hourly range with a 12-hour formatter

## 8. Solicitudes header (`features:order-requests`) — F-76

- [x] 8.1 `OrderRequestsHeader`: format with `rememberShortTimeFormatter()`; remove its private `toShortLocalTime`

## 9. Shell (`app`) — F-56, F-64

- [x] 9.1 `NavController.popIfOn(route)` and `navigateOnce(route)` (design decision 3); use them for Resumen de ventas, Detalle (`onBack`, `onSaved`, `onDeleted`), Registro (all its exits), Dispositivos (`onBack`, `onTabletUnlinked`) and the pushes from Inicio, Inventario and Perfil
- [x] 9.2 `homeGraph(onNavigateToProductRegister = { switchTab(INVENTORY_ROUTE); navigateOnce(productRegisterRoute(it)) }, onNavigateToProductDetail = { switchTab(INVENTORY_ROUTE); navigateOnce(productDetailRoute(it)) })`; Inventario's "+" uses `productRegisterRoute()`

## 10. Verification

- [x] 10.1 Run, one at a time: `:core:data:testDebugUnitTest`, `:core:domain:test`, `:features:home:testDebugUnitTest`, `:features:product-register:testDebugUnitTest`, `:features:sales-statistics:testDebugUnitTest`, `:features:order-requests:testDebugUnitTest`, `:app:testDebugUnitTest`, then `testDebugUnitTest`
- [x] 10.2 Run `./gradlew lintDebug` and `./gradlew assembleDebug`

## 11. Manual — QA

- [ ] 11.1 Manual — S:INI-013 / X:XBU-007: "Reabastecer" opens the right Detalle (`GET /products/{id}`); "Agregar" opens Registro with "Nombre" = "Soldadora"; Guardar returns to Inventario with the snackbar; back from Detalle returns to Inventario
- [ ] 11.2 Manual — S:EST-003: Compartir opens the share sheet with "Resumen de ventas · 2 – 8 de noviembre…"; disabled while a period loads
- [ ] 11.3 Manual — S:EST-061 / S:EXP-020 / S:DIS-090: quick double back in Resumen de ventas, Detalle and Dispositivos never leaves a blank screen; double tap on the gear opens one "Ajustes de la tablet"
- [ ] 11.4 Manual — X:XJO-013 / X:XBU-020: Ranking Diario with 4+ products sold shows 5 rows and "Top 5 de N productos · S/ X de S/ Y", Y equal to Inicio's total; a sold-then-deleted product shows "· Eliminado"; "Actualizado a las …"
- [ ] 11.5 Manual — X:XBU-021: note the needs-attention total, restock an out-of-stock product by curl, leave and come back: the total goes up by one and the caption says "con stock sin ventas"
- [ ] 11.6 Manual — S:INI-004: phone on 12 h → "Última actualización a las 5:04 p. m."; on 24 h → "17:04"; after midnight "00:36"; the same in Solicitudes and in the "Por hora" range. nginx shows one `GET /orders?status=PENDING&page=0&size=1` per Inicio load
- [ ] 11.7 Manual — S:INI-062: `inspect_screen` shows the bell and the badged tab `clickable=true`; TalkBack reads "Notificaciones, 1 pendiente, botón" and "Solicitudes, 1 pendiente, pestaña"
- [ ] 11.8 Manual — unmet searches: a group with variants shows "También: …"; a group with `personCountEstimated` shows "~N búsquedas"
