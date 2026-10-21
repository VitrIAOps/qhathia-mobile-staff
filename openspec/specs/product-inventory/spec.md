# product-inventory Specification

## Purpose

Defines how a store owner views and filters the store's inventory in the Qhathia staff app: the Inventory screen that lists products from the Products API, category and availability filtering, name search, a live product count, the post-login app shell (bottom navigation, header, registration FAB), and the product row's stock and discounted price as returned by the Products API.
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

After a product is successfully registered and the app returns to the Inventory screen, the inventory list SHALL refresh so the newly registered product can appear, and a success snackbar reading `Producto registrado correctamente` SHALL be shown using the design system's success (green) tones. That snackbar SHALL carry a text action labelled `Ver producto`, SHALL stay visible long enough to be tapped, and SHALL still dismiss on its own when the action is not used. The plan-limit and product-updated messages SHALL NOT carry this action.

#### Scenario: Returning from a successful registration

- **WHEN** the merchant completes a product registration and returns to the Inventory screen
- **THEN** the inventory list re-queries its products and a green success snackbar confirms the registration with a `Ver producto` action

#### Scenario: Snackbar dismisses without the action

- **WHEN** the success snackbar is shown and the merchant does not tap `Ver producto`
- **THEN** the snackbar dismisses on its own and the list keeps its scroll position and filters

#### Scenario: Returning after a plan-limit rejection

- **WHEN** the merchant returns to the Inventory screen because registration was rejected for exceeding the plan's product limit
- **THEN** a message indicating the plan limit was exceeded is shown on the Inventory screen, without a `Ver producto` action

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

Selecting the discount option (`Registrar descuento` or `Editar descuento`) SHALL open the `Aplicar descuento` dialog using the row's price as the original price, prefilled with the current discount when the product has one; the discount SHALL be loaded first if opening the menu did not already resolve it. Applying SHALL save the discount with replace semantics and confirm with a snackbar. After a successful save the product list SHALL be re-read with the active filters, so the row shows the discounted price computed by the server; this re-read SHALL keep the current rows on screen until the new ones arrive instead of replacing the list with a loading state, and SHALL keep the search text and the scroll position. If that re-read fails, the current rows SHALL stay on screen and no error state SHALL replace them.

#### Scenario: Product with a discount

- **WHEN** the merchant selects `Editar descuento` on a product that has a discount
- **THEN** the dialog opens prefilled with that discount

#### Scenario: Product without a discount

- **WHEN** the merchant selects `Registrar descuento` on a product that has no discount
- **THEN** the dialog opens empty so a discount can be created

#### Scenario: Discount applied

- **WHEN** the merchant applies a value in the dialog and the save succeeds
- **THEN** the dialog closes and a snackbar confirms the discount was updated

#### Scenario: Row reflects the saved discount

- **WHEN** a discount save from the row menu succeeds
- **THEN** the product list is re-read without showing the full-screen loading state, and the row then shows the original price struck through and the server's discounted price

#### Scenario: Re-read after the save fails

- **WHEN** a discount save succeeds but the following re-read of the product list fails
- **THEN** the rows already on screen stay visible and the list does not switch to its error state

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

When the app returns to the Inventory screen after a product was registered but its discount could not be created, the inventory list SHALL refresh and SHALL show a message distinct from the plain registration success message. Because the product exists, that message SHALL carry the same `Ver producto` action as the registration success snackbar, with the same visibility and dismissal behaviour.

#### Scenario: Returning from a registration with a failed discount

- **WHEN** the merchant returns to the Inventory screen because the product was registered but its discount could not be applied
- **THEN** the inventory list re-queries its products and a message states that the product was registered but the discount could not be applied, with a `Ver producto` action

### Requirement: View the registered product from the registration snackbar

Tapping `Ver producto` on the registration success snackbar, or on the discount-failed registration snackbar, SHALL locate the newly registered product, by its id, in the inventory list and SHALL scroll the list to that product's row. The action SHALL NOT navigate away from the Inventory screen. If the name search is hiding the product, the search SHALL be cleared so the row can be shown, without issuing a request. If the active category or availability filter excludes the product from the loaded list, the category filter SHALL return to `Todas`, the availability sub-filter and the search SHALL be cleared, the list SHALL be re-queried without filters and SHALL then scroll to the product's row. If the post-registration refresh is still in flight when the action is tapped, the scroll SHALL happen once the refreshed list is available. If the product is still not in the list with no filters active, the list SHALL NOT scroll and a message SHALL say the product could not be found in the list.

