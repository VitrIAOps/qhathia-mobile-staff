## 1. Network (`core:network`)

- [x] 1.1 In `ProductDtos.kt`, add `UpdateProductRequestDto(name, price, @EncodeDefault description = "", stock: Int? = null, categoryId: String? = null, @EncodeDefault tags = emptyList(), version: Long? = null)` per design decision 3, with KDoc on what absent, `""` and `[]` mean (API_REFERENCE §40)
- [x] 1.2 Add `version: Long? = null` to `ProductDetailDto` and update its KDoc
- [x] 1.3 `ProductApiService.updateProduct(id, body: UpdateProductRequestDto)`; rewrite its KDoc (no longer a full replace)
- [x] 1.4 `ProductDtosTest`: the update DTO encodes `description: ""` and `tags: []` when empty, omits `stock` and `version` when null, and never has `allowDiscountInquiry`/`position`/`brand`; `ProductDetailDto` decodes `version` (present and absent)
- [x] 1.5 `ProductApiServiceTest`: switch the conflict case to `UpdateProductRequestDto`

## 2. Domain (`core:domain`)

- [x] 2.1 `ProductUpdate`: `stock: Int?`, `description: String`, `version: Long?`; drop `allowDiscountInquiry` and `position`; rewrite the KDoc (partial update, what null/`""` mean)
- [x] 2.2 `ProductDetail`: add `version: Long? = null` with KDoc
- [x] 2.3 `UpdateProductError`: add `data class Conflict(val currentVersion: Long?)` with KDoc (409 `PRODUCT_VERSION_CONFLICT` / `CONCURRENT_MODIFICATION`)
- [x] 2.4 `ProductRepository.updateProduct` and `UpdateProductUseCase` return `Result<ProductDetail>`; update KDocs

## 3. Data (`core:data`)

- [x] 3.1 `ProductMappers.kt`: `ProductUpdate.toUpdateRequestDto()` builds `UpdateProductRequestDto`; `ProductDetailDto.toDomain()` maps `version`
- [x] 3.2 `ProductRepositoryImpl.updateProduct` returns the mapped `PUT` body
- [x] 3.3 Replace `Int.toUpdateError()` with `NetworkResult.HttpError.toUpdateError()` per design decision 5, and use it in `getProduct`, `updateProduct`, `deleteProduct` and `toUpdateResult`
- [x] 3.4 `ProductMappersTest`: update mapping (stock null, description `""`, version) and `version` on the detail
- [x] 3.5 `ProductRepositoryImplTest`: rewrite "sends the full replace body" for the partial body; success returns the `PUT` body as `ProductDetail`; 409 `PRODUCT_VERSION_CONFLICT` → `Conflict(8)`; 409 `CONCURRENT_MODIFICATION` → `Conflict(null)`; a 409 with another `errorCode` → `Unknown` carrying its `detail`; existing 404/422/500 cases still pass

## 4. Testing fixtures (`core:testing`)

- [x] 4.1 `FakeProductRepository.updateResult: Result<ProductDetail>`, defaulting to a success that echoes the product (`TestFixtures.productDetail()`); keep `updatedProduct`/`updateCallCount`; add `updateResults: ArrayDeque<Result<ProductDetail>>` for multi-call tests if the conflict tests need it
- [x] 4.2 `TestFixtures.productUpdate(...)` with the new fields (`stock: Int? = 12`, `description: String`, `version: Long? = null`); `productDetail(...)` with `version: Long? = 7`
- [x] 4.3 Fix the compile errors this causes in `core:domain` tests (`UpdateProductUseCase` users) and anything else that builds a `ProductUpdate`

## 5. Shared UI (`core:ui`)

- [x] 5.1 `QhathiaDiscountToggleRow`: add `warning: String? = null`, shown below the summary in `colorScheme.error` / `bodySmall` with `liveRegion = Polite`; add a preview with the warning

## 6. Product detail (`features:product-detail`)

- [x] 6.1 `ProductDetailUiState`: add `loadedVersion`, `loadedQuantity`, `isConsult`, `conflict: SaveConflict?` (new `SaveConflict(currentVersion: Long?)`), `isDiscountOverPrice`; `isSaveEnabled` accepts a blank stock when `isConsult`; drop `allowDiscountInquiry` and `position`; update the class KDoc
- [x] 6.2 `ProductDetailUiEvent`: add `ConflictReload`, `ConflictOverwrite`, `ConflictDismissed`
- [x] 6.3 `seededFrom`: set `loadedVersion`, `loadedQuantity`, `isConsult`; fix its KDoc (an empty stock is allowed for CONSULT)
- [x] 6.4 `onPriceChanged`: only `price`, `priceError`, `isDirty` (design decision 1)
- [x] 6.5 `save()`: over-price check first (decision 2); build `ProductUpdate` with the stock rule and `description.trim()` (decision 6); on success set `loadedVersion`/`loadedQuantity` from the `PUT` body before anything else; on `Conflict` open the dialog without a snackbar
- [x] 6.6 Stay-on-form branches adopt the re-read product's `version`, stock and `isConsult` (decision 8): `resyncPhotos` returns what it read; the discount branch adds a `getProductDetailUseCase` read
- [x] 6.7 Conflict events per design decision 7, including the re-read when `currentVersion` is null; `toMessageRes()` gains a `Conflict` branch
- [x] 6.8 `ProductDetailContent`: the conflict `AlertDialog` (three stacked actions), and `warning` on the discount row when `isDiscountOverPrice`
- [x] 6.9 `DetailQuantityField(isConsult)`: placeholder `A consultar` and the supporting hint when `isConsult`
- [x] 6.10 Strings in `values` and `values-en`: `detail_conflict_title`, `detail_conflict_message`, `detail_conflict_reload`, `detail_conflict_overwrite`, `detail_conflict_keep_editing`, `detail_discount_over_price`, `detail_discount_removed_over_price`, `detail_quantity_consult_placeholder`, `detail_quantity_consult_hint`, `detail_error_conflict`

