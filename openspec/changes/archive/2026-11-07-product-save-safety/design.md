## Context

**Registration.** `ProductRegisterViewModel.submit()` sends one multipart `POST /products/with-images` with no `Idempotency-Key`. While submitting:
- `ProductRegisterScreen` disables its `BackHandler` (`isDirty && !isSubmitting`), so the system back pops the screen and cancels the `viewModelScope` coroutine;
- `CancelRequested` ignores `isSubmitting`;
- the header chevron is always enabled;
- the scrim does not consume touches.

The backend (§40) accepts `Idempotency-Key: <uuid>` and answers a replay with `200`, the same product and `Idempotent-Replayed: true`, without uploading photos again or using plan quota. `onImagesPicked` has no limit, although the detail already caps photos at `MAX_PHOTOS = 10`.

**Detail save chain.** `save()` runs `PUT`, then photo deletes and the upload, then `persistDiscountChange`. After `product-edit-integrity`, the form adopts the server version after the `PUT` and on the stay-on-form branches. Problems that remain:
- the chain can be left midway (same back, chevron and scrim gaps as registration);
- partial failures are only a short snackbar;
- a retry repeats the `PUT`;
- `SaveProductDiscountUseCase` deletes every discount before creating the new one, so a failed create leaves none;
- when the stored discount cannot be re-read after a failure, `loadedDiscount` keeps the old value, `isDiscountChanged` may be false, and the discount is never created again.

The backend allows several discounts per product (no unique index) and prices with the cheapest (`DiscountCalculator`).

**Delete.** Since Fase 39, `DELETE /products/{id}` on a product in pending orders answers `409 PRODUCT_IN_PENDING_ORDERS` with `detail` and `pendingOrders[]`; `?force=true` deletes anyway. `ProductRepositoryImpl` maps that 409 to `Unknown`, so both delete flows show a generic error. The detail's confirmation uses the edited `name` and never mentions unsaved edits. A `404` (`PRODUCT_DELETED`) while saving only shows a snackbar. `ProductDetailDto.active` is decoded but dropped by the mapper.

Modules (no new module; graph unchanged):

```
features:product-register ─┐
features:product-detail ───┼─► core:domain ◄── core:data ──► core:network
features:product-inventory ┘     │                 (header, ?force, 409 mapping, active)
        └─► core:ui (QhathiaDeleteProductDialog)   core:common (MAX_PRODUCT_PHOTOS)
core:testing: fakes and fixtures
```

## Goals / Non-Goals

**Goals:**
- A retried or interrupted registration never creates a second product.
- A save is never abandoned midway by back, and a partial save is visible until it is resolved.
- A retry sends only what is still pending.
- Replacing a discount never leaves the product without one.
- Products in pending orders can be deleted on purpose; deleted products are never edited.

**Non-Goals:**
- An atomic "save everything" request (there is none in the backend).
- The order side of F-83 (`productActive`, `PRODUCT_NOT_SELLABLE` on confirm). It goes in `refresh-on-return`.
- A debug-only network fault injector for QA. The handoff lists it as optional; QA can use a proxy.
- `Idempotency-Key` on discount or photo writes (the backend does not support it).

## Decisions

### 1. One idempotency key per registration form, in `SavedStateHandle`

```kotlin
private val idempotencyKey: String =
    savedStateHandle.get<String>(KEY_IDEMPOTENCY) ?: UUID.randomUUID().toString().also {
        savedStateHandle[KEY_IDEMPOTENCY] = it
    }
```

- It travels as `NewProduct.idempotencyKey` → `toCreateRequestDto` is unaffected → `ProductApiService.createProductWithImages(product, images, idempotencyKey)`, which adds `header("Idempotency-Key", key)`.
- *Why `SavedStateHandle`:* the "kill the app after Agregar" case in F-85 comes back with a new ViewModel. Recreating the key would duplicate. The handle survives process death for the same back-stack entry.
- *Why not regenerate after success:* the screen closes on success. After a failure, the same form is the same product, so it keeps the key.
- A `200` replay already decodes as `ProductResponseDto` (`expectSuccess` treats every 2xx as success). The code path is unchanged; it gets a repository test.

