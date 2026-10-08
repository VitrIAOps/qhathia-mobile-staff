## ADDED Requirements

### Requirement: Discount toggle on the registration form

The Product Registration form SHALL show a `Descuento` toggle directly below the price field, off by default. The toggle SHALL be disabled while the price field does not hold a valid price greater than zero. Turning the toggle on SHALL open the `Aplicar descuento` dialog for the entered price. A discount is optional and SHALL NOT be required to submit the form.

#### Scenario: Toggle is off by default

- **WHEN** the registration form opens
- **THEN** the `Descuento` toggle is off and no discount summary is shown

#### Scenario: Toggle disabled without a price

- **WHEN** the price field is empty or invalid
- **THEN** the `Descuento` toggle is disabled and cannot be turned on

#### Scenario: Toggle enabled with a valid price

- **WHEN** the merchant has entered a valid price
- **THEN** the `Descuento` toggle is enabled

#### Scenario: Turning the toggle on opens the dialog

- **WHEN** the merchant turns the toggle on
- **THEN** the `Aplicar descuento` dialog opens showing the entered price as the original price

#### Scenario: Cancelling the dialog without a discount

- **WHEN** the merchant cancels the dialog and no discount had been applied before
- **THEN** the toggle returns to off

### Requirement: Discount summary on the registration form

After a discount is applied in the dialog, the dialog SHALL close and the form SHALL show, on one line below the toggle, the discount summary at the start and the resulting final price (the entered price with the discount applied, as `S/ {amount}`) at the end, spaced apart, with the toggle on. The final price SHALL follow changes to the price field. Tapping the summary line SHALL reopen the dialog prefilled with the applied discount so it can be changed before submitting. Turning the toggle off SHALL remove the discount and hide the summary. Applying, changing or removing a discount SHALL mark the form as having unsaved changes.

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

#### Scenario: Price change invalidates the discount

- **WHEN** the merchant edits the price so that it is no longer valid, or so that an applied fixed amount is greater than the new price
- **THEN** the discount is removed, the summary is hidden and the toggle returns to off

### Requirement: Discount creation on registration

When the form is submitted with a discount applied, the app SHALL create the discount for the newly registered product immediately after the product registration succeeds, with the one-week duration. When no discount is applied, no discount request SHALL be issued.

#### Scenario: Registration with a discount

- **WHEN** the merchant submits a valid form with a discount applied and the product registration succeeds
- **THEN** the discount is created for the new product, and the app returns to the inventory list with the registration success message

#### Scenario: Registration without a discount

- **WHEN** the merchant submits a valid form with the toggle off
- **THEN** only the product registration request is issued

#### Scenario: Product registered but the discount fails

- **WHEN** the product registration succeeds but creating the discount fails
- **THEN** the app returns to the inventory list and shows a message stating that the product was registered but the discount could not be applied

#### Scenario: Registration fails

- **WHEN** the product registration fails
- **THEN** no discount request is issued and the applied discount stays on the form

### Requirement: Stock field label

The product quantity field on the Product Registration form SHALL be labelled `Stock`.

#### Scenario: Label shown on the form

- **WHEN** the registration form is displayed
- **THEN** the quantity field's label reads `Stock`

## MODIFIED Requirements

### Requirement: Required field validation

The form SHALL require a non-blank product name, a price greater than zero, a quantity that is not negative, at least one category and at least one image before registration is allowed. Tags SHALL be optional. Validation SHALL run per field as the merchant edits or selects it, and the submit action SHALL remain disabled until every rule is satisfied.

#### Scenario: Submit disabled while incomplete

- **WHEN** any required field (name, price, quantity, category or image) is missing or invalid
- **THEN** the submit action is disabled and the offending field shows a validation message once the merchant has interacted with it

#### Scenario: Price must be greater than zero

- **WHEN** the merchant enters a price of zero, a negative value, or a non-numeric value
- **THEN** the price field shows a validation error and the form cannot be submitted

#### Scenario: Submit enabled when valid

- **WHEN** the merchant has provided a non-blank name, a price greater than zero, a non-negative quantity, at least one category and at least one image
- **THEN** the submit action becomes enabled, whether or not any tag was entered

### Requirement: Tag entry

The form SHALL provide a free-text tag field (per the Figma mockup) where the merchant types zero or more tags separated by commas. Tags SHALL be optional. The entered tags SHALL be parsed into individual trimmed, non-blank values for the product registration request, and when none are entered the request SHALL carry an explicit empty tag list.

#### Scenario: Enter one or more tags

- **WHEN** the merchant types one or more comma-separated tags
- **THEN** the tags are parsed into trimmed, non-blank values and included in the product registration request

#### Scenario: No tag entered

- **WHEN** the tag field is empty or contains only separators/whitespace
- **THEN** submission is not blocked and the product registration request carries an empty tag list
