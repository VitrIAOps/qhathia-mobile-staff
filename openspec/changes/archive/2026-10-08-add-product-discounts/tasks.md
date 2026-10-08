> No new Gradle module is required: every module touched below already exists and is registered in `settings.gradle.kts`. No code is added as packages inside `:app`.

## 1. Network layer (`:core:network`)

- [x] 1.1 In `dto/ProductDtos.kt`, add `@Serializable DiscountDto(id, discountType, value: Double, startsAt, endsAt, active: Boolean = true)` and `@Serializable CreateDiscountRequestDto(discountType, value, startsAt, endsAt)` (ISO-8601 strings for the instants), with KDoc referencing the contract endpoints.
- [x] 1.2 In `service/ProductApiService.kt`, add `getDiscounts(productId): NetworkResult<List<DiscountDto>>` → `GET api/v1/products/{id}/discounts`.
- [x] 1.3 In `service/ProductApiService.kt`, add `createDiscount(productId, body): NetworkResult<DiscountDto>` → `POST api/v1/products/{id}/discounts`.
- [x] 1.4 In `service/ProductApiService.kt`, add `deleteDiscount(productId, discountId): NetworkResult<Unit>` → `DELETE api/v1/products/{id}/discounts/{discountId}` (204, no body), all wrapped in `runCatching { … }.toNetworkResult()`.

## 2. Domain layer (`:core:domain`)

- [x] 2.1 Add `model/Discount.kt`: `enum DiscountType { PERCENTAGE, FIXED_AMOUNT }`, `data class Discount(type, value)` with `applyTo(price: Double): Double` (rounded to cents, never below 0), and `data class ProductDiscount(id, discount, startsAt: Instant, endsAt: Instant, active)`.
- [x] 2.2 Add `model/DiscountDraft.kt`: immutable editor model `(price, type, input)` exposing `resultingPrice`, `isValid`, an error kind, `withType()` (clears input), `withInput()`, `withQuickPercentage()`, `toDiscount()` and a `from(price, discount?)` factory; rules: percentage whole number 1–100, amount > 0 with ≤ 2 decimals and ≤ price. A resulting price of exactly 0 is valid (100 % or amount == price); anything beyond is rejected. Expose `QUICK_PERCENTAGES = listOf(10, 20, 50)`.
- [x] 2.3 Add `model/DiscountError.kt` (`Network`, `NotFound`, `InvalidInput`, `Unknown`) and `DiscountException`, mirroring `UpdateProductError`.
- [x] 2.4 Add `repository/ProductDiscountRepository.kt` with `getDiscounts(productId): Result<List<ProductDiscount>>`, `createDiscount(productId, discount, startsAt, endsAt): Result<ProductDiscount>`, `deleteDiscount(productId, discountId): Result<Unit>`.
- [x] 2.5 Add `GetProductDiscountUseCase` returning the current discount (active, not ended at `clock.instant()`, latest-ending if several) or `null`.
- [x] 2.6 Add `SaveProductDiscountUseCase(productId, discount)`: list → delete every existing discount → create with `startsAt = clock.instant()`, `endsAt = startsAt + 7 days`; stop and fail on the first failing step.
- [x] 2.7 Add `RemoveProductDiscountUseCase(productId)`: list → delete every existing discount.
- [x] 2.8 Inject `java.time.Clock` into the use cases, reusing the `Clock` already provided by `:app`'s `AppModule` (no new binding).

## 3. Data layer (`:core:data`)

- [x] 3.1 Add mappers in `mapper/ProductMappers.kt` (or a new `DiscountMappers.kt`): `DiscountDto.toDomain()` (unknown `discountType` rows are skipped) and `Discount.toCreateRequestDto(startsAt, endsAt)`.
- [x] 3.2 Add `repository/ProductDiscountRepositoryImpl` mapping `NetworkResult → Result` with `DiscountException` (404 → `NotFound`, 400/422 → `InvalidInput`, network → `Network`).
- [x] 3.3 Bind `ProductDiscountRepository` → `ProductDiscountRepositoryImpl` in the data Hilt module, matching the existing `ProductRepository` binding style.

## 4. Shared UI (`:core:ui`)

- [x] 4.1 Add `component/QhathiaDiscountDialog.kt`: stateless dialog per Figma `1488:16634` — title, struck-through original price, bold resulting price, `QhathiaSegmentedControl` (Porcentaje / Monto), labelled `QhathiaTextField`, quick-option chips (percentage mode only), `Cancelar` / `Aplicar` buttons with `isApplyEnabled` and `isLoading`. Takes primitives and callbacks only — no domain types.
- [x] 4.2 Add `component/QhathiaDiscountToggleRow.kt`: `Descuento` label + `Switch` (`enabled`, `checked`, `isLoading`) and an optional tappable summary line below it, with 48dp touch targets and accessibility semantics.
- [x] 4.3 Add the shared strings to `:core:ui` resources (dialog title, mode labels, input labels, validation errors, `Descuento`, `Descuento de %1$s%%`, `Descuento de %1$s soles`, cancel/apply).
- [x] 4.4 Add light and dark `@Preview`s for the dialog (percentage, amount, error, loading) and the toggle row (off, disabled, on with summary).