### 2. Nobody leaves while a request is in flight

Same rule on both forms (`isSubmitting` on Registro, `isSaving` or `isDeleting` on Detalle):
- `BackHandler(enabled = busy || isDirty) { if (!busy) onEvent(CancelRequested) }`. Back is swallowed while busy.
- The ViewModel ignores `CancelRequested` and the bell event while busy.
- The headers get `backEnabled = !busy`.
- The scrim is a shared `QhathiaSavingOverlay` in `core:ui`: `.clickable(indication = null, interactionSource = …) {}` and `semantics { contentDescription = "Guardando producto…"; liveRegion = Polite }`. It replaces the touch-through `Box` in both forms.

### 3. Partial save is state, not a snackbar

`ProductDetailUiState.partialSave: PartialSave?` (`enum class PartialSave { PHOTOS, DISCOUNT }`):
- set on the two stay-on-form branches of `save()`;
- cleared at the start of a save that ends in `NavigateBackSaved`, and on reload.

`EditForm` shows a banner at the top in `errorContainer`/`onErrorContainer` with `liveRegion = Polite`: "Se guardaron los datos del producto, pero no el descuento. Toca Guardar para reintentar." / "…no las fotos…". The `ShowError` effect for those two cases becomes a long snackbar: `ProductDetailUiEffect.ShowError(messageRes, long = false)`, and the screen maps `long` to `SnackbarDuration.Long`. When leaving with `partialSave != null`, the discard dialog uses `detail_discard_message_partial_discount` / `_photos`.

### 4. Retry skips a `PUT` that already went through

After a successful `PUT`, the ViewModel keeps `lastAppliedUpdate: ProductUpdate?`, the update it just sent. It is kept in the ViewModel, not in UiState: it never renders. On the next `save()`, the `ProductUpdate` is built as usual. If it is the same edit as `lastAppliedUpdate` (`isSameEditAs`), the `PUT` is skipped and the chain starts at photos, with the photo/discount steps reading the current state. "Same edit" means:
- *`version` ignored:* it changed after the first `PUT`;
- *`stock` ignored only when untouched since then:* after the first `PUT` the baseline moved (`adopting()`), so an untouched stock is now `null` while the first update may have carried a number. A stock the merchant changed again is non-null and different, so the `PUT` is sent.

Any edit to the product fields makes them differ, and the `PUT` is sent as usual. `lastAppliedUpdate` is cleared by `load()` (conflict reload) and when the save completes.

- *Alternative:* a `productSaved` boolean. Rejected: it cannot tell whether the merchant edited the name after the partial failure.

### 5. Discount replace: create, then delete the others

```kotlin
val existing = repository.getDiscounts(productId).getOrElse { return Result.failure(it) }
val created = repository.createDiscount(productId, discount, startsAt, endsAt).getOrElse { return Result.failure(it) }
existing.forEach { repository.deleteDiscount(productId, it.id).onFailure { e -> return Result.failure(e) } }
return Result.success(created)
```

- *Why:* a failed create now leaves the old discount in place. A failed delete leaves two, and the kiosk shows the cheaper. The form reports a partial save and the next save cleans up: it reads both, creates again and deletes both old ones.
- *Trade-off:* for a moment the product can carry two discounts, which can price it lower than the merchant intended. Acceptable: the alternative is a full-price window, which is what the regression reports as harmful.
- The detail's failure branch re-reads the stored discount. If that read fails, it sets `loadedDiscount = null`, so `isDiscountChanged` stays true and the retry creates the form's discount again. (Previously it kept the stale value.)
- Every caller (Detalle, Registro, the Inventario quick discount) gets the new order. The tests asserting `delete…, create…` are updated to `create…, delete…`.

### 6. `UpdateProductError.HasPendingOrders` and `force`

