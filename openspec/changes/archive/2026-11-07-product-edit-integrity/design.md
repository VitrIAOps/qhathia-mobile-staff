## Context

**F-86.** Both forms evaluate `Discount.fits` on every price keystroke and drop the discount from state (`ProductDetailViewModel.onPriceChanged`, `ProductRegisterViewModel.onPriceChanged`). `parsePrice` returns `null` for `""` and `"150."`, so the discount is lost while typing. On the detail, `isDiscountChanged = discount != loadedDiscount` then turns true, and `persistDiscountChange` calls `removeProductDiscountUseCase`, which deletes the stored discount on the server. The backend keeps discounts in their own table and a `PUT` never touches them (API_REFERENCE §25), so the only thing removing them is the app. The backend does **not** reject a fixed discount larger than the price: the product would be priced at S/ 0. That makes a check before saving mandatory once the form stops dropping the discount.

**F-93 / F-81 / description.** Since Fase 40 (§40), `PUT /products/{id}` keeps every absent field. `""` clears `description`/`brand` and `[]` clears `tags`. `version` in the body enables optimistic locking: a stale value answers `409 PRODUCT_VERSION_CONFLICT` with `currentVersion`, and a write collision in the database answers `409 CONCURRENT_MODIFICATION`. Confirming an order bumps the version, because the stock changes. The app still:
- sends `CreateProductRequestDto` with every field, `stock` always included;
- turns a blank description into `null`, which the `Json` does not encode (`encodeDefaults` is off), so the backend keeps the old one;
- never reads `version`;
- classifies a 409 as `Unknown`.

`ProductDetail.stock == null` (CONSULT) leaves the stock field empty and `isValidQuantity("")` blocks saving.

`problem-detail-errors` gives `NetworkResult.HttpError.problem?.errorCode` and `currentVersion` for every `ProductApiService` call.

Modules (no new module, graph unchanged):

```
features:product-detail ─┐
features:product-register┴─► core:domain ◄── core:data ──► core:network
          │                     ProductUpdate, ProductDetail.version,     UpdateProductRequestDto,
          └─► core:ui           UpdateProductError.Conflict,              ProductDetailDto.version
              QhathiaDiscountToggleRow(warning)  ProductRepository.updateProduct: Result<ProductDetail>
core:testing: FakeProductRepository, TestFixtures
```

## Goals / Non-Goals

**Goals:**
- No price edit can remove a discount, locally or on the server, unless the merchant sees why.
- The product is never saved with a fixed discount larger than its price.
- No save silently overwrites another vendor's edit or undoes a sale.
- Clearing the description works; CONSULT products can be edited without a stock.

**Non-Goals:**
- `Idempotency-Key`, the back lock while saving, and partial saves after a network cut (F-85, F-96: `product-save-safety`).
- `PRODUCT_IN_PENDING_ORDERS` / `PRODUCT_DELETED` on delete and edit (F-83, F-87: `product-save-safety`).
- Price formatting such as `1.23456785E7` (F-82: `money-format`).
- Editing `brand`, `allowDiscountInquiry` or `position` (the form does not expose them).
- Showing a field-by-field diff between the merchant's edits and the other vendor's.

## Decisions

### 1. The discount is plain form state: the price never touches it

`onPriceChanged` only updates `price`, `priceError` and `isDirty`. A derived `isDiscountOverPrice` (`discount != null && parsePrice(price)?.let { !discount.fits(it) } == true`) drives the warning on the row and the check when saving.

- *Why:* any rule that removes the discount while typing goes through intermediate states (`""`, `"150."`, `"1"` on the way to `1500`). Keeping the discount and validating on save is the only rule that does not depend on how the merchant types.
- *Alternative:* keep a "shadow" discount and restore it when the price becomes valid again. Rejected (handoff "No hacer"): two sources of truth, the bug comes back the first time they drift.
- *Alternative:* block Save while the discount is over the price. Rejected: the merchant would not know why Save is greyed out. The handoff asks for the save-time removal with a message.
- With an empty or invalid price the toggle stays **checked and disabled** (`isDiscountToggleEnabled` already requires a valid price), and the summary shows without a final price (`discountedPrice` already returns `null`). Content code is unchanged apart from the new `warning`.