#### Scenario: Product is in the visible list

- **WHEN** the merchant taps `Ver producto` and the registered product is among the visible rows of the list
- **THEN** the list scrolls to that product's row and the merchant stays on the Inventory screen

#### Scenario: Product is hidden by the search

- **WHEN** the merchant taps `Ver producto` while a search text hides the registered product, which is in the loaded list
- **THEN** the search field is cleared, the full loaded list is shown and it scrolls to that product's row, without a new request

#### Scenario: Product is excluded by the active filters

- **WHEN** the merchant taps `Ver producto` and the registered product is not in the loaded list because of the selected category or availability filter
- **THEN** the `Todas` category becomes selected, the availability sub-filter and the search are cleared, the list is re-queried without filters and it scrolls to that product's row

#### Scenario: Action tapped while the list is still refreshing

- **WHEN** the merchant taps `Ver producto` before the post-registration refresh has finished
- **THEN** the list scrolls to the product's row as soon as the refreshed list arrives

#### Scenario: Action from the discount-failed snackbar

- **WHEN** the merchant taps `Ver producto` on the snackbar stating the product was registered but the discount could not be applied
- **THEN** the list scrolls to that product's row exactly as it does from the success snackbar

#### Scenario: Load fails after the action was tapped

- **WHEN** the merchant taps `Ver producto` and the list load it is waiting for fails
- **THEN** the list does not scroll and the usual inventory load error is shown

#### Scenario: Product is not in the unfiltered list

- **WHEN** the merchant taps `Ver producto`, no category or availability filter is active and the registered product is not in the loaded list
- **THEN** the list does not scroll, no further request is issued and a message says the product could not be found in the list

### Requirement: Product stock indicator

Each product row SHALL show the product's stock as returned in the `stock` field of `GET /api/v1/products`, in the form "N stock". The app SHALL NOT derive, estimate or default a stock number: when `stock` is absent or `null` (legacy products and "consult" mode) the row SHALL show no stock indicator, and when `stock` is `0` the row SHALL read `Sin existencias` instead of a number. The `availability` enum (`AVAILABLE`, `LOW_STOCK`, `CONSULT`) SHALL continue to drive the availability sub-filter.

#### Scenario: Row shows the real stock

- **WHEN** a product row is rendered for a product whose `stock` is 1
- **THEN** the row shows `1 stock`

#### Scenario: List and detail agree

- **WHEN** the merchant reads a product's stock on its Inventory row and then opens that product's detail
- **THEN** the stock on the detail form is the same number shown on the row

#### Scenario: Stock unknown

- **WHEN** a product row is rendered for a product whose `stock` is `null` or missing from the response
- **THEN** the row shows no stock indicator and no number

#### Scenario: Out of stock

- **WHEN** a product row is rendered for a product whose `stock` is 0
- **THEN** the row shows `Sin existencias` and does not show `0 stock`

### Requirement: Discounted price on the product row

When a product has a discount in effect, its Inventory row SHALL show the original `price` struck through and the `discountedPrice` from `GET /api/v1/products` highlighted next to it. The discounted price SHALL be the value sent by the server; the app SHALL NOT compute it for the row from the product's discounts. When `discountedPrice` is absent or `null`, the row SHALL show only the price, unstyled. Screen readers SHALL announce both prices with their meaning (original price and price with discount), not just two numbers.

#### Scenario: Product with a discount in effect

- **WHEN** a product row is rendered for a product with `price` 35.90 and `discountedPrice` 30.90
- **THEN** the row shows `S/ 35.90` struck through and `S/ 30.90` highlighted

#### Scenario: Product without a discount

- **WHEN** a product row is rendered for a product whose `discountedPrice` is `null`
- **THEN** the row shows only `S/ 35.90`, not struck through

#### Scenario: Screen reader on a discounted row

- **WHEN** a screen reader focuses the price of a discounted row
- **THEN** it announces the original price and the price with discount as such

