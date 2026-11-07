## MODIFIED Requirements

### Requirement: Discount summary on the registration form

After a discount is applied in the dialog, the dialog SHALL close and the form SHALL show, on one line below the toggle, the discount summary at the start and the resulting final price (the entered price with the discount applied, as `S/ {amount}`) at the end, spaced apart, with the toggle on. The final price SHALL follow changes to the price field. Tapping the summary line SHALL reopen the dialog prefilled with the applied discount so it can be changed before submitting. Turning the toggle off SHALL remove the discount and hide the summary. Applying, changing or removing a discount SHALL mark the form as having unsaved changes.

Editing the price SHALL NOT change the applied discount. While the price is empty or not yet a valid amount, the toggle SHALL stay on but disabled, and the summary SHALL be shown without a final price. When a fixed-amount discount is greater than the price entered, the row SHALL show, below the summary and in the error color, `El descuento es mayor que el precio. Edítalo o quítalo.`

#### Scenario: Percentage discount applied

- **WHEN** the merchant applies a 20 % discount
- **THEN** the dialog closes, the toggle stays on and `Descuento de 20%` is shown below it

#### Scenario: Final price next to the summary

- **WHEN** the price is S/ 189.00 and a 20 % discount is applied
- **THEN** the line below the toggle shows `Descuento de 20%` at the start and `S/ 151.20` at the end

#### Scenario: Final price follows the price field

- **WHEN** the merchant changes the price to S/ 200.00 with a 20 % discount applied
- **THEN** the final price shown next to the summary becomes `S/ 160.00`

#### Scenario: Amount discount applied

- **WHEN** the merchant applies a discount of 15 soles
- **THEN** the dialog closes, the toggle stays on and `Descuento de 15 soles` is shown below it

#### Scenario: Editing the applied discount

- **WHEN** the merchant taps the discount summary
- **THEN** the dialog reopens prefilled with the applied discount, and applying a new value replaces the summary

#### Scenario: Turning the toggle off

- **WHEN** the merchant turns the toggle off
- **THEN** the discount is removed and the summary is hidden

#### Scenario: Clearing the price keeps the discount

- **WHEN** the price is 189.90 with a 35 % discount and the merchant clears it and types 200
- **THEN** the toggle stays on, `Descuento de 35%` stays shown, and the final price becomes `S/ 130.00`

#### Scenario: Typing a decimal price keeps the discount

- **WHEN** the price goes from `150` through `150.` to `150.5` with a discount applied
- **THEN** the discount stays applied at every step

#### Scenario: Fixed amount greater than the price

- **WHEN** a fixed discount of 900 is applied on a price of 1000 and the price is changed to 100
- **THEN** the discount stays applied and the row shows `El descuento es mayor que el precio. Edítalo o quítalo.`

### Requirement: Discount creation on registration

When the form is submitted with a discount applied, the app SHALL create the discount for the newly registered product immediately after the product registration succeeds, with the one-week duration. When no discount is applied, no discount request SHALL be issued.

Before any request, when a fixed-amount discount is greater than the price entered, submitting SHALL remove the discount from the form, show `Quitamos el descuento porque es mayor que el nuevo precio. Revisa y vuelve a agregar.` and send nothing.

#### Scenario: Registration with a discount

- **WHEN** the merchant submits a valid form with a discount applied and the product registration succeeds
- **THEN** the discount is created for the new product, and the app returns to the inventory list with the registration success message

#### Scenario: Registration with a discount after correcting the price

- **WHEN** the merchant applies a 35 % discount, clears the price, types 200 and submits
- **THEN** the product is registered and the 35 % discount is created for it

#### Scenario: Registration with a fixed amount over the price

- **WHEN** the merchant submits with a fixed discount of 900 and a price of 100
- **THEN** no request is issued, the discount is removed from the form and the message `Quitamos el descuento porque es mayor que el nuevo precio. Revisa y vuelve a agregar.` is shown

#### Scenario: Registration without a discount

- **WHEN** the merchant submits a valid form with the toggle off
- **THEN** only the product registration request is issued

#### Scenario: Product registered but the discount fails

- **WHEN** the product registration succeeds but creating the discount fails
- **THEN** the app returns to the inventory list and shows a message stating that the product was registered but the discount could not be applied

#### Scenario: Registration fails

- **WHEN** the product registration fails
- **THEN** no discount request is issued and the applied discount stays on the form
