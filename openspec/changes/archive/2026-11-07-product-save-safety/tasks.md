## 1. Common (`core:common`)

- [x] 1.1 Add `MAX_PRODUCT_PHOTOS = 10` to `validation/ProductValidators.kt` with KDoc (backend per-product limit)

## 2. Network (`core:network`)

- [x] 2.1 `ProductApiService.createProductWithImages(product, images, idempotencyKey: String)`: send `Idempotency-Key`; KDoc on the `200` replay
- [x] 2.2 `ProductApiService.deleteProduct(id, force: Boolean = false)`: `parameter("force", true)` only when forced
- [x] 2.3 `ProductApiServiceTest`: the header is sent on create; `?force=true` is present only when forced

## 3. Domain (`core:domain`)

- [x] 3.1 `NewProduct.idempotencyKey: String`; `ProductDetail.active: Boolean = true`; KDocs
- [x] 3.2 `UpdateProductError.HasPendingOrders(detail: String?, orderCodes: List<String>)`; `toMessageRes()`-style `when`s that become non-exhaustive get a branch
- [x] 3.3 `ProductRepository.deleteProduct(id, force = false)` and `DeleteProductUseCase(productId, force = false)`
- [x] 3.4 `SaveProductDiscountUseCase`: read, create, then delete the previous ones (design decision 5); rewrite the KDoc
- [x] 3.5 `SaveProductDiscountUseCase` and `RemoveProductDiscountUseCase`: a `DiscountError.NotFound` on `deleteDiscount` counts as deleted
- [x] 3.6 `ProductDiscountUseCasesTest`: new order (`get, create, delete…`); create fails → nothing deleted; delete fails after create → failure; a 404 on delete is ignored (save and remove)

## 4. Data (`core:data`)

- [x] 4.1 `ProductRepositoryImpl.registerProduct` passes `product.idempotencyKey`; `deleteProduct` passes `force`
- [x] 4.2 `toUpdateError()`: `409` + `PRODUCT_IN_PENDING_ORDERS` → `HasPendingOrders(problem.detail, problem.pendingOrders.mapNotNull { it.orderCode })`
- [x] 4.3 `ProductMappers`: `ProductDetailDto.toDomain()` maps `active`
- [x] 4.4 Tests: register sends the key and a `200` replay returns the id; delete forwards `force`; the pending-orders 409 maps to `HasPendingOrders` with detail and codes (replacing the change-1 test that expected `Unknown`); `active` is mapped

## 5. Testing fixtures (`core:testing`)

- [x] 5.1 `FakeProductRepository`: record `lastDeleteForce`; `deleteResults` queue for "409 then 204"; keep `registeredProduct` (now with the key)
- [x] 5.2 `TestFixtures.productDetail(active = true)`; fix every `NewProduct(...)` construction in tests

## 6. Shared UI (`core:ui`)

- [x] 6.1 `QhathiaDeleteProductDialog(pendingOrdersDetail: String? = null, hasUnsavedChanges: Boolean = false)` per design decision 7; previews for both
- [x] 6.2 Strings `values` / `values-en`: `delete_product_message_unsaved`, `delete_product_confirm_force` ("Eliminar igual"), `delete_product_pending_orders_fallback` ("Este producto está en pedidos pendientes (%1$s). Confírmalos o recházalos antes, o elimínalo igual."), `saving_product_cd` ("Guardando producto…")

## 7. Product registration (`features:product-register`)

- [x] 7.1 `ProductRegisterViewModel`: inject `SavedStateHandle`; create or restore the idempotency key; pass it in `NewProduct`
- [x] 7.2 `CancelRequested` and `OrderRequestsRequested` are ignored while `isSubmitting`
- [x] 7.3 `ProductRegisterScreen`: `BackHandler(enabled = isSubmitting || isDirty) { if (!isSubmitting) … }`
- [x] 7.4 `ProductRegisterHeader(backEnabled)`, passed `!isSubmitting`; the scrim consumes touches and carries `saving_product_cd` as a polite live region
- [x] 7.5 Photo limit (design decision 9): `onImagesPicked` takes what fits and emits `register_photos_limit_reached`; `canAddPhotos` → `QhathiaPhotoStrip(canAdd)`; `isSubmitEnabled` requires `images.size <= MAX_PRODUCT_PHOTOS`; subtitle mentions the maximum
- [x] 7.6 Strings `values` / `values-en`: `register_photos_limit_reached`, updated `register_photos_subtitle` with "Máximo 10 fotos"
- [x] 7.7 Tests: the key is reused across two submits and survives a new ViewModel with the same `SavedStateHandle`; cancel while submitting emits nothing (with a gated register); 11 picks keep 10 and emit the message; at 10 `canAddPhotos` is false

## 8. Product detail (`features:product-detail`)

