# product-detail Specification

## Purpose

Defines how a store owner opens one of the store's products from the inventory list and edits it in the Qhathia staff app: loading the full product from the Products API, the editable form (name, description with AI optimization, category, price, stock and tags), saving through a full-replace update, and the read-only view of the product's registered photos.
## Requirements
### Requirement: Product detail load

The Product Detail screen SHALL retrieve the product from `GET /api/v1/products/{id}` using the id passed on the navigation route, and seed its form from the response. Only the product id SHALL cross the navigation boundary — never the product itself. The request SHALL be authenticated with the merchant's Bearer token via the existing authenticated HTTP client.

#### Scenario: Product loads successfully

- **WHEN** the merchant opens the Product Detail screen and the request succeeds
- **THEN** the form is populated with the product's name, description, category, price, stock and tags, and its photos are displayed

#### Scenario: Product request fails

- **WHEN** the product request fails due to a network or server error
- **THEN** the screen displays a retryable error state instead of the form

#### Scenario: Loading indicator while fetching

- **WHEN** the product request is in flight
- **THEN** the screen shows a loading indicator in place of the form

#### Scenario: Categories fail to load

- **WHEN** the product loads but the category catalog request fails
- **THEN** the form is still shown, with the category selector in a retryable error state

### Requirement: Product editing

The Product Detail screen SHALL let the merchant edit the product's name, description, category, price, stock and tags, validating each field as it is edited using the same rules as product registration. Tags SHALL be optional: clearing the tag field SHALL save the product with an explicit empty tag list. Photos SHALL be shown read-only; adding or removing them is not offered on this screen.

#### Scenario: Saving is blocked until the form is valid

- **WHEN** any required field (name, price or stock) is empty or invalid, or no category is selected
- **THEN** the save action is disabled and the offending field shows its validation message

#### Scenario: Saving with no tags

- **WHEN** the merchant clears the tag field on an otherwise valid form and saves
- **THEN** the save is allowed and the update request carries an empty tag list

#### Scenario: Saving is blocked with no changes

- **WHEN** the product has just loaded and nothing has been edited
- **THEN** the save action is disabled

#### Scenario: Optimizing the description with AI

- **WHEN** the merchant triggers the AI action on a product that has a description
- **THEN** the description is rewritten through `POST /api/v1/products/regenerate-description` and replaced in the form, and a failure leaves the original description untouched and shows an error message

#### Scenario: A product with no photos

- **WHEN** the loaded product has no registered media
- **THEN** the photos section shows an explanatory placeholder instead of an empty strip

### Requirement: Product update

Saving SHALL replace the product through `PUT /api/v1/products/{id}`. Because the endpoint is a full replace, the request SHALL carry every editable field, including those the form does not expose, which are carried over unchanged from the loaded product. `availability` SHALL be omitted so the backend derives it from the submitted stock, matching the behaviour on create.

#### Scenario: Successful save

- **WHEN** the merchant saves a valid form and the request succeeds
- **THEN** the app returns to the Inventory screen and signals that the product was updated

#### Scenario: Fields the form does not expose survive the save

- **WHEN** the product is saved
- **THEN** the request carries the product's discount-inquiry flag and position as loaded, rather than resetting them to defaults

#### Scenario: Save fails

- **WHEN** the update request fails
- **THEN** the merchant stays on the form with the entered values intact and a message reflecting the cause (not found, invalid input, no connectivity, or a generic failure) is shown

### Requirement: Discard guard

A form with unsaved changes SHALL intercept cancel and system back with a discard-confirmation dialog; a pristine form SHALL navigate back immediately.

#### Scenario: Leaving a dirty form

- **WHEN** the merchant presses back or cancel after editing any field
- **THEN** a dialog asks whether to discard the changes, and only confirming leaves the screen

#### Scenario: Leaving a pristine form

- **WHEN** the merchant presses back or cancel without editing anything
- **THEN** the app returns to the Inventory screen without a dialog

### Requirement: Discount shown on product detail

The Product Detail screen SHALL load the product's current discount together with the product and SHALL show a `Descuento` toggle directly below the price field. When the product has a current discount the toggle SHALL be on and the discount summary SHALL be shown below it, with the resulting final price (the form's price with the discount applied) at the end of the same line; otherwise the toggle SHALL be off. The toggle SHALL be disabled while the price field does not hold a valid price. A failure to load the discount SHALL NOT block the form.

