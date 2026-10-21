## 1. Network, domain and data

- [x] 1.1 `core:network` — `ProductApiService.deleteProduct(id)`: `DELETE api/v1/products/{id}`, `204` with no body
- [x] 1.2 `core:domain` — `ProductRepository.deleteProduct(id): Result<Unit>` (KDoc: soft delete, failures carry `UpdateProductException`)
- [x] 1.3 `core:domain` — `DeleteProductUseCase` that normalizes any failure to `UpdateProductException` (design Decision 1)
- [x] 1.4 `core:data` — `ProductRepositoryImpl.deleteProduct`, mapping HTTP codes with the existing `toUpdateError()`
- [x] 1.5 `core:testing` — `FakeProductRepository` supports deletes: records deleted ids, and can be set up to fail
- [x] 1.6 Unit tests: the use case passes success through, keeps `UpdateProductException`, and wraps other failures as `Unknown`

## 2. Inventory state and ViewModel

- [x] 2.1 `InventoryUiState`: add `deleteCandidate: Product?` and `isDeleting` (design Decision 2)
- [x] 2.2 `InventoryUiEvent`: add `DeleteProductClicked(productId)`, `DeleteConfirmed` and `DeleteDismissed`
- [x] 2.3 `InventoryViewModel`: open, dismiss (ignored while deleting) and confirm the delete; on success or `NotFound`, remove the product from `loadedProducts`, `visibleProducts` and `knownDiscounts`, and emit the matching `ShowMessage`; on other failures emit `ShowError` and keep the row (design Decision 3)
- [x] 2.4 Strings (es/en): `inventory_action_delete_product`, `inventory_delete_title`, `inventory_delete_message` (with `%1$s` for the name), `inventory_delete_confirm`, `inventory_delete_cancel`, `inventory_product_deleted`, `inventory_product_already_deleted`, `inventory_delete_error`
- [x] 2.5 ViewModel tests:
  - opening the dialog makes no request
  - dismissing clears the dialog, and is ignored while deleting
  - success removes the product from both lists and shows the message
  - `404` removes the product and shows the "already deleted" message
  - a network failure keeps the product and shows the error
  - deleting under an active search removes the product from the full list too

## 3. UI

- [x] 3.1 `ProductRow` menu: a `HorizontalDivider`, then `Eliminar producto` with `Icons.Outlined.Delete`, both in `colorScheme.error` (design Decision 4)
- [x] 3.2 `DeleteProductDialog` (`AlertDialog`): the title, a message naming the product, and `Cancelar` / `Eliminar` (in the error colour). While deleting, both buttons are disabled and `Eliminar` shows progress
- [x] 3.3 Wire the events through `InventoryContent` and `ProductRow`, and show the dialog when `deleteCandidate != null`
- [x] 3.4 Previews: the dialog idle and deleting (light and dark), and the open row menu if a menu preview already exists

## 4. Verification

- [x] 4.1 `./gradlew :core:domain:test :core:data:testDebugUnitTest :features:product-inventory:testDebugUnitTest :features:product-inventory:lintDebug`
- [x] 4.2 Full build: `./gradlew testDebugUnitTest lintDebug assembleDebug`

## QA (manual, run by QA)

- Inventario: ⋮ shows `Eliminar producto` last, in red. Confirming removes the row, the count drops, and "Producto eliminado" is shown
- After a pull to refresh, on the kiosk tablet, and in Inicio ("Productos para reponer"), the deleted product does not come back
- Cancel and tap-outside do nothing. Airplane mode shows the error and keeps the row
- Light and dark, large font: the dialog's message and buttons are not clipped