## 5. Test utilities (`:core:testing`)

- [x] 5.1 Add `FakeProductDiscountRepository` (in-memory list, configurable failures per operation, recorded calls in order).
- [x] 5.2 Add `TestFixtures.productDiscount(...)` helpers.

## 6. Product registration (`:features:product-register`)

- [x] 6.1 Extend `ProductRegisterUiState` with `discount: Discount? = null`, `discountDraft: DiscountDraft? = null` and derived `isDiscountToggleEnabled` (valid price, not submitting).
- [x] 6.2 Extend `ProductRegisterUiEvent` with `DiscountToggled(enabled)`, `DiscountSummaryClicked`, `DiscountDraftChanged(draft)` (one event carrying the next draft for a mode, input or quick-option change), `DiscountApplied`, `DiscountDismissed`.
- [x] 6.3 Handle the events in `ProductRegisterViewModel`: toggle on / summary tap opens the draft; apply stores the discount and marks the form dirty; dismiss closes the draft (toggle stays off when no discount was set); toggle off clears the discount.
- [x] 6.4 In `onPriceChanged`, clear the discount when the new price is invalid or an applied fixed amount exceeds it.
- [x] 6.5 Inject `SaveProductDiscountUseCase`; in `submit()`, after a successful registration with a discount set, save it for the returned product id; on discount failure emit a new `ProductRegisterUiEffect.NavigateBackWithDiscountFailed`, surfaced through a new `onDiscountFailed` callback on `ProductRegisterScreen` and `ProductRegisterNavigation` (same pattern as `onPlanLimit`).
- [x] 6.6 Render `QhathiaDiscountToggleRow` below the price field in `ProductRegisterContent` and show `QhathiaDiscountDialog` while `discountDraft != null`.
- [x] 6.7 Change `register_quantity_label` from `Cantidad` to `Stock` in `strings.xml`.
- [x] 6.8 Update the screen `@Preview`s (light + dark) to cover the toggle with a summary.

## 7. Product detail (`:features:product-detail`)

- [x] 7.1 Extend `ProductDetailUiState` with `loadedDiscount: Discount? = null` (as stored), `discount: Discount? = null` (as shown on the form), `discountDraft`, `isDiscountLoading`, `isDiscountError` and derived `isDiscountToggleEnabled` and `isDiscountChanged` (`discount != loadedDiscount`).
- [x] 7.2 Extend `ProductDetailUiEvent` with the same discount events as registration plus `RetryDiscount`.
- [x] 7.3 Inject `GetProductDiscountUseCase`, `SaveProductDiscountUseCase`, `RemoveProductDiscountUseCase`; load the discount concurrently in `load()` as a non-fatal request, seeding both `loadedDiscount` and `discount`.
- [x] 7.4 Handle the form-only events: toggle on / summary tap opens the draft; apply sets `discount`, closes the dialog and sets `isDirty = true`; toggle off clears `discount` and sets `isDirty = true`; dismiss leaves the form as it was. No request is issued by any of them.
- [x] 7.5 In `onPriceChanged`, clear `discount` when the new price is invalid or the discount's fixed amount exceeds it.
- [x] 7.6 In `save()`, after `UpdateProductUseCase` succeeds and only when `isDiscountChanged` and the discount did not fail to load: call `SaveProductDiscountUseCase` (discount set) or `RemoveProductDiscountUseCase` (turned off). On success emit `NavigateBackSaved`; on discount failure stay on the form, keep `isDirty = true` and emit `ShowError` with the "product saved, discount not updated" message. A failed product update issues no discount request.
- [x] 7.7 Render `QhathiaDiscountToggleRow` below the price field in `ProductDetailContent`, the retry affordance on load failure, and the dialog while `discountDraft != null`.
- [x] 7.8 Change `detail_quantity_label` from `Cantidad` to `Stock`; add the discount error strings.
- [x] 7.9 Update the screen `@Preview`s (light + dark).

## 8. Inventory (`:features:product-inventory`)

