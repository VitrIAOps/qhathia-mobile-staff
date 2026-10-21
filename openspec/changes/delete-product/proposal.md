## Why

A merchant has no way to take a product out of the catalog from the app. Today a discontinued product stays in Inventario and on the kiosk tablet forever, unless someone sets its stock to 0, which leaves it on display as "Sin existencias". The backend has supported this since the start (`DELETE /api/v1/products/{id}`). The triage ([BUG] "los tres puntos del producto no abren nada") listed it as missing. Product decision (2026-10-21): offer a single, irreversible **Eliminar producto**, and no reversible "Desactivar", because the backend has no reactivate endpoint and `POST /deactivate` is the same soft delete.

## What Changes

- The Inventario row's three-dot menu gains a third option, **Eliminar producto**, styled as destructive and set apart from the other two.
- Choosing it opens a confirmation dialog naming the product and stating that it disappears from Inventario and from the tablet and cannot be undone. The two buttons are `Cancelar` and `Eliminar`.
- On confirm the app calls `DELETE /api/v1/products/{id}` (`204`). The dialog stays open with progress until the request answers.
  - **Success:** the dialog closes, the row leaves the list, the count updates, and a snackbar reads "Producto eliminado".
  - **Already gone (`404`):** treated as done. The row leaves the list, and the snackbar reads "Este producto ya había sido eliminado".
  - **Any other failure:** the dialog closes, the row stays, and an error snackbar is shown.

## Capabilities

### New Capabilities

_None._

### Modified Capabilities

- `product-inventory`: the row actions menu has three options instead of two, and adds a requirement for deleting a product with confirmation.

## Impact

- **`core:network`**: `ProductApiService.deleteProduct(id)`.
- **`core:domain`**: `ProductRepository.deleteProduct(id)`, `DeleteProductUseCase`. Failures reuse `UpdateProductException` (`NotFound`, `Network`, `Unknown`).
- **`core:data`**: `ProductRepositoryImpl.deleteProduct`.
- **`core:testing`**: `FakeProductRepository` supports deletes.
- **`features:product-inventory`**: menu option, confirmation dialog, events, state, ViewModel, strings (es/en), previews and tests.
- **API:** `DELETE /api/v1/products/{id}`, already in production. Any role may call it.
- **Not in scope:**
  - Deleting from the Product Detail screen.
  - Bulk delete.
  - Undo. The backend cannot reactivate a product.
  - Pending orders that contain the deleted product. The backend keeps them, and they are handled as today.
