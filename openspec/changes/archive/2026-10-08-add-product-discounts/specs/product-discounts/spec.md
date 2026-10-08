## ADDED Requirements

### Requirement: Discount types and resulting price

A product discount SHALL be either a percentage of the product price or a fixed amount in Peruvian soles. The resulting price SHALL be the original price minus the discount, rounded to two decimals and never below zero.

#### Scenario: Percentage discount

- **WHEN** a 20 % discount is applied to a price of S/ 189.00
- **THEN** the resulting price is S/ 151.20

#### Scenario: Fixed-amount discount

- **WHEN** a discount of 15 soles is applied to a price of S/ 189.00
- **THEN** the resulting price is S/ 174.00

### Requirement: Apply discount dialog

The app SHALL provide an `Aplicar descuento` dialog showing the product's original price struck through and the resulting price in bold, a selector between `Porcentaje` and `Monto`, an input for the value, and `Cancelar` and `Aplicar` actions. The resulting price SHALL update as the merchant changes the mode or the value. While no valid value is entered, the resulting price SHALL equal the original price.

#### Scenario: Dialog opens for a product without a discount

- **WHEN** the dialog opens for a product that has no discount
- **THEN** `Porcentaje` is selected, the input is empty, the resulting price equals the original price and `Aplicar` is disabled

#### Scenario: Dialog opens for a product with a discount

- **WHEN** the dialog opens for a product that already has a discount
- **THEN** the mode and the value are prefilled from that discount and the resulting price reflects it

#### Scenario: Resulting price follows the input

- **WHEN** the merchant enters or changes a valid value
- **THEN** the bold resulting price updates immediately while the struck-through original price stays unchanged

#### Scenario: Cancel discards the edit

- **WHEN** the merchant taps `Cancelar` or dismisses the dialog
- **THEN** the dialog closes and the product's discount is left as it was before the dialog opened

### Requirement: Percentage mode

In `Porcentaje` mode the dialog SHALL offer three quick options — 10 %, 20 % and 50 % — and SHALL also accept a custom percentage typed by the merchant. A percentage SHALL be a whole number from 1 to 100; 100 % is the limit and yields a resulting price of S/ 0.00.

#### Scenario: Full percentage discount

- **WHEN** the merchant types 100 in the percentage input
- **THEN** the resulting price is S/ 0.00 and `Aplicar` is enabled

#### Scenario: Quick option selected

- **WHEN** the merchant taps the 20 % option
- **THEN** the percentage input shows 20 and the resulting price reflects a 20 % discount

#### Scenario: Custom percentage

- **WHEN** the merchant types 35 in the percentage input
- **THEN** the resulting price reflects a 35 % discount and `Aplicar` is enabled

#### Scenario: Percentage above the maximum

- **WHEN** the merchant types a percentage greater than 100
- **THEN** the input shows a validation error and `Aplicar` is disabled

#### Scenario: Empty or zero percentage

- **WHEN** the percentage input is empty or zero
- **THEN** `Aplicar` is disabled

### Requirement: Fixed-amount mode

In `Monto` mode the dialog SHALL show no quick options and SHALL accept the discount amount in soles typed by the merchant. The amount SHALL be greater than zero, have at most two decimals and not exceed the product's price.

#### Scenario: Valid amount

- **WHEN** the merchant types an amount greater than zero and not greater than the price
- **THEN** the resulting price is the original price minus that amount and `Aplicar` is enabled

#### Scenario: Amount equal to the price

- **WHEN** the merchant types an amount equal to the product's price
- **THEN** the resulting price is S/ 0.00 and `Aplicar` is enabled

#### Scenario: Amount greater than the price

- **WHEN** the merchant types an amount greater than the product's price
- **THEN** the input shows a validation error and `Aplicar` is disabled

#### Scenario: Switching modes

- **WHEN** the merchant switches between `Porcentaje` and `Monto`
- **THEN** the input is cleared, the quick options are shown only in `Porcentaje`, and the resulting price returns to the original price

### Requirement: Discount summary label

Wherever a product's applied discount is summarised, the app SHALL show `Descuento de {percentage}%` for a percentage discount and `Descuento de {amount} soles` for a fixed-amount discount.

#### Scenario: Percentage summary

- **WHEN** a 20 % discount is applied
- **THEN** the summary reads `Descuento de 20%`

#### Scenario: Amount summary

- **WHEN** a discount of 15 soles is applied
- **THEN** the summary reads `Descuento de 15 soles`

### Requirement: Read the current discount

The app SHALL read a product's discounts from `GET /api/v1/products/{id}/discounts` using the authenticated HTTP client. The product's current discount SHALL be the active discount that has not ended; when there is none, the product SHALL be treated as having no discount.

#### Scenario: Product with an active discount

- **WHEN** the discounts request returns an active discount whose end date is in the future
- **THEN** that discount is the product's current discount

#### Scenario: Product with no usable discount

- **WHEN** the discounts request returns an empty list, or only inactive or already-ended discounts
- **THEN** the product is treated as having no discount

### Requirement: Save a discount with replace semantics

Saving a discount for an existing product SHALL first delete every discount the product currently has through `DELETE /api/v1/products/{id}/discounts/{discountId}` and then create the new one through `POST /api/v1/products/{id}/discounts`, so a product never holds more than one discount created by the app. The request SHALL send `discountType` as `PERCENTAGE` or `FIXED_AMOUNT` and the entered `value`.

#### Scenario: Product without a previous discount

- **WHEN** a discount is saved for a product that has none
- **THEN** only the create request is issued

#### Scenario: Product with a previous discount

- **WHEN** a discount is saved for a product that already has one
- **THEN** the existing discount is deleted first and the new one is created afterwards

#### Scenario: Creation fails after the deletion

- **WHEN** the existing discount was deleted but creating the new one fails
- **THEN** the app reports the failure and shows the product as having no discount

### Requirement: One-week discount duration

Every discount created by the app SHALL start at the moment it is saved and end exactly seven days later. The merchant SHALL NOT be asked for start or end dates.

#### Scenario: Duration of a new discount

- **WHEN** a discount is created
- **THEN** the request's `startsAt` is the current instant and its `endsAt` is seven days after `startsAt`

### Requirement: Remove a discount

Removing a product's discount SHALL delete every discount the product currently has through `DELETE /api/v1/products/{id}/discounts/{discountId}`.

#### Scenario: Discount removed

- **WHEN** the merchant removes the discount of a product that has one
- **THEN** the discount is deleted and the product is shown without a discount

#### Scenario: Removal fails

- **WHEN** the delete request fails
- **THEN** an error message is shown and the product keeps showing its discount