- [x] 8.1 In `ProductRow`, wrap the `⋮` icon in an `IconButton` with a content description and anchor a Material 3 `DropdownMenu` to it with `Editar producto` and `Editar descuento` (icons per Figma `1478:16234`); add `onEditProduct` / `onEditDiscount` callbacks.
- [x] 8.2 Extend `InventoryUiState` with `discountProductId`, `discountDraft`, `isDiscountLoading`, `isDiscountSaving`; extend `InventoryUiEvent` with `EditDiscountClicked(productId)` and the dialog events; extend `InventoryUiEffect` with a success message effect.
- [x] 8.3 Inject `GetProductDiscountUseCase` and `SaveProductDiscountUseCase` in `InventoryViewModel`; on `EditDiscountClicked` load the current discount then open the draft with the row's price; on apply save, close and emit the success snackbar; on failure keep the dialog open and emit an error.
- [x] 8.4 Show `QhathiaDiscountDialog` from `InventoryContent` while `discountDraft != null`, and wire the row menu callbacks.
- [x] 8.5 Add a trailing clear `IconButton` to `InventorySearchField`, visible only when the query is not empty, calling `onQueryChange("")`, with a content description.
- [x] 8.6 Add `INVENTORY_REGISTER_RESULT_DISCOUNT_FAILED` to the inventory navigation results and handle it in `InventoryScreen` (refresh + distinct snackbar message).
- [x] 8.7 Add the new strings (menu items, content descriptions, discount updated / failed messages) and update `@Preview`s (light + dark) for the row menu and the filled search field.

## 9. Order requests (`:features:order-requests`)

- [x] 9.1 Add the same trailing clear `IconButton` to `OrderSearchField`, visible only when the query is not empty, calling `onQueryChange("")`; add its content-description string and refresh the header `@Preview`.

## 10. App wiring (`:app`)

- [x] 10.1 In `navigation/MainShell.kt`, pass `onDiscountFailed` to the registration destination, setting `INVENTORY_REGISTER_RESULT_DISCOUNT_FAILED` on the inventory entry's saved state and popping back, alongside the existing `onRegistered` and `onPlanLimit` handlers.

## 11. Tests & Definition of Done

- [x] 11.1 Unit-test `Discount.applyTo` and `DiscountDraft` (percentage bounds 1–100, amount ≤ price and ≤ 2 decimals, mode switch clears input, quick options, `from()` prefill).
- [x] 11.2 Unit-test the three use cases with `FakeProductDiscountRepository` and `MutableClock`: current-discount selection, delete-before-create order, one-week window, failure short-circuit.
- [x] 11.3 Unit-test `ProductDiscountRepositoryImpl` and the mappers (success, HTTP error mapping, network error, unknown discount type skipped).
- [x] 11.4 Extend `ProductRegisterViewModelTest`: toggle gating by price, apply / dismiss / toggle off, price change clears the discount, discount saved after registration, discount failure effect, no discount call when registration fails.
- [x] 11.5 Extend `ProductDetailViewModelTest`: discount seeded on load, non-fatal load failure and retry, apply / toggle off only dirty the form and issue no request, price change clears the discount, save persists the change after the product update (save, remove, untouched → no discount call), product-update failure issues no discount call, discount failure keeps the form dirty with the specific error, discard issues no discount call.
- [x] 11.6 Extend `InventoryViewModelTest`: `EditDiscountClicked` prefill and empty cases, load failure, apply success and failure.
- [x] 11.7 Run `./gradlew test` and `./gradlew lint` and confirm both pass; verify every new or changed screen composable has working light and dark `@Preview`s.

## 12. Follow-up: register vs. edit wording, final price next to the summary

- [x] 12.1 `QhathiaDiscountToggleRow`: add `finalPrice: Double?` and lay the summary line out as a space-between row — summary at the start, `S/ {finalPrice}` at the end — with a content description for the price.
- [x] 12.2 Add a derived `discountedPrice` to `ProductRegisterUiState` and `ProductDetailUiState` (the form's price with the discount applied) and pass it to the toggle row in both forms.
- [x] 12.3 `InventoryUiState`: add `knownDiscounts` (looked-up discount per product id), `discountLookupProductId` and `hasDiscount(productId)`; add `InventoryUiEvent.RowActionsOpened(productId)`.
- [x] 12.4 `InventoryViewModel`: look the discount up once when a row's menu opens (silent on failure), reuse it when the option is chosen instead of loading again, update it after a save, drop it after a failed save, and clear everything on a product reload.
- [x] 12.5 `ProductRow`: report the menu opening, label the option `Registrar descuento` when the product has no discount and `Editar descuento` otherwise, and show a spinner and disable it while the lookup is in flight; add the `inventory_action_register_discount` string (es + en).
- [x] 12.6 Tests: `discountedPrice` in both form ViewModel tests; lookup, label state, cache reuse, post-save update and reload invalidation in `InventoryViewModelTest`; re-run `./gradlew test lint`.

## 13. Follow-up: optional tags and fainter placeholders

- [x] 13.1 `QhathiaTextField`: draw the placeholder at reduced opacity so an empty field does not read as filled.
- [x] 13.2 Drop the "at least one tag" condition from `ProductRegisterUiState.isSubmitEnabled` and `ProductDetailUiState.isSaveEnabled`.
- [x] 13.3 Mark `CreateProductRequestDto.tags` with `@EncodeDefault` so an empty list is sent as `"tags": []` on create and on the full-replace update.
- [x] 13.4 Tests: blank tags no longer block registration or saving and are sent as an empty list; the request JSON carries `"tags": []`.
