## Context

`GET /api/v1/products` has returned three new fields per row since 2026-10-18 (`docs/android/api-contract.md` §25 and §26):

| Field | Type | Meaning |
|---|---|---|
| `stock` | `int \| null` | Same value as `products.stock` on the detail. `null` for legacy and "consult" products |
| `discountedPrice` | `number \| null` | Price with the winning discount in effect applied. `null` = no discount in effect |
| `discountEndsAt` | ISO-8601 `\| null` | End of that discount |

The server now owns the discount rules: when several discounts are in effect the cheapest wins, the window is `startsAt <= now < endsAt`, rounding is `HALF_UP` to 2 decimals, and the result never goes below 0.

Current state in the staff app:

- `ProductDto` (`core:network`) has none of the three fields. `Product` (`core:domain`) neither.
- `ProductRow.kt:131` renders `mockedStock(product)`, a value derived from the product id's hash (`ProductRow.kt:246`).
- The row renders `product.price` only.
- `InventoryViewModel.saveDiscount()` saves the discount and updates `knownDiscounts`, but does not re-read the list. `loadProducts()` always sets `isLoading = true`, which swaps the list for a spinner.
- Product Detail and Product Registration compute `discountedPrice` locally (`ProductDetailUiState.discountedPrice`, `ProductRegisterUiState.discountedPrice`) from the form's price and the discount draft.

Modules involved (all existing; no new module):

```
features:product-inventory → core:domain ← core:data → core:network
                           → core:ui
core:testing (fixtures)    → core:domain
```

## Goals / Non-Goals

**Goals:**

- The Inventory row shows the real stock and the server's discounted price.
- Delete `mockedStock` and every reference to the stock being mocked.
- After a discount is saved from the row menu, the row reflects it without a manual refresh.

**Non-Goals:**

- Product Detail and Product Registration keep computing their resulting price locally (see Decision 3).
- Rendering `discountEndsAt` ("termina en …"). It is carried to the domain so a later change can show it, but there is no design for it on the row yet.
- Using `discountedPrice` to pre-resolve the row menu's `Registrar descuento` / `Editar descuento` label. `null` means "no discount *in effect*"; a scheduled discount that starts tomorrow still exists, so the label must keep coming from `GET /products/{id}/discounts`.
- The Discovery app's part of BUG-013 (catalog grid, "Descuentos" tab), tracked in its own repo.
- Low-stock styling on the row (e.g. colouring `stock <= 5`). Not requested; can follow later.

## Decisions

### 1. All three fields are optional with `null` defaults, end to end

`ProductDto` declares `val stock: Int? = null`, `val discountedPrice: Double? = null`, `val discountEndsAt: String? = null`. `Product` carries `stock: Int?`, `discountedPrice: Double?`, `discountEndsAt: Instant?`.

- `null` is meaningful for all three (unknown stock / no discount in effect), so it must survive to the UI instead of being collapsed to `0` or `price`.
- Defaults keep deserialization working against an older backend and keep existing DTO tests valid.
- `discountEndsAt` is parsed to `java.time.Instant` in the mapper (`core:data`), the same way other ISO timestamps are mapped in this project; an unparsable value maps to `null` instead of failing the whole list. `core:domain` stays free of Android dependencies (`java.time` is JVM).
- `price` stays a `Double`, as it is today for `Product`. Switching the row to `BigDecimal` is out of scope.

*Alternative considered:* a non-null `stock` defaulting to `0`. Rejected: it would print "Sin existencias" for every legacy or "consult" product — the same kind of invented number this change removes.

### 2. The row trusts `discountedPrice` and does no arithmetic

The row shows `price` struck through plus `discountedPrice` when the latter is non-null, and plain `price` otherwise. It does not look at `price > discountedPrice` or at any discount object.

- The handoff is explicit that repeating the arithmetic on the client is how BUG-013 happened. One source, the server.
- Styling follows the existing `QhathiaDiscountDialog`: original price in `onSurfaceVariant` with `TextDecoration.LineThrough`, discounted price emphasized (bold, `onSurface`). The two prices sit in the same row slot where the price is today; the stock text stays right-aligned.
- Accessibility: the price group gets a merged `contentDescription` using new strings, e.g. "Precio original S/ 35.90, precio con descuento S/ 30.90", so TalkBack does not read two bare numbers. This mirrors `discount_original_price_cd` / `discount_resulting_price_cd` in `core:ui`, but the strings live in `features:product-inventory` because they are row-specific.

