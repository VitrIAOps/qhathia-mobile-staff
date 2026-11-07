## MODIFIED Requirements

### Requirement: Save a discount with replace semantics

Saving a discount for an existing product SHALL first read the discounts the product has, then create the new one through `POST /api/v1/products/{id}/discounts`, and only after that delete every previously stored discount through `DELETE /api/v1/products/{id}/discounts/{discountId}`. A product therefore never loses its discount when a request fails midway. At worst it keeps two for a while, and the kiosk applies the one that leaves the lower price. The request SHALL send `discountType` as `PERCENTAGE` or `FIXED_AMOUNT` and the entered `value`.

#### Scenario: Product without a previous discount

- **WHEN** a discount is saved for a product that has none
- **THEN** only the create request is issued

#### Scenario: Product with a previous discount

- **WHEN** a discount is saved for a product that already has one
- **THEN** the new discount is created first and the previous one is deleted afterwards

#### Scenario: Creation fails

- **WHEN** creating the new discount fails
- **THEN** no discount is deleted, the product keeps the discount it had, and the app reports the failure

#### Scenario: Deleting the previous discount fails

- **WHEN** the new discount was created but deleting the previous one fails
- **THEN** the app reports the failure, and the product has both discounts until the next save removes the extra one

### Requirement: Remove a discount

Removing a product's discount SHALL delete every discount the product currently has through `DELETE /api/v1/products/{id}/discounts/{discountId}`. A discount that answers `404` on delete SHALL count as already removed. When saving a discount deletes the previous ones, the same rule SHALL apply.

#### Scenario: Discount removed

- **WHEN** the merchant removes the discount of a product that has one
- **THEN** the discount is deleted and the product is shown without a discount

#### Scenario: Removal fails

- **WHEN** the delete request fails for a reason other than `404`
- **THEN** an error message is shown and the product keeps showing its discount

#### Scenario: A discount already deleted elsewhere

- **WHEN** one of the product's discounts answers `404` on delete
- **THEN** it counts as removed, the remaining ones are still deleted, and the removal succeeds
