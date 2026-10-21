## ADDED Requirements

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

## MODIFIED Requirements

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

## REMOVED Requirements

### Requirement: Product stock indicator (mocked)

**Reason**: `GET /api/v1/products` returns the real `stock` per row since 2026-10-18. The mocked value (derived from the product id's hash) was being read as real stock and led to accepting an order that could not be fulfilled (triage BUG-019/BUG-021).

**Migration**: Replaced by the `Product stock indicator` requirement, which reads `stock` from the API. The `mockedStock` helper is deleted.
