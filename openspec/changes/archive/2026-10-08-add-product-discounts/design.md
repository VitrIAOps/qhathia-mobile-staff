## Context

The app already registers products (`:features:product-register`), edits them (`:features:product-detail`) and lists them (`:features:product-inventory`) through `ProductRepository` → `ProductApiService`. Discounts exist in the backend as a sub-resource of a product but are not consumed anywhere: `ProductDetailDto` explicitly ignores `discounts[]`, and the inventory row's `⋮` icon is visual-only.

Source of truth for this change:

- ClickUp task [Descuentos de productos](https://app.clickup.com/t/86e3mp889).
- Figma `Qhatia-DS`: registration form with the toggle (`1478:15846`), `Aplicar descuento` dialog (`1488:16634`), row actions menu (`1478:16234`).
- `docs/android/api-contract.md` §8 — discounts endpoints:
  - `GET /api/v1/products/{id}/discounts` → `[{ id, discountType, value, startsAt, endsAt, active, createdAt }]`
  - `POST /api/v1/products/{id}/discounts` with `{ discountType: PERCENTAGE | FIXED_AMOUNT, value (> 0), startsAt, endsAt }` (all required, `endsAt > startsAt`) → `201 DiscountResponse`
  - `DELETE /api/v1/products/{id}/discounts/{discountId}` → `204`

Constraints: Clean Architecture + MVI, feature modules never depend on each other, `:core:domain` is pure Kotlin, `:core:network` does not know domain models, `:core:ui` holds no business logic. The project is fully modularised; every module this change touches already exists.

## Goals / Non-Goals

**Goals:**

- Create, edit and remove a single discount per product from the registration form, the product detail and the inventory row menu.
- One implementation of the discount rules and one implementation of the dialog, reused by the three features.
- Ship the two bundled fixes (search clear action, `Cantidad` → `Stock`).

**Non-Goals:**

- Choosing the discount's start/end dates — duration is fixed to one week from the moment it is applied.
- Showing the discounted price or a discount badge on the inventory row, the home screen or statistics.
- Managing several simultaneous discounts on one product.
- Removing a discount from the inventory row menu (removal is done with the toggle on the detail screen).
- Any backend change.

## Decisions

### 1. Modules and dependency flow

No new Gradle module. The work lands in existing modules and respects the dependency graph:

```
features:product-register ┐
features:product-detail   ├─→ core:domain (Discount, DiscountDraft, use cases, ProductDiscountRepository)
features:product-inventory┘        ↑ implements
        │                     core:data (ProductDiscountRepositoryImpl, mappers) ─→ core:network (DTOs, ProductApiService)
        └─→ core:ui (QhathiaDiscountDialog, QhathiaDiscountToggleRow)
```

*Alternative considered:* a `:features:product-discount` module owning the dialog and a ViewModel. Rejected — three feature modules need the dialog and features must not depend on each other; a dialog is not a navigation destination, so a feature module would only add a forbidden edge.

### 2. Discount rules live in `:core:domain`, the dialog in `:core:ui`

- `Discount(type: DiscountType, value: Double)` with `DiscountType { PERCENTAGE, FIXED_AMOUNT }` and `fun applyTo(price: Double): Double` (never below 0, rounded to cents). `ProductDiscount(id, discount, startsAt, endsAt, active)` represents a stored one.
- `DiscountDraft(price, type, input)` is a pure, immutable editor model: it parses `input`, exposes `resultingPrice`, `isValid`, an error kind and `toDiscount()`. Rules: percentage is a whole number from 1 to 100; fixed amount is > 0, at most two decimals and not greater than `price`.
- `QhathiaDiscountDialog` in `:core:ui` is stateless and takes primitives only (formatted original/resulting price, selected mode, input text, error text, `isApplyEnabled`, `isLoading`, callbacks). It reuses `QhathiaSegmentedControl`, `QhathiaTextField` and `QhathiaButton`. `QhathiaDiscountToggleRow` renders the `Descuento` label, the switch and the optional tappable summary line.

Each feature's `UiState` holds a nullable `DiscountDraft` (dialog open when non-null) and its ViewModel forwards dialog events to the draft. This keeps the rules in one tested place without making `:core:ui` depend on `:core:domain`.

*Alternative considered:* keeping the editing state inside the dialog with `remember`. Rejected — the rules would sit in a composable, be untestable with plain unit tests, and break the MVI contract.

### 3. One discount per product, replace = delete then create

The API has no update endpoint. `SaveProductDiscountUseCase(productId, discount)`:

1. `GET` the product's discounts.
2. `DELETE` every discount returned (so the product never accumulates stale ones).
3. `POST` the new one with `startsAt = clock.instant()` and `endsAt = startsAt + 7 days`.

`RemoveProductDiscountUseCase` does steps 1–2. `GetProductDiscountUseCase` returns the current discount: the `active` one whose `endsAt` is in the future, the latest-ending if several, or `null`.

The clock is injected (`java.time.Clock`, matching the `MutableClock` test utility in `:core:testing`) so the one-week window is testable.

*Alternative considered:* reading `discounts[]` from `GET /products/{id}` instead of the dedicated endpoint. Rejected — the task names the three discount endpoints explicitly, and the inventory row menu has no product detail loaded.

### 4. Registration: discount is created after the product

The product id does not exist until `POST /products/with-images` returns, so the form only keeps the chosen `Discount` in `ProductRegisterUiState`. On submit the ViewModel calls `RegisterProductUseCase`, and on success, if a discount is set, `SaveProductDiscountUseCase(newId, discount)`.

If the product is created but the discount call fails, the app still returns to the inventory (the product exists, so staying on the form would invite a duplicate submit) with a distinct result: "product registered, but the discount could not be applied". The merchant can retry from `Editar descuento`.

Toggle rules on the form: disabled while the price is not valid; switching on opens the dialog; cancelling the dialog with no discount set leaves the toggle off; switching off clears the discount. If the price is later edited so that the stored discount is no longer valid (price cleared/invalid, or a fixed amount greater than the new price), the discount is cleared and the toggle returns to off.

### 5. Detail: the discount is part of the form and is persisted on `Guardar`

On the detail screen the discount behaves like any other form field. `ProductDetailUiState` keeps the discount as loaded (`loadedDiscount`) and the one shown on the form (`discount`). `Aplicar` in the dialog and switching the toggle off only change `discount` and mark the form dirty; nothing is sent until the merchant taps `Guardar`. Leaving with a pending discount change goes through the existing discard-confirmation dialog, and discarding leaves the stored discount untouched.

On save the ViewModel runs, in order:

1. `UpdateProductUseCase` (`PUT /products/{id}`), as today.
2. Only if that succeeds and `discount != loadedDiscount`: `SaveProductDiscountUseCase` when a discount is set, or `RemoveProductDiscountUseCase` when it was turned off.

The product goes first so a failed product update never leaves a discount changed on its own. If step 1 succeeds and step 2 fails, the merchant stays on the form with a "product saved, but the discount could not be updated" message and the form stays dirty; tapping `Guardar` again repeats both steps, which is safe because the `PUT` is a full replace.

- Detail loads the current discount concurrently with the product and categories. A failure is non-fatal: the form is shown and the discount control is disabled with a retry affordance, and saving skips step 2.
- The dialog previews against the price currently typed in the form; the toggle is disabled while that price is invalid. As on registration, editing the price so the pending discount is no longer valid clears it.

*Alternative considered:* persisting the discount immediately on `Aplicar` / toggle off. Rejected by product decision — on the edit form every change, including the discount, must be confirmed with `Guardar`, and the discard dialog already protects against leaving by mistake.

### 5b. Row menu: `Aplicar` is the save

The inventory row menu has no form and no `Guardar`, so there `Aplicar` in the dialog calls `SaveProductDiscountUseCase` directly. `Editar descuento` fetches the current discount, then opens the dialog prefilled (or empty when there is none), using the price from the list row. Success shows a snackbar; the list does not need a re-query because the row does not render discount data.

### 6. Row actions menu

The discount option reads `Registrar descuento` for a product without a discount and `Editar descuento` for one that has it. `GET /api/v1/products` returns no discount data, so that is looked up lazily: opening a row's menu emits `RowActionsOpened(productId)`, the ViewModel calls `GetProductDiscountUseCase` once and remembers the answer in `InventoryUiState.knownDiscounts`. While the lookup is in flight the option is disabled with a spinner; if it fails the option falls back to `Editar descuento` and loads the discount when chosen. A remembered answer is reused to open the dialog without a second request, replaced after a save, and discarded whenever the product list reloads.

*Alternative considered:* fetching every product's discounts right after the list loads, so the label is ready before the menu opens. Rejected — it costs one request per product on every list load for a label most rows never show. If backend adds a discount flag to the list response, the lookup goes away.


`ProductRow` wraps the `⋮` in an `IconButton` (48dp target, content description) that anchors a Material 3 `DropdownMenu`, so the options are drawn from the icon as the task requires. The expanded state is local UI state in the row; the two items emit `ProductClicked(productId)` (existing) and a new `EditDiscountClicked(productId)`.

### 7. Bundled fixes

- **Search clear:** `InventorySearchField` and `OrderSearchField` gain a trailing clear icon button, visible only when the query is not empty, that calls `onQueryChange("")`. Both keep their current look; the two fields differ in shape, so they are edited in place rather than merged into a shared component. The sales-statistics screen has no text search input and is untouched.
- **Label:** `register_quantity_label` and `detail_quantity_label` change from `Cantidad` to `Stock`. The task mentions the registration form; the detail form is the same form in edit mode, so both change to stay consistent.

## Risks / Trade-offs

- [Replace is not atomic: delete succeeds, create fails] → the product is left without a discount. Accepted until backend offers an update or atomic replace endpoint; the app shows an error and the merchant can save again, which re-runs the replace. `SaveProductDiscountUseCase` is the only place that would change.
- [Product registered but discount failed] → handled with the distinct inventory message in Decision 4; no silent loss.
- [Product updated but discount failed on the detail save] → handled in Decision 5: the merchant stays on the form with a specific message and can retry.
- [Price edited on the detail form without touching a stored fixed-amount discount] → covered by the same rule as registration: a discount that no longer fits the new price is cleared from the form, and the removal is persisted on save.
- [Discount expires after a week] → once expired, the detail shows the toggle off, which matches "no current discount". The merchant re-applies to renew.
- [Device clock is wrong] → `startsAt`/`endsAt` derive from the device clock; a badly skewed clock shifts the window. Accepted for now; the backend validates `endsAt > startsAt`.

## Resolved Questions

- **Quick percentage options:** 10 %, 20 % and 50 %, as in the Figma dialog. The task text's 30 % is superseded.
- **Discounts down to zero:** a discount may bring the resulting price to `S/ 0.00` (100 %, or a fixed amount equal to the price), and that is the limit — a percentage above 100 or an amount above the price is rejected, so the resulting price is never negative.
- **Replace strategy:** delete-then-create stays until backend confirms a better option (see Risks).
