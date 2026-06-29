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

The post-login experience SHALL present a bottom navigation bar with the destinations Inicio, Inventario, Solicitudes, Dispositivos and Perfil, with Inventario marked active while the Inventory screen is shown, and an application header containing the screen title and a notification icon.

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
