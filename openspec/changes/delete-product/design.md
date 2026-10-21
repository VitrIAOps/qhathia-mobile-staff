## Context

The Inventario row menu (`ProductRow.RowActionsMenu`) offers `Editar producto` and a discount option. `InventoryViewModel` already follows a pattern for an in-place write from that menu: the discount dialog uses state for what is open and what is saving, one-shot `ShowMessage` / `ShowError` effects, and no navigation.

`DELETE /api/v1/products/{id}` is a soft delete (`is_active=false`) that answers `204`. Any role may call it. Inactive products are left out of the catalog the tablet reads. `ProductRepositoryImpl` already maps product failures to `UpdateProductException` (`404 → NotFound`, `503`/transport → `Network`).

## Goals / Non-Goals

**Goals:**
- Delete a product from the row menu with one deliberate confirmation.
- Never leave a deleted product on screen, and never remove one that was not deleted.

**Non-Goals:**
- An undo. There is no reactivate endpoint.
- Bulk delete, and role-based hiding. The backend allows every role.

## Decisions

### 1. Domain: a dedicated use case, reusing `UpdateProductException`

- Add `ProductRepository.deleteProduct(id): Result<Unit>` and `DeleteProductUseCase`. The use case normalizes any failure to `UpdateProductException`, as `UpdateProductUseCase` does.
- The error causes are the same as for an edit (`NotFound`, `Network`, `Unknown`), so a new `DeleteProductError` type would duplicate them.
- `InvalidInput` cannot happen here and is mapped to `Unknown` in the UI.

### 2. State: the confirmation lives in `InventoryUiState`

- Add `deleteCandidate: Product?`, which is non-null while the dialog is open, and `isDeleting: Boolean`.
- The new events are `DeleteProductClicked(productId)`, `DeleteConfirmed` and `DeleteDismissed`.
- While `isDeleting` is true, dismissing does nothing and the dialog's buttons are disabled. This mirrors `isDiscountSaving`.
- **Alternative rejected:** dialog state held locally in the composable. It would not survive rotation, and the ViewModel would still need to know which product to delete.

### 3. Outcome handling

| Result | Dialog | Row | Snackbar |
|---|---|---|---|
| `204` | closes | removed from `loadedProducts` and `visibleProducts` | `ShowMessage("Producto eliminado")` |
| `404` (`NotFound`) | closes | removed (it is already gone server-side) | `ShowMessage("Este producto ya había sido eliminado")` |
| other | closes | stays | `ShowError("No se pudo eliminar el producto. Inténtalo de nuevo")` |

- The row is removed locally, not by re-querying. The server has already confirmed the result, and a refetch would flash the list or lose scroll position for no new information.
- The cached `knownDiscounts` entry for that id is dropped.
- **Alternative rejected:** an optimistic removal before the request answers. If the request failed, the row would come back after the merchant had already looked away.

### 4. Menu and dialog presentation

- **Menu:** `Eliminar producto` is the last item, after a `HorizontalDivider`. Its text and leading `Icons.Outlined.Delete` icon use `colorScheme.error`.
- **Dialog:** a Material `AlertDialog`, following the destructive `ConfirmDialog` already used in Seguridad.
  - Title: "¿Eliminar producto?"
  - Message: "«{nombre}» dejará de verse en el inventario y en la tablet. Esta acción no se puede deshacer."
  - Buttons: `Cancelar`, and `Eliminar` in the error colour. While deleting, `Eliminar` shows a small progress indicator in place of its label.
- The name is shown because the merchant opened the menu from a list of look-alike rows.

### 5. The edit screen

- **Button:** a full-width `TextButton` "Eliminar producto", with the delete icon in the error colour, placed under the `Cancelar` / `Guardar` row. It is disabled while saving or deleting.
- **Confirmation:** the same dialog as Inventario. It moves to `core:ui` as `QhathiaDeleteProductDialog`, with its four strings, so both modules share one design and one text.
- **State:** `showDeleteDialog` and `isDeleting` in `ProductDetailUiState`.
  - While `isDeleting` is true, the ViewModel ignores back, the header bell, dismissals and a second confirm, and `isSaveEnabled` is false.
  - `BackHandler` is enabled while deleting, so system back cannot pop the screen and cancel the request.
- **Unsaved edits:** they are dropped without the discard dialog. The delete confirmation already covers losing the product.
- **Outcome:**
  - `204` → `NavigateBackDeleted(alreadyDeleted = false)`.
  - `404` → `NavigateBackDeleted(alreadyDeleted = true)`.
  - Any other failure → the dialog closes, the merchant stays on the form with the edits intact, and `ShowError("No se pudo eliminar el producto…")` is shown.
- **Back in Inventario:**
  - `MainShell` puts `INVENTORY_RESULT_PRODUCT_DELETED` or `…_ALREADY_DELETED` on the existing result channel (`INVENTORY_REGISTER_RESULT_KEY`). This is the same mechanism as `PRODUCT_UPDATED`, so the "consume once" fix carries over.
  - Inventario refreshes and shows "Producto eliminado" or "Este producto ya había sido eliminado".
  - Here a reload is right: Inventario did not see the delete happen.

## Risks / Trade-offs

- **[Risk] `GET /products` may still list inactive products**, in which case a later reload would bring the row back. → The contract describes the delete as removing the product from use, and the catalog filters `isActive=true`. QA checks that the product stays gone after a pull to refresh and on the tablet.
- **[Risk] A seller deletes by mistake.** The backend allows any role, and there is no undo. → The confirmation names the product and says the action is irreversible. Restricting it by role is a product decision, left open.
- **[Trade-off] Pending orders that contain the product are untouched.** They can still be confirmed or rejected as today.

## Open Questions

- Should only OWNER be able to delete? The backend allows SELLER today, and this change follows the backend.
