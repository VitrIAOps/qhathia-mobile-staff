## 1. Transport, domain and mapping

- [x] 1.1 `core:network` — add `confirmedOrders: Int? = null` and `confirmedOrdersDeltaPercent: Double? = null` to `StoreActivityDto`, and `totalCount: Int? = null` to `NeedsAttentionDto`; update their KDoc (`totalImmobilized` is the sum of the listed items only; `totalCount` is the size of the whole set)
- [x] 1.2 `core:network` — deserialization tests: an activity response with and without `confirmedOrders`; a ranking response with and without `needsAttention.totalCount`
- [x] 1.3 `core:domain` — add `confirmedOrders: Int? = null` and `confirmedOrdersDeltaPercent: Double? = null` as the last properties of `StoreActivity`, and `needsAttentionTotalCount: Int? = null` as the last of `SalesRanking`, documenting that `null` means "not reported" and is different from `0`
- [x] 1.4 `core:data` — map them in `toStoreActivity()` (delta through `finiteOrNull()`) and `toSalesRanking()` (negative `totalCount` → `null`)
- [x] 1.5 `core:data` — `StoreInsightsMappersTest`: confirmed orders and delta mapped; missing field stays `null`; non-finite delta becomes `null`; `totalCount` mapped, missing → `null`, negative → `null`
- [x] 1.6 `core:testing` — add `confirmedOrders`, `confirmedOrdersDeltaPercent` to `TestFixtures.storeActivity(...)` and `needsAttentionTotalCount` to `TestFixtures.salesRanking(...)` (sensible non-null defaults, e.g. 9 / 12.5 / `needsAttention.size`)
- [x] 1.7 `./gradlew :core:network:testDebugUnitTest :core:data:testDebugUnitTest :core:domain:test`

## 2. Pedidos confirmados in the Actividad tab (BUG-003)

- [x] 2.1 `features:sales-statistics` — add `statistics_activity_orders` ("Pedidos" / "Orders") and `statistics_activity_orders_caption` ("confirmados" / "confirmed")
- [x] 2.2 `ActivityTab` — when `activity.confirmedOrders != null`, draw a `HorizontalDivider(color = QhathiaColors.rowDivider)` and a full-width `ActivityFigure` with `Icons.Outlined.ShoppingBag`, the new strings, `confirmedOrders` and its delta (design Decision 2); draw nothing extra when it is `null`
- [x] 2.3 Preview data — give `SalesStatisticsPreviewData.activity` confirmed orders (9, +12.5%); add a preview for the card without the field (older backend), light and dark

## 3. Needs-attention caption (BUG-004)

- [x] 3.1 Add `needsAttentionCaption(listed, totalCount)` and `NeedsAttentionCaption` (design Decision 3) in `features:sales-statistics`
- [x] 3.2 Reword `statistics_needs_attention_caption` (es/en) to the "Inmovilizados en …" / "Tied up in …" forms, and add the `statistics_needs_attention_caption_partial` plural (es/en)
- [x] 3.3 `RankingTab` — build the caption from `needsAttentionCaption(ranking.needsAttention.size, ranking.needsAttentionTotalCount)`
- [x] 3.4 Preview data — give the ranking preview a `needsAttentionTotalCount` larger than its list so the partial caption is visible
- [x] 3.5 Unit tests for `needsAttentionCaption`: total missing → `All`; total equal → `All`; total larger → `Partial`; total smaller (inconsistent) → `All`
- [x] 3.6 Grep the feature for "Generados por los" / "productos no vendidos" to confirm the old wording is gone from the needs-attention card
- [x] 3.7 `./gradlew :features:sales-statistics:testDebugUnitTest :features:sales-statistics:lintDebug`

## 4. Docs and verification

- [x] 4.1 Mark §3.2 and §3.3 as done in `docs/handoff/android-pendientes.md`, including the `confirmedOrders` and `needsAttention.totalCount` rows of "Resumen de campos nuevos"
- [x] 4.2 Full build: `./gradlew testDebugUnitTest lintDebug assembleDebug`
- [ ] 4.3 Manual — Actividad tab against production: the three figures read as a funnel, pills worded for the period, layout holds at 360 dp and at large font scale, light and dark; compare with Figma node `2238-31712` and confirm the added row with design
- [ ] 4.4 Manual — Ranking tab in monthly view: the caption reads "Inmovilizados en N de los M productos sin ventas" when more products did not sell than are listed
