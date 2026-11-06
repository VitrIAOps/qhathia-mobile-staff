## Why

The two CRITICAL Staff findings of the 2026-11-03 QA regression (`docs/handoff/android-regresion-2026-11-03.md` §3.3) silently destroy data on the server:

- **F-86** — editing the price drops the discount from the form on every keystroke that leaves the price empty, ending in `.` or below a fixed amount. On the Product Detail, saving then **deletes the stored discount** (`removeProductDiscountUseCase`), so the kiosk and the PDF charge the full price; on Registro the product is created without the discount the merchant set. The merchant only saw the switch turn off.
- **F-93** — `PUT /products/{id}` is sent without `version`, so the last writer wins without a warning. Two vendors overwrite each other, and because the form always sends `stock`, a sale confirmed while the form was open is undone.

Since Fase 40 the backend preserves fields left out of the `PUT`, needs `""` to clear the description, and answers `409 PRODUCT_VERSION_CONFLICT` with `currentVersion` when the `version` is stale. The app is still written for a full replace, which also causes **F-81** (a "consultar disponibilidad" product cannot be saved without inventing a stock) and makes clearing the description impossible. `problem-detail-errors` (P-1) already exposes the `errorCode`, so the conflict can now be told apart from any other 409.

## What Changes

- **Price edits never touch the discount** (Detalle and Registro). The discount stays on the form while the price is empty, partial or too low; the switch stays on (disabled while there is no valid price) and the summary stays visible.
- **Fixed discount over the price**: the row warns "El descuento es mayor que el precio. Edítalo o quítalo." Saving or registering in that state removes the discount from the form, shows "Quitamos el descuento porque es mayor que el nuevo precio. Revisa y vuelve a guardar." (or "…agregar") and sends nothing. The product is never left at S/ 0.
- **Optimistic version on update**: the detail reads `version` from `GET /products/{id}` and sends it on the `PUT`. A `409 PRODUCT_VERSION_CONFLICT` (or `CONCURRENT_MODIFICATION`) opens the dialog "Otro usuario cambió este producto" with three options: "Ver lo nuevo" reloads the product, "Guardar lo mío" saves again with the server's current version, and "Seguir editando" closes the dialog.
- **Partial update body**: a new `UpdateProductRequestDto` replaces the create DTO on `PUT`. It sends name, price, description (always, `""` clears it), category, tags and version. `stock` is sent only when the merchant changed it. `allowDiscountInquiry`, `position` and `brand` are no longer sent, so the backend keeps them (also closes F-77).
- **"A consultar" products (F-81)**: with an empty stock field the product can be saved and stays `CONSULT`. The field shows the placeholder "A consultar" and a hint.
- **BREAKING (internal)**: `ProductRepository.updateProduct` returns `Result<ProductDetail>`, the body of the `PUT`. `ProductUpdate` drops `allowDiscountInquiry`/`position`, and gains `stock: Int?`, a non-null `description` and `version`. `UpdateProductError` gains `Conflict(currentVersion)`. Product HTTP errors are classified by `problem?.errorCode` and then by status.

## Capabilities

### New Capabilities

_None._

### Modified Capabilities

- `product-detail`: "Product editing" (a stock-less product can be saved), "Product update" (partial body with `version`, the conflict dialog, clearing the description), "Discount editing on product detail" (a price edit keeps the discount; warning when a fixed amount exceeds the price), "Discount persisted on save" (the over-price check before any request).
- `product-registration`: "Discount summary on the registration form" (a price edit keeps the discount; warning), "Discount creation on registration" (the over-price check before submitting).

## Impact

- **`core:network`** — `ProductDtos.kt`: new `UpdateProductRequestDto`, `ProductDetailDto.version`. `ProductApiService.updateProduct` takes the new DTO.
- **`core:domain`** — `ProductUpdate`, `ProductDetail.version`, `UpdateProductError.Conflict`, `ProductRepository.updateProduct` and `UpdateProductUseCase` return `ProductDetail`.
- **`core:data`** — `ProductMappers.kt` (`toUpdateRequestDto`, `version`), `ProductRepositoryImpl` (`HttpError.toUpdateError()` reading `problem?.errorCode`).
- **`core:ui`** — `QhathiaDiscountToggleRow` gains `warning: String?`.
- **`core:testing`** — `FakeProductRepository.updateResult: Result<ProductDetail>`, `TestFixtures.productUpdate/productDetail`.
- **`features:product-detail`** — UiState, ViewModel (`onPriceChanged`, `save`, conflict events), Content (conflict dialog, warning), quantity field, strings (`values`, `values-en`).
- **`features:product-register`** — UiState, ViewModel (`onPriceChanged`, `submit`), Content (warning), strings.
- **Depends on** `problem-detail-errors` (branch `feature/problem-detail-errors`, not yet merged): this branch is cut from it.
- **Not in this change:** `Idempotency-Key` and the back lock (F-85), partial save after a network cut (F-96), `PRODUCT_IN_PENDING_ORDERS` / `PRODUCT_DELETED` on delete and edit (F-83, F-87), amount formatting (F-82). Those belong to `product-save-safety` and `money-format`.
