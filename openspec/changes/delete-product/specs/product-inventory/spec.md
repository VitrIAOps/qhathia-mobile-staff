## MODIFIED Requirements

### Requirement: Product row actions menu

Each product row SHALL expose its trailing three-dot icon as a button that opens a menu anchored at that icon, with exactly three options, in this order: `Editar producto`, a discount option, and `Eliminar producto`. `Eliminar producto` SHALL be set apart from the other two and styled as destructive (error colour). The discount option SHALL read `Registrar descuento` when the product has no current discount and `Editar descuento` when it has one. Because the product list carries no discount data, opening the menu SHALL look up that product's current discount; while the lookup is in flight the discount option SHALL show a loading indication and be disabled, and if the lookup fails it SHALL read `Editar descuento` and remain usable. The button SHALL expose an accessible label. Tapping the rest of the row SHALL keep opening the Product Detail screen.

#### Scenario: Opening the menu

- **WHEN** the merchant taps the three-dot icon of a product row
- **THEN** a menu with `Editar producto`, the discount option and `Eliminar producto` is shown, drawn from the icon's position

#### Scenario: Product without a discount

- **WHEN** the menu is opened for a product that has no current discount
- **THEN** the discount option reads `Registrar descuento`

#### Scenario: Product with a discount

- **WHEN** the menu is opened for a product that has a current discount
- **THEN** the discount option reads `Editar descuento`

#### Scenario: Discount lookup in flight

- **WHEN** the menu has just been opened and the product's discount is still being looked up
- **THEN** the discount option shows a loading indication and cannot be selected yet

#### Scenario: Label follows a saved discount

- **WHEN** the merchant registers a discount from the menu and opens the same product's menu again
- **THEN** the discount option reads `Editar descuento`

#### Scenario: Dismissing the menu

- **WHEN** the merchant taps outside the open menu
- **THEN** the menu closes and no navigation or request happens

#### Scenario: Editar producto

- **WHEN** the merchant selects `Editar producto`
- **THEN** the menu closes and the app navigates to the Product Detail screen for that product

#### Scenario: Eliminar producto

- **WHEN** the merchant selects `Eliminar producto`
- **THEN** the menu closes and the delete confirmation for that product opens

## ADDED Requirements

### Requirement: Delete a product from the inventory list

Selecting `Eliminar producto` in a row's actions menu SHALL open a confirmation dialog titled "¿Eliminar producto?". The dialog SHALL name the product and state that it will stop showing in the inventory and on the tablet, and that the action cannot be undone. It SHALL offer `Cancelar` and a destructive `Eliminar`. Nothing SHALL be requested until the merchant confirms.

On confirm the app SHALL call `DELETE /api/v1/products/{id}`. While the request is in flight the dialog SHALL stay open: both buttons are disabled, `Eliminar` shows progress, and dismissing the dialog is ignored.

- **On success:** the dialog closes, the product is removed from the list without reloading it, the product count updates, and a message reads "Producto eliminado".
- **When the product no longer exists (`404`):** the dialog closes, the product is removed from the list, and a message reads "Este producto ya había sido eliminado".
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

#### Scenario: Failure
- **WHEN** the merchant confirms and the request fails for any other reason (no connection, server error)
- **THEN** the dialog closes, the product stays in the list and "No se pudo eliminar el producto. Inténtalo de nuevo" is shown

#### Scenario: Deleted product with an active search
- **WHEN** the merchant deletes a product while a search is narrowing the list
- **THEN** the product leaves both the searched list and the full list, so clearing the search does not bring it back