- [x] 8.1 `MAX_PHOTOS` becomes `MAX_PRODUCT_PHOTOS` from `core:common` (keep the name as an alias, or replace the uses)
- [x] 8.2 UiState: `partialSave: PartialSave?` (new enum), `loadedName`, `isDeleted`, `pendingOrders: HasPendingOrders?`; KDoc
- [x] 8.3 `ProductDetailUiEffect.ShowError(messageRes, long: Boolean = false)`; the Screen maps `long` to `SnackbarDuration.Long`
- [x] 8.4 `seededFrom`: `loadedName`, `isDeleted = !product.active`, `partialSave = null`
- [x] 8.5 Busy guard (design decision 2): `CancelRequested` and `OrderRequestsRequested` ignored while saving; Screen `BackHandler`; `ProductDetailHeader(backEnabled)`; the scrim consumes touches with `saving_product_cd`
- [x] 8.6 `save()`: keep `lastAppliedUpdate` after a successful `PUT`; skip the `PUT` when the new update equals it ignoring `version` and `stock` (design decision 4); set `partialSave` on the two stay-on-form branches and emit the long message; clear `partialSave` and `lastAppliedUpdate` on completion and in `load()`
- [x] 8.7 Discount failure branch: re-read the stored discount; if the read fails set `loadedDiscount = null`
- [x] 8.8 `NotFound` from the `PUT`, the photo step or the discount step → `NavigateBackDeleted(alreadyDeleted = true)`, no further step
- [x] 8.9 Delete: on `HasPendingOrders` keep the dialog open with `pendingOrders`; confirm sends `force = pendingOrders != null`; dismiss clears it; pass `loadedName`, `isDirty` and the warning to the dialog
- [x] 8.10 Content: partial-save banner at the top of `EditForm`; deleted state ("Este producto fue eliminado" + `Volver`) instead of the form; discard dialog wording when `partialSave != null`
- [x] 8.11 Strings `values` / `values-en`: `detail_partial_save_discount`, `detail_partial_save_photos`, `detail_discard_message_partial_discount`, `detail_discard_message_partial_photos`, `detail_deleted_title`, `detail_deleted_back`; reword `detail_error_photos_save` / `detail_error_discount_save` to match the banner
- [x] 8.12 Update the detail tests whose discount call order changes (`delete…, create…` → `create…, delete…`)
- [x] 8.13 Tests: discount create fails → `partialSave == DISCOUNT`, second Save sends no `PUT` (`updateCallCount == 1`) and creates the discount; same for photos; an edited name after the partial failure sends the `PUT` again; the re-read fails → `loadedDiscount == null` and the retry creates; `CancelRequested` while saving (gated) emits nothing; `NotFound` on `PUT` / photos / discount → `NavigateBackDeleted(true)`; `active = false` → `isDeleted`, no form; delete 409 → warning shown, `Eliminar igual` forces, dismiss clears; the dialog uses `loadedName` after a rename

## 9. Inventory (`features:product-inventory`)

- [x] 9.1 UiState `pendingOrders: HasPendingOrders?`; ViewModel keeps `deleteCandidate` on `HasPendingOrders`, confirms with `force`, clears on dismiss
- [x] 9.2 Content passes `pendingOrdersDetail` to the dialog
- [x] 9.3 Update the inventory discount test for the new call order
- [x] 9.4 Tests: 409 keeps the dialog with the server text and the row; `Eliminar igual` sends `force` and removes the row with "Producto eliminado"; dismiss sends nothing more

## 10. Verification

- [x] 10.1 Run, one at a time: `:core:network:testDebugUnitTest`, `:core:domain:test`, `:core:data:testDebugUnitTest`, `:features:product-register:testDebugUnitTest`, `:features:product-detail:testDebugUnitTest`, `:features:product-inventory:testDebugUnitTest`, then `testDebugUnitTest`
- [x] 10.2 Run `./gradlew lintDebug` and `./gradlew assembleDebug`

## 11. Manual — QA

- [ ] 11.1 Manual — S:EXP-022 / S:REG-094: Registro with 10 photos, Agregar and Back at once → the screen stays, the spinner keeps going, it ends in Inventario with "Producto registrado"; nginx `201`, no `499`
- [ ] 11.2 Manual — S:RED-023 / S:REG-085: airplane mode right after Agregar, then retry → one product in `GET /products`; the second `POST` answers `200`
- [ ] 11.3 Manual — S:RED-011 / S:DET-039: product with 2 photos, remove one, price 105 and 20 %, Guardar with `/discounts` blocked (proxy) → persistent banner; Back during the save does nothing; with network, Guardar sends only `POST …/discounts` and returns to Inventario
- [ ] 11.4 Manual — S:INV-048 / X:EXP-003: product in a pending order, Inventario ⋮ Eliminar → the dialog shows the order; "Eliminar igual" sends `DELETE ?force=true` (204)
- [ ] 11.5 Manual — S:DET-080 / X:XCO-025: open Detalle, delete the product by curl, rename, Guardar → back to Inventario with "Este producto ya había sido eliminado"
- [ ] 11.6 Manual — S:DET-072: rename "QA Sin foto det" to "…x", Eliminar producto → the dialog names "QA Sin foto det" and mentions the unsaved changes
- [ ] 11.7 Manual — S:REG-031: pick 11 photos → 10 kept, message shown, add tile disabled; `POST` carries 10 parts and answers 201