### 3. Product Detail and Registration keep their local computation

The handoff suggested making Product Detail read the server's `discountedPrice`. Checking the code shows that would be wrong here: on both screens the resulting price is a **live preview of an unsaved form**. The merchant can change the price field or the discount draft, and the shown result must follow those edits before anything is saved (spec `product-detail` → "Discount shown on product detail": "the form's price with the discount applied"). The server only knows the saved values.

So `ProductDetailDto` is not changed, and `DiscountDraft`/`Discount.applyTo` stay as they are. This does not reintroduce BUG-013: that bug is about **read-only** surfaces showing full price, and the Inventory row is the only read-only price surface in the staff app.

*Alternative considered:* show the server value until the merchant edits, then switch to the local value. Rejected: two sources for one number on the same screen, and the values can differ (e.g. several discounts in effect, where the server picks the cheapest while the form edits one).

### 4. Quiet reload after a discount save from the row menu

After `saveProductDiscountUseCase` succeeds, `InventoryViewModel` re-reads the list with the active filters through a quiet path:

- It does **not** set `isLoading = true`, so the `LazyColumn` keeps its rows and scroll position (rows are keyed by product id).
- On success it replaces `loadedProducts` and recomputes `visibleProducts` with the current search query, like `loadProducts()` does.
- On failure it changes nothing: no error state, no extra snackbar. The discount was saved and already confirmed; a stale price until the next refresh is the lesser problem.
- It leaves `knownDiscounts` untouched: nothing outside this screen changed them, and the entry just saved is already accurate. (`loadProducts()` keeps clearing them, because it follows edits made elsewhere.)
- Every products request, regular or quiet, cancels the one in flight. Otherwise a quiet re-read still running when the merchant changes a filter could land after the filtered response and overwrite it. If a quiet fetch replaces a regular load still in flight, it takes over that load's loading and error handling, so the spinner is never left on.

Implementation shape: extract the shared "fetch with current filters and apply" step from `loadProducts()` into a private function with a `showLoading: Boolean` (or a separate `reloadProductsQuietly()`), so the filter/search logic is not duplicated.

*Alternative considered:* update the row locally with `Discount.applyTo(price)`. Rejected for the same reason as Decision 2: it puts the arithmetic back on the client, and with several discounts in effect the local value could differ from what the server, the Discovery app and the PDF show.

*Alternative considered:* call the existing `refresh()`. Rejected: it blanks the list with a spinner right after the dialog closes, which reads as "something went wrong".

### 5. Stock text rules live in the row

`ProductRow` decides the stock label:

| `stock` | Shown |
|---|---|
| `null` | nothing |
| `0` | `Sin existencias` (new string `inventory_out_of_stock`, en: "Out of stock") |
| `n > 0` | `inventory_stock_format` ("%1$d stock") |

"Sin existencias" matches the wording already used on Inicio (`home_out_of_stock`) and keeps a stock-out from reading like a low number. The string is duplicated in `features:product-inventory` because feature modules cannot depend on each other.

## Risks / Trade-offs

- **[Risk] Backend sends `discountedPrice >= price` (e.g. a 0-soles discount).** → The row would show the same number twice, one struck through. Accepted: the server owns the rule, and hiding it on the client would be arithmetic by another name. If it shows up, it is a backend bug to report.
- **[Risk] An older backend without the fields.** → All three default to `null`: no stock shown, no discount shown. Nothing invented. The fields are already in production, so this only matters for local backends.
- **[Trade-off] The quiet reload costs one extra `GET /products` per discount save.** → Acceptable: the save is a rare, deliberate action and the list request is the one the screen already makes.
- **[Trade-off] `discountEndsAt` is mapped but not shown.** → Small dead field in the domain until a follow-up renders it. Keeping the mapping now avoids a second DTO/domain change later and is covered by a mapper test.
- **[Risk] Previews and tests that relied on `mockedStock` producing numbers.** → `TestFixtures.product(...)` gains `stock`/`discountedPrice` parameters; previews set explicit values to show all three stock states and both price states.
