## ADDED Requirements

### Requirement: Delete the product from the edit screen

The product edit screen SHALL show an `Eliminar producto` button below the `Cancelar` and `Guardar` actions, full width and in the error colour. It SHALL be disabled while the product is being saved or deleted, and unavailable until the product has loaded.

Tapping it SHALL open the same confirmation as the Inventario row menu: "¿Eliminar producto?", naming the product, stating that it cannot be undone, with `Cancelar` and a destructive `Eliminar`. Nothing SHALL be requested until the merchant confirms.

On confirm the app SHALL call `DELETE /api/v1/products/{id}`. While the request is in flight, the dialog SHALL stay open with both buttons disabled and progress on `Eliminar`. Back, the header bell and dismissing the dialog SHALL be ignored, and saving SHALL be disabled.

- **On success or `404`:** the app SHALL return to Inventario. Unsaved edits are dropped without the discard dialog. Inventario SHALL reload and show "Producto eliminado", or "Este producto ya había sido eliminado" for a `404`.
- **On any other failure:** the merchant SHALL stay on the edit screen with the edits intact, the dialog SHALL close, and an error SHALL read "No se pudo eliminar el producto. Inténtalo de nuevo".

#### Scenario: Button placement
- **WHEN** the merchant opens a product's edit screen and it loads
- **THEN** an `Eliminar producto` button is shown below `Cancelar` and `Guardar`

#### Scenario: Asking for confirmation
- **WHEN** the merchant taps `Eliminar producto`
- **THEN** the confirmation names the product and no request is made yet

#### Scenario: Deleting
- **WHEN** the merchant confirms and the server answers `204`
- **THEN** the app returns to Inventario, the product is no longer listed and "Producto eliminado" is shown

#### Scenario: Deleting with unsaved edits
- **WHEN** the merchant has edited the name, then deletes and confirms
- **THEN** the product is deleted and the app returns to Inventario without asking to discard the edits

#### Scenario: Already deleted elsewhere
- **WHEN** the merchant confirms and the server answers `404`
- **THEN** the app returns to Inventario and "Este producto ya había sido eliminado" is shown

#### Scenario: Failure
- **WHEN** the merchant confirms and the request fails for any other reason
- **THEN** the edit screen stays with the edits intact and "No se pudo eliminar el producto. Inténtalo de nuevo" is shown

#### Scenario: Request in flight
- **WHEN** the delete request has not answered yet
- **THEN** back and the header bell do nothing, the dialog cannot be dismissed and `Guardar` is disabled
