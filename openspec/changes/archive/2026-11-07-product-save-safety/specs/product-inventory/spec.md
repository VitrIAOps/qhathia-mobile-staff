## MODIFIED Requirements

### Requirement: Delete a product from the inventory list

Selecting `Eliminar producto` in a row's actions menu SHALL open a confirmation dialog titled "¿Eliminar producto?". The dialog SHALL name the product and state that it will stop showing in the inventory and on the tablet, and that the action cannot be undone. It SHALL offer `Cancelar` and a destructive `Eliminar`. Nothing SHALL be requested until the merchant confirms.

On confirm the app SHALL call `DELETE /api/v1/products/{id}`. While the request is in flight the dialog SHALL stay open: both buttons are disabled, `Eliminar` shows progress, and dismissing the dialog is ignored.

- **On success:** the dialog closes, the product is removed from the list without reloading it, the product count updates, and a message reads "Producto eliminado".
- **When the product no longer exists (`404`):** the dialog closes, the product is removed from the list, and a message reads "Este producto ya había sido eliminado".
- **When the product is in pending orders (`409 PRODUCT_IN_PENDING_ORDERS`):** the dialog stays open and shows the server's `detail`, with `Cancelar` and a destructive `Eliminar igual`. `Eliminar igual` calls `DELETE /api/v1/products/{id}?force=true`, whose result is handled like the first delete.
- **On any other failure:** the dialog closes, the product stays in the list, and an error reads "No se pudo eliminar el producto. Inténtalo de nuevo".

There SHALL be no undo.

#### Scenario: Asking for confirmation
- **WHEN** the merchant selects `Eliminar producto` on the row of "Taladro Bosch"
- **THEN** a dialog titled "¿Eliminar producto?" names "Taladro Bosch", says it cannot be undone, and no request is made yet

#### Scenario: Cancelling
- **WHEN** the merchant taps `Cancelar` or outside the confirmation dialog
- **THEN** the dialog closes, no request is made and the product stays in the list

#### Scenario: Deleting
- **WHEN** the merchant confirms and the server answers `204`
- **THEN** the dialog closes, the product leaves the list, the count drops by one and "Producto eliminado" is shown

#### Scenario: Request in flight
- **WHEN** the merchant has confirmed and the server has not answered yet
- **THEN** the dialog stays open with both buttons disabled and progress on `Eliminar`, and tapping outside does not close it

#### Scenario: Already deleted elsewhere
- **WHEN** the merchant confirms and the server answers `404`
- **THEN** the product leaves the list and "Este producto ya había sido eliminado" is shown

#### Scenario: Product in pending orders
- **WHEN** the merchant confirms and the server answers `409 PRODUCT_IN_PENDING_ORDERS` with "Este producto está en 1 pedido pendiente (#400776)…"
- **THEN** the dialog stays open showing that message, with `Cancelar` and `Eliminar igual`, and the product stays in the list

#### Scenario: Deleting anyway
- **WHEN** the merchant taps `Eliminar igual`
- **THEN** the app calls `DELETE /api/v1/products/{id}?force=true` and, on `204`, the product leaves the list and "Producto eliminado" is shown

#### Scenario: Cancelling after the pending-orders warning
- **WHEN** the merchant taps `Cancelar` on the pending-orders warning
- **THEN** the dialog closes, nothing else is requested and the product stays in the list

#### Scenario: Failure
- **WHEN** the merchant confirms and the request fails for any other reason (no connection, server error)
- **THEN** the dialog closes, the product stays in the list and "No se pudo eliminar el producto. Inténtalo de nuevo" is shown

#### Scenario: Deleted product with an active search
- **WHEN** the merchant deletes a product while a search is narrowing the list
- **THEN** the product leaves both the searched list and the full list, so clearing the search does not bring it back
