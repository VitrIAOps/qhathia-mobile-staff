# product-inventory Specification

## Purpose

Defines how a store owner views and filters the store's inventory in the Qhathia staff app: the Inventory screen that lists products from the Products API, category and availability filtering, name search, a live product count, the post-login app shell (bottom navigation, header, registration FAB), and the interim mocked stock indicator.
## Requirements
### Requirement: Product list display

The Inventory screen SHALL retrieve the authenticated store's products from `GET /api/v1/products` and display each product as a row showing its primary image, name, price and availability indicator. The request SHALL be authenticated with the merchant's Bearer token via the existing authenticated HTTP client.

#### Scenario: Products load successfully

- **WHEN** the merchant opens the Inventory screen and the products request succeeds with one or more products
- **THEN** the screen displays one row per product with its image, name, formatted price and availability indicator

#### Scenario: Store has no products

- **WHEN** the products request succeeds but returns an empty list
- **THEN** the screen displays an empty state instead of an empty list and shows a product count of zero

#### Scenario: Products request fails

- **WHEN** the products request fails due to a network or server error
- **THEN** the screen displays a retryable error state and does not crash

#### Scenario: Loading indicator while fetching

- **WHEN** the products request is in flight
- **THEN** the screen shows a loading indicator and hides it once the request completes

### Requirement: Product stock indicator (mocked)

Each product row SHALL display a stock indicator in the form "N stock" matching the mockup. Because the products API exposes no numeric stock quantity yet, the value SHALL be a mocked placeholder isolated behind a single helper so it can be swapped for the real field once backend provides it, without touching the row layout. The `availability` enum (`AVAILABLE`, `LOW_STOCK`, `CONSULT`) SHALL continue to drive the availability sub-filter even though it is not the row's primary indicator.

#### Scenario: Row shows a mocked stock value

- **WHEN** a product row is rendered
- **THEN** the row shows a "N stock" indicator sourced from the mocked stock helper

#### Scenario: Mock is isolated for later replacement

- **WHEN** backend later adds a numeric stock field to the products API
- **THEN** only the mocked stock helper (and its DTO/domain mapping) needs to change, not the row composable

### Requirement: Category filter

The Inventory screen SHALL load the store's categories from `GET /api/v1/categories` and present them as a horizontal, scrollable filter strip preceded by a default `Todas` option. Selecting a category SHALL re-query products filtered by that category's id; selecting `Todas` SHALL list all products with no category filter.

#### Scenario: Default shows all products

- **WHEN** the Inventory screen first loads
- **THEN** the `Todas` option is selected and products are listed without a category filter

#### Scenario: Filter by a category

- **WHEN** the merchant selects a category chip
- **THEN** the products list is re-queried with that category's `categoryId` and only its products are shown, and the selected chip is visually marked as active

#### Scenario: Categories fail to load

- **WHEN** the categories request fails
- **THEN** the screen still shows the `Todas` option and the product list, degrading without blocking inventory browsing

### Requirement: Availability sub-filter

The Inventory screen SHALL provide an availability sub-filter offering Disponibles (`AVAILABLE`), Stock bajo (`LOW_STOCK`) and Consultar disponibilidad (`CONSULT`), with a default of no availability filter. Applying a sub-filter SHALL re-query products with the corresponding `availability` query parameter, combinable with the active category filter.

#### Scenario: Apply an availability sub-filter

- **WHEN** the merchant applies an availability sub-filter
- **THEN** the products list is re-queried with the matching `availability` parameter, preserving any active category filter

#### Scenario: Clear the availability sub-filter

- **WHEN** the merchant clears the availability sub-filter
- **THEN** the products list is re-queried without the `availability` parameter, preserving any active category filter

### Requirement: Product search

The Inventory screen SHALL provide a search field that filters the currently loaded products by name, case-insensitively, without issuing a new network request.

#### Scenario: Search narrows the visible list

- **WHEN** the merchant types text into the search field
- **THEN** only products whose name contains the entered text (case-insensitive) remain visible and the product count reflects the visible products

#### Scenario: Clearing search restores the list

- **WHEN** the merchant clears the search field
- **THEN** all products matching the active category and availability filters are shown again

### Requirement: Product count

The Inventory screen SHALL display a count label reflecting the number of products currently shown after the active filters and search have been applied.

#### Scenario: Count reflects the visible products

- **WHEN** the visible product set changes due to loading, filtering, or searching
- **THEN** the count label updates to the number of products currently displayed

### Requirement: Inventory app shell

The post-login experience SHALL present a bottom navigation bar with exactly four destinations — Inicio, Inventario, Solicitudes and Perfil — with Inventario marked active while the Inventory screen is shown, and an application header containing the screen title and a notification icon. Terminal pairing is reached from Perfil rather than from a bottom-navigation destination.

#### Scenario: Inventory is the active tab

- **WHEN** the Inventory screen is displayed
- **THEN** the bottom navigation bar is visible with the Inventario destination marked as active and the header shows the inventory title and notification icon

### Requirement: Product registration entry point

The Inventory screen SHALL display a floating action button for registering a new product. Tapping it SHALL navigate to the Product Registration screen.

#### Scenario: Registration FAB is visible

- **WHEN** the Inventory screen is displayed
- **THEN** a floating action button for product registration is visible over the product list

#### Scenario: Registration FAB navigates

- **WHEN** the merchant taps the registration floating action button
- **THEN** the app navigates to the Product Registration screen

### Requirement: Inventory refresh after successful registration

After a product is successfully registered and the app returns to the Inventory screen, the inventory list SHALL refresh so the newly registered product can appear, and a success snackbar SHALL be shown using the design system's success (green) tones.

#### Scenario: Returning from a successful registration

- **WHEN** the merchant completes a product registration and returns to the Inventory screen
- **THEN** the inventory list re-queries its products and a green success snackbar confirms the registration

#### Scenario: Returning after a plan-limit rejection

- **WHEN** the merchant returns to the Inventory screen because registration was rejected for exceeding the plan's product limit
- **THEN** a message indicating the plan limit was exceeded is shown on the Inventory screen

### Requirement: Product detail entry point

Each product row SHALL be selectable. Tapping a row SHALL navigate to the Product Detail screen for that product, passing only the product id. The row SHALL expose its tappability to accessibility services.

#### Scenario: Tapping a product row

- **WHEN** the merchant taps a product row in the inventory list
- **THEN** the app navigates to the Product Detail screen for that product

#### Scenario: Returning from a saved edit

- **WHEN** the merchant saves an edit on the Product Detail screen and returns to the Inventory screen
- **THEN** the inventory list re-queries its products and a snackbar confirms the product was updated

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