### 2. Over-price check before any request, on save and on submit

In the detail's `save()` and the register's `submit()`, before launching:

```kotlin
if (current.isDiscountOverPrice) {
    _uiState.update { it.copy(discount = null, isDirty = true) }
    emit(ShowError(R.string.detail_discount_removed_over_price))   // register_… on Registro
    return
}
```

The form now differs from `loadedDiscount`, so the *next* save removes the stored discount with the same flow as turning the toggle off. That is an action the merchant saw and confirmed by tapping Save again.

### 3. A dedicated `UpdateProductRequestDto`

```kotlin
@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class UpdateProductRequestDto(
    val name: String,
    val price: Double,
    @EncodeDefault val description: String = "",            // always sent; "" clears
    val stock: Int? = null,                                  // null → not encoded → kept
    val categoryId: String? = null,
    @EncodeDefault val tags: List<String> = emptyList(),     // always sent; [] clears
    val version: Long? = null,                               // null → not encoded → no check
)
```

- The shared `Json` has `encodeDefaults = false` and no `explicitNulls` setting. In kotlinx.serialization a property equal to its default is not encoded unless it is marked `@EncodeDefault`. So a null `stock` or `version` is simply absent from the body, which is what "keep" means in §40.
- `brand`, `allowDiscountInquiry` and `position` are not in the DTO, so they are kept (F-77, and no overwriting of another vendor's flag).
- `CreateProductRequestDto` stays create-only. The `ProductApiServiceTest` from P-1 is moved to the new DTO.

### 4. Domain shape

- `ProductUpdate(name, price, stock: Int?, description: String, categoryId, tags, version: Long?)`. The KDoc says it is a partial update, `stock == null` means "keep", `description == ""` clears and `version == null` skips the check.
- `ProductDetail.version: Long? = null`, mapped from `ProductDetailDto.version`.
- `ProductRepository.updateProduct(...)` and `UpdateProductUseCase` return `Result<ProductDetail>`, the body of the `PUT`, which already decodes as `ProductDetailDto`. The caller gets the new version without another `GET`.
- `UpdateProductError.Conflict(val currentVersion: Long?)`.

### 5. Error classification reads the `errorCode` first

`ProductRepositoryImpl` replaces `Int.toUpdateError()` with `NetworkResult.HttpError.toUpdateError()`:

```kotlin
when {
    code == 404 -> NotFound                                   // incl. PRODUCT_DELETED
    code == 409 && problem?.errorCode == "PRODUCT_VERSION_CONFLICT" -> Conflict(problem.currentVersion)
    code == 409 && problem?.errorCode == "CONCURRENT_MODIFICATION" -> Conflict(null)
    code == 400 || code == 422 -> InvalidInput
    code == 503 -> Network
    else -> Unknown(problem?.detail ?: "HTTP $code")
}
```

It is used by every product call that maps to `UpdateProductError` (get, update, delete, photos). Any other 409 stays `Unknown`. `PRODUCT_IN_PENDING_ORDERS` gets its own case in `product-save-safety`. The `when` over `UpdateProductError` in `ProductDetailViewModel.toMessageRes()` gains a `Conflict` branch (generic copy, never reached from `save()`, which opens the dialog instead). The inventory's check `== NotFound` needs no change.

### 6. What the detail remembers and sends

`ProductDetailUiState` gains:
- `loadedVersion: Long?`;
- `loadedQuantity: String`, the stock text as seeded;
- `isConsult: Boolean`, true when the product loaded with `stock == null`;
- `conflict: SaveConflict?` (`data class SaveConflict(val currentVersion: Long?)`), non-null while the dialog is open. One nullable value instead of the handoff's `showConflictDialog` + `conflictVersion` pair, following `discountDraft`.

Building the update:

```kotlin
val stockUntouched = current.quantity.trim() == current.loadedQuantity
val keepConsult = current.isConsult && current.quantity.isBlank()
val stock = if (stockUntouched || keepConsult) null else parseQuantity(current.quantity) ?: return
ProductUpdate(..., stock = stock, description = current.description.trim(), version = version)
```

`isSaveEnabled` replaces `isValidQuantity(quantity)` with `isValidQuantity(quantity) || (isConsult && quantity.isBlank())`.

### 7. Conflict dialog: "Guardar lo mío" adopts the server version, then saves

On `UpdateProductError.Conflict`, `save()` sets `isSaving = false, conflict = SaveConflict(error.currentVersion)` and emits nothing.

New events:
- `ConflictReload` → `conflict = null`, then `load()`. `load()` re-seeds every field, photos and discount, which is "Ver lo nuevo".
- `ConflictOverwrite` → `conflict = null` and `loadedVersion = currentVersion`, then `save()`. When `currentVersion` is null (`CONCURRENT_MODIFICATION`), the product is read first and its `version` is adopted. If that read fails, the error message is shown and nothing is sent.
- `ConflictDismissed` → `conflict = null`.

Adopting the version in state, instead of passing an override to `save()`, means a network failure on the overwrite can be retried with plain Save without another conflict. The merchant already chose to overwrite. "Guardar lo mío" goes through the same `isSaveEnabled` and over-price checks and the same stock rule, so it never sends an untouched `stock`.

The dialog is an `AlertDialog` in `ProductDetailContent`, next to the other dialogs. The three actions are stacked (`Ver lo nuevo`, `Guardar lo mío`, `Seguir editando`) so they fit with large fonts. It is only shown while `conflict != null`, which every action clears before any request.

### 8. Keeping the version current when the merchant stays on the form

After a successful `PUT`, the first thing `save()` does is `loadedVersion = updated.version` and `loadedQuantity = updated.stock?.toString().orEmpty()`. When photos or the discount then fail, the form stays open. The handoff does not know whether those writes bump `version` (they are child collections and Hibernate may increment it), so those branches read the product again and adopt its `version`/stock:
- the photo branch already calls `getProductDetailUseCase` in `resyncPhotos`, and now also returns the read product so the caller sets `loadedVersion`;
- the discount branch adds one `getProductDetailUseCase`.

If that read fails, the `PUT`'s version is kept. In the worst case the retry gets a conflict dialog, never a silent overwrite.

`isConsult` is refreshed from the same reads, so a CONSULT product that was given a stock now requires one.

### 9. `QhathiaDiscountToggleRow(warning: String? = null)`

Shown below the summary line in `colorScheme.error`, `bodySmall`, in a `liveRegion = Polite` node so TalkBack announces it when it appears. Both forms pass `stringResource(R.string.…_discount_over_price)` when `isDiscountOverPrice`. The default `null` keeps every other caller unchanged.

## Risks / Trade-offs

- [Product saved at S/ 0 if the check is skipped] → The check runs in `save()`/`submit()` before any request, and on "Guardar lo mío" too because it calls `save()`. Covered by VM tests on both forms.
- [Stale `loadedVersion` after a photo or discount write] → Re-read in the stay-on-form branches (decision 8). The fallback is a conflict dialog, not data loss.
- ["Guardar lo mío" overwrites the other vendor's fields] → That is the explicit choice the dialog offers. `stock` is still omitted when untouched, so a sale is never undone.
- [`@EncodeDefault` on `description` means it is always sent] → Intended: the form always shows the description, so sending it is a write of what the merchant sees, and `""` clears it.
- [Tests that encode the old behavior] → `ProductDetailViewModelTest` "a price change that no longer fits the discount removes it from the form", `ProductRegisterViewModelTest` "a price change that no longer fits the discount removes it" and "clearing the price removes the discount and locks the toggle" are inverted, not deleted.
- [Branch dependency] → This branch starts from `feature/problem-detail-errors`. Merge that PR first, or rebase onto `develop` once it is merged.

## Migration Plan

Single merge, no flag, no persisted data. Rollback is reverting the commit. The backend already accepts both shapes, and without `version` it behaves as before.

## Open Questions

_None blocking._ Whether photo or discount writes bump `version` is handled by re-reading (decision 8) rather than assumed.
