## MODIFIED Requirements

### Requirement: Discount shown on product detail

The Product Detail screen SHALL load the product's current discount together with the product and SHALL show a `Descuento` toggle directly below the price field. When the product has a current discount the toggle SHALL be on and the discount summary SHALL be shown below it, with the resulting final price at the end of the same line; otherwise the toggle SHALL be off. The toggle SHALL be disabled while the price field does not hold a valid price. A failure to load the discount SHALL NOT block the form.

While the form's price and discount are the ones loaded with the product, the final price SHALL be the `discountedPrice` returned by `GET /api/v1/products/{id}`; when that field is `null` the final price SHALL be the product's regular price, because no discount is in force. Once the merchant edits the price or the discount, the final price SHALL be the form's price with the form's discount applied. After a save that updated the product, the final price SHALL no longer come from the value loaded with the product.

#### Scenario: Product with a discount

- **WHEN** the product loads and has a current discount of 20 %
- **THEN** the toggle is on and `Descuento de 20%` is shown below it

#### Scenario: Product without a discount

- **WHEN** the product loads and has no current discount
- **THEN** the toggle is off and no summary is shown

#### Scenario: Discount fails to load

- **WHEN** the product loads but the discount request fails
- **THEN** the form is still shown and the discount control is disabled with a retry affordance

#### Scenario: Final price comes from the server on load

- **WHEN** the product loads with a price of S/ 35.90, a current discount and a `discountedPrice` of S/ 30.90
- **THEN** the final price shown at the end of the summary line is S/ 30.90, without being computed on the phone

#### Scenario: Server reports no discount in force

- **WHEN** the product loads with a current discount but its `discountedPrice` is `null`
- **THEN** the toggle is on, the summary is shown, and the final price shown is the product's regular price

#### Scenario: Final price follows a price edit

- **WHEN** the merchant changes the price of a product loaded with a discount
- **THEN** the final price shown is the new price with the form's discount applied

#### Scenario: Final price follows a discount edit

- **WHEN** the merchant applies a different discount in the `Aplicar descuento` dialog
- **THEN** the final price shown is the form's price with the new discount applied

#### Scenario: Returning to the loaded values

- **WHEN** the merchant edits the price and then types back exactly the loaded price, with the discount untouched
- **THEN** the final price shown is again the server's `discountedPrice`

#### Scenario: Retrying the discount after a failed load

- **WHEN** the discount failed to load and the merchant retries successfully
- **THEN** the final price shown is the server's `discountedPrice` loaded with the product

#### Scenario: Form kept after a partially failed save

- **WHEN** the merchant saves, the product update succeeds but the discount save fails, and the merchant stays on the form
- **THEN** the final price shown is the form's price with the form's discount applied
