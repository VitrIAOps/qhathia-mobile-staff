## Why

The HIGH findings of the 2026-11-03 QA regression (`docs/handoff/android-regresion-2026-11-03.md` §3.3) leave products duplicated, half-saved or undeletable. The backend already does its part (Fases 39–40):

- **F-85** — Back during "Agregar" leaves the screen. That cancels the request (nginx 499), but the backend still creates the product, so a retry duplicates it. The app sends no `Idempotency-Key`. The system back, the chevron and the loading scrim all let the merchant out mid-request.
- **F-96** — "Guardar" is a chain of separate requests: the `PUT`, then the photos, then the discount. Back also works mid-chain. A failure after the `PUT` is reported only in a short snackbar. A retry repeats the `PUT`. Replacing the discount deletes the old one *before* creating the new one, so a failed create leaves the product with no discount at all.
- **F-83** — With Fase 39, deleting a product that is in a pending order answers `409 PRODUCT_IN_PENDING_ORDERS`. The app shows "No se pudo eliminar el producto", so it can no longer delete such a product at all. It cannot send `?force=true`.
- **F-87** — Saving a product deleted elsewhere shows "Este producto ya no existe" but keeps the merchant on the form. A deleted product (`active: false`) still opens as editable.
- **F-97** — "Eliminar producto" with unsaved edits names the *edited* name and does not say the edits will be lost.
- **F-78** — Registro accepts 11 photos and then fails with a generic error. The backend limit is 10.

## What Changes

- **Duplicate-safe registration (F-85).** Each registration form gets one `Idempotency-Key` (a UUID kept in `SavedStateHandle`), sent on `POST /products/with-images` and reused on every retry. A replay `200` is handled like a `201`. While submitting, system back, the chevron and the bell do nothing, and the scrim consumes touches and announces "Guardando producto…".
- **No leaving mid-save on the detail (F-96).** The same rule applies to the detail while saving: back, chevron and scrim.
- **Visible partial save.** When the product was saved but its photos or discount were not, the form shows a persistent banner at the top: "Se guardaron los datos del producto, pero no el descuento. Toca Guardar para reintentar." (or "…no las fotos…"). The snackbar becomes long. Leaving in that state, the discard dialog says the missing part will not be saved.
- **Retry only what is pending.** When the product fields are the same as the last successful `PUT`, a new save skips the `PUT` and sends only the photos and/or the discount.
- **Discount never lost on replace.** `SaveProductDiscountUseCase` creates the new discount first and only then deletes the others. If a delete fails, the product has two discounts for a while: the kiosk applies the cheaper one, and the form reports a partial save. If, after a failed discount step, what the server stores cannot be read, the form assumes there is no discount, so the retry creates it again.
- **Delete with pending orders (F-83).** On `409 PRODUCT_IN_PENDING_ORDERS`, both delete confirmations (Inventario and Detalle) stay open, show the server's `detail` and offer "Eliminar igual", which sends `DELETE ?force=true`, and "Cancelar".
- **Product deleted elsewhere (F-87).** A `404` while saving the product, its photos or its discount returns to Inventario with "Este producto ya había sido eliminado". A product loaded with `active: false` shows "Este producto fue eliminado" and a `Volver` button instead of the form.
- **Delete with unsaved edits (F-97).** The detail's confirmation names the product as loaded and adds "También se perderán los cambios que no guardaste."
- **Photo limit on Registro (F-78).** At most 10 photos (`MAX_PRODUCT_PHOTOS` moved to `core:common` and shared by both forms). Extra picks are dropped with "Solo se pueden agregar 10 fotos; algunas no se agregaron", the add tile is disabled at 10, and a hint says "Máximo 10 fotos".

## Capabilities

### New Capabilities

_None._

### Modified Capabilities

- `product-registration`: "Product registration orchestration" (`Idempotency-Key`, replay), "Registration feedback and navigation" (no leaving while submitting), "Image selection restricted to JPG and PNG" (10-photo limit).
- `product-detail`: "Product detail load" (deleted product), "Product update" (404 leaves; no leaving while saving), "Discard guard" (partial-save wording), "Discount persisted on save" (partial banner, retry only the discount, unreadable stored discount), "Photo changes persisted on save" (banner, retry without `PUT`), "Delete the product from the edit screen" (pending orders, loaded name, unsaved-edits wording).
- `product-inventory`: "Delete a product from the inventory list" (pending orders, "Eliminar igual").
- `product-discounts`: "Save a discount with replace semantics" (create first, then delete the others), "Remove a discount" (a `404` on delete counts as already removed).

## Impact

- **`core:common`** — `MAX_PRODUCT_PHOTOS` in `ProductValidators.kt`.
- **`core:network`** — `ProductApiService.createProductWithImages(..., idempotencyKey)` sends the header; `deleteProduct(id, force)` sends `?force=true`.
- **`core:domain`** — `NewProduct.idempotencyKey`, `ProductDetail.active`, `UpdateProductError.HasPendingOrders(detail, orderCodes)`, `ProductRepository.deleteProduct(id, force)`, `DeleteProductUseCase(force)`, `SaveProductDiscountUseCase` (create, then delete the rest).
- **`core:data`** — `ProductRepositoryImpl` (header, `force`, `409 PRODUCT_IN_PENDING_ORDERS`), `ProductMappers` (`active`).
- **`core:ui`** — `QhathiaDeleteProductDialog(pendingOrdersDetail, hasUnsavedChanges)`, strings.
- **`core:testing`** — `FakeProductRepository` (force, idempotency key), `FakeProductDiscountRepository` (call order), `TestFixtures`.
- **`features:product-register`** — ViewModel (`SavedStateHandle`, key, photo limit, cancel while submitting), Screen (`BackHandler`), Header (`backEnabled`), Content (scrim), `ImagePickerSection` (`canAdd`, hint), strings.
- **`features:product-detail`** — UiState (`partialSave`, `lastAppliedUpdate`, `loadedName`, `isDeleted`, pending-orders warning), ViewModel, Screen, Header, Content (banner, deleted state, scrim), strings.
- **`features:product-inventory`** — ViewModel and Content for the pending-orders confirmation.
- **Depends on** `product-edit-integrity` (branch `feature/product-edit-integrity`, which builds on `problem-detail-errors`). This branch is cut from it.
- **Not in this change:** the order side of F-83 (`productActive` / "Producto eliminado del inventario" in Solicitudes, `409 PRODUCT_NOT_SELLABLE` on confirm). It goes with the order detail in `refresh-on-return`, next to `listPriceSnapshot`.