## 7. Product detail tests (`features:product-detail`)

- [x] 7.1 Invert "a price change that no longer fits the discount removes it from the form": the discount stays and `isDiscountOverPrice` is true
- [x] 7.2 189.90 + 35 % → `PriceChanged("")` → `PriceChanged("200")`: discount still 35 %, `discountedPrice == 130.0`; `"150"`, `"150."`, `"150.5"` keep it
- [x] 7.3 Loaded discount, clear the price, type another, save: no discount request at all after load
- [x] 7.4 Fixed 900, price 100, Save: message shown, no `PUT`, `discount == null`, form dirty; saving again removes the discount
- [x] 7.5 The update carries `version` from the load; changing only the name sends `stock == null`; changing the stock sends it; clearing the description sends `""`
- [x] 7.6 CONSULT product: price 50 → 55 enables Save and sends `stock == null`; a stocked product with the field cleared cannot save
- [x] 7.7 `Conflict(8)`: dialog opens, no snackbar; `ConflictOverwrite` sends `version = 8` and no untouched stock; `ConflictReload` reloads and clears the edits; `ConflictDismissed` keeps the edits
- [x] 7.8 `Conflict(null)` + `ConflictOverwrite`: the product is read and its version is sent; the read fails → network message, dialog closed
- [x] 7.9 Update succeeds and the discount fails, then save again: the second `PUT` carries the re-read version (not the loaded one); same for the photo-failure branch
- [x] 7.10 Re-check the existing save tests that asserted `allowDiscountInquiry`/`position` or a full body, and update them to the new contract

## 8. Product registration (`features:product-register`)

- [x] 8.1 `ProductRegisterUiState.isDiscountOverPrice`; `ProductRegisterViewModel.onPriceChanged` no longer touches the discount
- [x] 8.2 `submit()`: over-price check before registering, removing the discount with `register_discount_removed_over_price`
- [x] 8.3 `ProductRegisterContent`: pass `warning` to `QhathiaDiscountToggleRow`
- [x] 8.4 Strings in `values` and `values-en`: `register_discount_over_price`, `register_discount_removed_over_price`
- [x] 8.5 Tests: invert "a price change that no longer fits the discount removes it" and "clearing the price removes the discount and locks the toggle" (now: the discount stays, the toggle is locked); 189.90 + 35 % → `""` → `200` keeps it and submitting creates the discount; fixed 900 over 100 → no register call, message, discount removed

## 9. Verification

- [x] 9.1 Run, one at a time: `./gradlew :core:network:testDebugUnitTest`, `:core:domain:test`, `:core:data:testDebugUnitTest`, `:features:product-detail:testDebugUnitTest`, `:features:product-register:testDebugUnitTest`; all pass
- [x] 9.2 Run `./gradlew lintDebug` and `./gradlew assembleDebug`

## 10. Manual — QA

- [ ] 10.1 Manual — S:REG-046: Registro, 189.90 + 35 %, clear the price and type 200 → "Descuento de 35%" with S/ 130.00; Agregar sends `POST …/discounts`
- [ ] 10.2 Manual — S:EXP-025: 1000 with a fixed 900, lower to 100 → warning on the row; Guardar → "Quitamos el descuento…", nothing sent
- [ ] 10.3 Manual — Detalle with a stored 20 %: 150 → 150.50 → 200 (clearing on the way), Guardar → only `PUT`, no `DELETE …/discounts`; `GET` has `discountedPrice 160.00`
- [ ] 10.4 Manual — X:XCO-024: open Detalle, change the description by curl, change the price and Guardar → conflict dialog and `PUT 409`; "Ver lo nuevo" shows the other description
- [ ] 10.5 Manual — Open Detalle, confirm a sale of the product, change only the name and Guardar → dialog; "Guardar lo mío" keeps the stock reduced
- [ ] 10.6 Manual — Empty the description and save → `GET` has `"description": null`
- [ ] 10.7 Manual — S:DET-014: "QA Consult" 50 → 55 → Guardar enabled; `PUT` without `stock`; `GET` has `stock null`, `availability CONSULT`
