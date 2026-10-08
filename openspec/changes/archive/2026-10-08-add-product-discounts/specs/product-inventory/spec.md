## ADDED Requirements

### Requirement: Product row actions menu

Each product row SHALL expose its trailing three-dot icon as a button that opens a menu anchored at that icon, with exactly two options: `Editar producto` and a discount option. The discount option SHALL read `Registrar descuento` when the product has no current discount and `Editar descuento` when it has one. Because the product list carries no discount data, opening the menu SHALL look up that product's current discount; while the lookup is in flight the discount option SHALL show a loading indication and be disabled, and if the lookup fails it SHALL read `Editar descuento` and remain usable. The button SHALL expose an accessible label. Tapping the rest of the row SHALL keep opening the Product Detail screen.

#### Scenario: Opening the menu

- **WHEN** the merchant taps the three-dot icon of a product row
- **THEN** a menu with `Editar producto` and the discount option is shown, drawn from the icon's position

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

### Requirement: Edit discount from the inventory list

Selecting the discount option (`Registrar descuento` or `Editar descuento`) SHALL open the `Aplicar descuento` dialog using the row's price as the original price, prefilled with the current discount when the product has one; the discount SHALL be loaded first if opening the menu did not already resolve it. Applying SHALL save the discount with replace semantics and confirm with a snackbar.

#### Scenario: Product with a discount

- **WHEN** the merchant selects `Editar descuento` on a product that has a discount
- **THEN** the dialog opens prefilled with that discount

#### Scenario: Product without a discount

- **WHEN** the merchant selects `Registrar descuento` on a product that has no discount
- **THEN** the dialog opens empty so a discount can be created

#### Scenario: Discount applied

- **WHEN** the merchant applies a value in the dialog and the save succeeds
- **THEN** the dialog closes and a snackbar confirms the discount was updated

#### Scenario: Current discount fails to load

- **WHEN** the product's discount cannot be loaded
- **THEN** the dialog does not open and an error message is shown

#### Scenario: Save fails

- **WHEN** the save request fails
- **THEN** the dialog stays open with the entered value and an error message is shown

### Requirement: Search clear action

The inventory search field SHALL show a trailing clear (`x`) button whenever it contains text. Tapping it SHALL remove all the entered text. The button SHALL NOT be shown while the field is empty.

#### Scenario: Clear button appears with text

- **WHEN** the merchant has typed text in the search field
- **THEN** a clear button is shown at the end of the field

#### Scenario: Clearing the text

- **WHEN** the merchant taps the clear button
- **THEN** the search field becomes empty, the clear button disappears and all products matching the active filters are shown again

### Requirement: Inventory feedback when a registration's discount fails

When the app returns to the Inventory screen after a product was registered but its discount could not be created, the inventory list SHALL refresh and SHALL show a message distinct from the plain registration success message.

#### Scenario: Returning from a registration with a failed discount

- **WHEN** the merchant returns to the Inventory screen because the product was registered but its discount could not be applied
- **THEN** the inventory list re-queries its products and a message states that the product was registered but the discount could not be applied