#### Scenario: Product with a discount

- **WHEN** the product loads and has a current discount of 20 %
- **THEN** the toggle is on and `Descuento de 20%` is shown below it

#### Scenario: Product without a discount

- **WHEN** the product loads and has no current discount
- **THEN** the toggle is off and no summary is shown

#### Scenario: Discount fails to load

- **WHEN** the product loads but the discount request fails
- **THEN** the form is still shown and the discount control is disabled with a retry affordance

### Requirement: Discount editing on product detail

Turning the toggle on, or tapping the discount summary, SHALL open the `Aplicar descuento` dialog for the price currently entered in the form, prefilled with the discount currently shown on the form when there is one. Applying in the dialog SHALL only update the form: it SHALL close the dialog, show the new summary and mark the form as having unsaved changes. Turning the toggle off SHALL likewise only update the form, hiding the summary and marking the form as having unsaved changes. No discount request SHALL be issued until the merchant saves the form.

#### Scenario: Adding a discount to the form

- **WHEN** the merchant turns the toggle on for a product without a discount and applies a value in the dialog
- **THEN** the dialog closes, the toggle is on, the summary shows the new discount, the save action is enabled and no discount request has been issued

#### Scenario: Changing the discount on the form

- **WHEN** the merchant taps the discount summary and applies a different value
- **THEN** the summary shows the new discount, the save action is enabled and no discount request has been issued

#### Scenario: Turning the toggle off

- **WHEN** the merchant turns the toggle off for a product with a discount
- **THEN** the summary is hidden, the save action is enabled and no discount request has been issued

#### Scenario: Cancelling the dialog

- **WHEN** the merchant cancels the dialog
- **THEN** the toggle and the summary are left as they were before the dialog opened

#### Scenario: Price change invalidates the pending discount

- **WHEN** the merchant edits the price so that it is no longer valid, or so that the discount's fixed amount is greater than the new price
- **THEN** the discount is removed from the form, the summary is hidden and the toggle returns to off

### Requirement: Discount persisted on save

Saving the form SHALL persist the discount change after the product update succeeds: a new or changed discount SHALL be saved with replace semantics and the one-week duration, and a discount turned off SHALL be removed. When the discount shown on the form is the same as the one loaded, no discount request SHALL be issued. When the discount failed to load, saving SHALL NOT issue any discount request.

#### Scenario: Save with a new or changed discount

- **WHEN** the merchant saves a form whose discount differs from the one loaded and both requests succeed
- **THEN** the product is updated, the previous discount (if any) is deleted, the new one is created, and the app returns to the Inventory screen signalling that the product was updated

#### Scenario: Save with the discount turned off

- **WHEN** the merchant saves a form where a previously loaded discount was turned off
- **THEN** the product is updated, the discount is deleted, and the app returns to the Inventory screen

#### Scenario: Save with an untouched discount

- **WHEN** the merchant saves a form without having changed the discount
- **THEN** only the product update request is issued

#### Scenario: Product update fails

- **WHEN** the product update request fails
- **THEN** no discount request is issued and the merchant stays on the form with the entered values and the pending discount intact

#### Scenario: Product saved but the discount fails

- **WHEN** the product update succeeds but saving or removing the discount fails
- **THEN** the merchant stays on the form, a message states that the product was saved but the discount could not be updated, and the form remains with unsaved changes so saving can be retried

### Requirement: Discount changes covered by the discard guard

A discount change that has not been saved SHALL count as an unsaved change for the discard-confirmation dialog, and confirming the discard SHALL leave the product's stored discount untouched.

#### Scenario: Leaving with only a discount change

- **WHEN** the merchant changes only the discount and presses back or cancel
- **THEN** the discard-confirmation dialog is shown

#### Scenario: Discarding a discount change

- **WHEN** the merchant confirms the discard after changing the discount
- **THEN** the screen is left, no discount request is issued and the product keeps the discount it had when it was loaded

### Requirement: Stock field label

The product quantity field on the Product Detail form SHALL be labelled `Stock`.

#### Scenario: Label shown on the form

- **WHEN** the product detail form is displayed
- **THEN** the quantity field's label reads `Stock`

