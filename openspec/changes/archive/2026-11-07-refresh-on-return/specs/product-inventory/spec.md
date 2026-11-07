## MODIFIED Requirements

### Requirement: Product list display

The Inventory screen SHALL retrieve the authenticated store's products from `GET /api/v1/products` and display each product as a row showing its primary image, name, price and availability indicator. The request SHALL be authenticated with the merchant's Bearer token via the existing authenticated HTTP client.

The list SHALL be read again, quietly (keeping the rows on screen, with the active filters and search), each time the screen starts again after its first start: back to the tab, back from a pushed screen with Back, or back from the background. Pulling the list down SHALL read it again too, showing the pull indicator until the answer arrives. Pulling SHALL work on the empty and error states as well. A pull that fails SHALL keep the rows and say so; a quiet reload that fails SHALL say nothing.

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

#### Scenario: Back to the tab

- **WHEN** a product was created elsewhere and the merchant goes from Inicio back to Inventario
- **THEN** the list is read again and the new product is listed, without a loading state replacing the rows

#### Scenario: Back from a product with Back

- **WHEN** a product was deleted elsewhere while its detail was open, and the merchant comes back with Back
- **THEN** the list is read again and the product is no longer listed

#### Scenario: First start does not read twice

- **WHEN** the Inventory screen opens for the first time
- **THEN** the products are requested once

#### Scenario: Pull to refresh

- **WHEN** the merchant pulls the list down, including when it shows "0 productos"
- **THEN** the products are requested again and the pull indicator shows until the answer arrives

#### Scenario: Pull to refresh fails

- **WHEN** a pull-to-refresh request fails
- **THEN** the rows on screen stay and an error message is shown