- `UpdateProductError.HasPendingOrders(val detail: String?, val orderCodes: List<String>)`, mapped in `ProductRepositoryImpl.toUpdateError()` from `409` + `PRODUCT_IN_PENDING_ORDERS`, reading `problem.detail` and `problem.pendingOrders[].orderCode`.
- `ProductApiService.deleteProduct(id, force: Boolean = false)` adds `parameter("force", true)` only when forced. `ProductRepository.deleteProduct(id, force = false)` and `DeleteProductUseCase(productId, force = false)` pass it on.
- Both ViewModels keep the confirmation open and store the error itself as `pendingOrders: UpdateProductError.HasPendingOrders?` (Inventario keeps `deleteCandidate`, Detalle keeps `showDeleteDialog`). The Content turns it into text: the server's `detail`, or `delete_product_pending_orders_fallback` listing `#orderCodes` when `detail` is null, so no string is built in a ViewModel. The confirm action sends `force = pendingOrders != null`. Dismissing clears it.

### 7. `QhathiaDeleteProductDialog(pendingOrdersDetail: String? = null, hasUnsavedChanges: Boolean = false)`

- With `pendingOrdersDetail`: the text is that detail, and the confirm button reads `Eliminar igual`.
- Otherwise: `delete_product_message` or, with `hasUnsavedChanges`, `delete_product_message_unsaved`.
- The detail passes `productName = loadedName` (new `ProductDetailUiState.loadedName`, seeded in `seededFrom`) and `hasUnsavedChanges = isDirty`. Inventario passes neither flag.

### 8. A product gone while saving leaves the form

- In `save()`, `UpdateProductError.NotFound` from the `PUT` emits `NavigateBackDeleted(alreadyDeleted = true)`.
- `persistPhotoChanges` and `persistDiscountChange` failures are inspected the same way. A `NotFound` from `UpdateProductPhotosUseCase` (which already passes `UpdateProductException` and already treats a missing photo as deleted), or a `DiscountError.NotFound` from the discount use cases, leaves too, and no further step runs.
- `DiscountError.NotFound` covers "the product is gone" and "that discount is gone" alike. So `SaveProductDiscountUseCase` and `RemoveProductDiscountUseCase` treat a `404` on `deleteDiscount` as already deleted, which is what the photo use case does for media. A `NotFound` that escapes them then comes from `getDiscounts` or `createDiscount`, which only answer `404` when the product is gone.
- `ProductDetail.active: Boolean = true`, mapped from `ProductDetailDto.active`. `seededFrom` sets `isDeleted = !product.active`. The Content shows a `CenteredSlot` with "Este producto fue eliminado" and `Volver` (`CancelRequested`, the form is pristine) instead of `EditForm`.

### 9. Registration photo limit

`MAX_PRODUCT_PHOTOS = 10` moves to `core:common/validation/ProductValidators.kt`. The detail's `MAX_PHOTOS` becomes an alias to it so its code and tests stay unchanged. `onImagesPicked` copies the detail's logic: dedupe, then `take(MAX - images.size)`, and if any were dropped, `ShowError(register_photos_limit_reached)`. The format error keeps priority when both apply, as in the detail. `ProductRegisterUiState.canAddPhotos` is passed as `QhathiaPhotoStrip(canAdd = …)`, `isSubmitEnabled` requires `images.size <= MAX`, and the subtitle adds "Máximo 10 fotos". No `maxItems` on the picker contract: it must be greater than 1 and breaks when one slot is left.

## Risks / Trade-offs

- [Two discounts for a moment] → The kiosk prices with the cheaper one. The form flags the partial save, and the next save deletes the extra.
- [Skipping the `PUT` hides a server-side change made in between] → The photo and discount steps do not depend on the product fields. A conflicting product edit would still be caught on the merchant's next real field change, through `version`.
- [`SavedStateHandle` key reused across two different products] → The key belongs to one registration screen instance. A new "Agregar producto" is a new back-stack entry with a fresh handle.
- [Swallowing back can trap the merchant on a hung request] → The client has a 30 s request timeout (`KtorClientFactory`), after which the failure path re-enables everything.
- [Order of steps in tests] → `FakeProductDiscountRepository.calls` already records the order; the affected assertions are listed in the tasks.

## Migration Plan

Single merge, no flag. Rollback is reverting the commit. The backend accepts requests with or without the header and the `force` parameter.

## Open Questions

_None._
