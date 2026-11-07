## 1. Formatters (`core:ui`, `core:common`)

- [x] 1.1 `core:ui` `format/MoneyFormat.kt`: `Double.toPenPrice()`, `BigDecimal.toPenPrice()`, `BigDecimal.toPenAmount()` with fixed symbols ('.' decimal, ',' grouping) and `HALF_UP` (design decision 1)
- [x] 1.2 `SalesFormatters.toSolesLabel()` delegates to `toPenAmount()`; KDoc
- [x] 1.3 `core:common` `formatPriceInput(price: Double): String` next to `parsePrice` (design decision 3)
- [x] 1.4 Tests: `MoneyFormatTest` (12345678.5 → "S/ 12,345,678.50"; 189.9 → "S/ 189.90"; 0.005 rounds half up; 2848.5 amount → "S/ 2,849"; never "E"); `ProductValidatorsTest` `formatPriceInput` for 149.0, 189.9, 12345678.5, and each result accepted by `parsePrice`

## 2. Strings and callers

- [x] 2.1 `core:ui` (`values`, `values-en`): `discount_original_price_format`, `discount_price_format`, `discount_original_price_cd`, `discount_resulting_price_cd` to `%1$s`; `QhathiaDiscountDialog` and `QhathiaDiscountToggleRow` pass `toPenPrice()`; drop `preview_price_format`, `previewPriceText` uses `toPenPrice()` (design decision 2)
- [x] 2.2 `features:product-inventory` (`values`, `values-en`): `inventory_price_format`, `inventory_price_discounted_cd` to `%1$s`; `ProductRow` passes `toPenPrice()`
- [x] 2.3 `features:order-requests` (`values`, `values-en`): `order_price` to `%1$s`; `OrderCard` and `OrderDetailDialog` pass `toPenPrice()`
- [x] 2.4 `features:profile`: `SubscriptionSummaryCard` and `SubscriptionScreen` show the plan price with `toPenPrice()`

## 3. Price field (`features:product-detail`)

- [x] 3.1 `ProductDetailViewModel.seededFrom` uses `formatPriceInput` for `price` and `serverPricing.priceText`; remove the private `formatPrice`
- [x] 3.2 `ProductDetailViewModelTest`: "a large price seeds a valid field" (12345678.5 → "12345678.50", valid, discount enabled); "a price with cents seeds two decimals and keeps the server discounted price"; the existing "a whole price renders without a trailing decimal" still passes

## 4. Verification

- [x] 4.1 Run, one at a time: `:core:common:testDebugUnitTest`, `:core:ui:testDebugUnitTest`, `:features:product-detail:testDebugUnitTest`, `:features:product-inventory:testDebugUnitTest`, `:features:order-requests:testDebugUnitTest`, `:features:profile:testDebugUnitTest`, then `testDebugUnitTest`
- [x] 4.2 Run `./gradlew lintDebug` and `./gradlew assembleDebug`

## 5. Manual — QA

- [ ] 5.1 Manual — S:DET-015: "QA Precio grande" (12345678.5) opens with "12345678.50", the discount switch is enabled and saving works
- [ ] 5.2 Manual — S:EXP-011 / S:PER-059: Inventario, the discount summary, "Aplicar descuento" (struck price with "S/"), Solicitudes and Inicio write money with a point and thousands commas, in Spanish and in English
- [ ] 5.3 Manual — Mi suscripción shows the plan price with its cents
