## MODIFIED Requirements

### Requirement: Product editing

The Product Detail screen SHALL let the merchant edit the product's name, description, category, price, stock and tags, validating each field as it is edited using the same rules as product registration. Tags SHALL be optional: clearing the tag field SHALL save the product with an explicit empty tag list. Photos SHALL be editable as described in "Photo editing on product detail".

A product loaded without stock ("consultar disponibilidad") SHALL be savable with the stock field empty: the field SHALL show the placeholder `A consultar` and the hint `Déjalo vacío para seguir mostrándolo como «Consultar disponibilidad»`. A product loaded with a stock SHALL still require a valid stock to save.

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
- **THEN** the photos section shows an explanatory placeholder and the `Agregar` tile

#### Scenario: Editing a "consultar disponibilidad" product

- **WHEN** a product loaded with no stock has its price changed from 50 to 55 and the stock field left empty
- **THEN** the save action is enabled, the update request carries no `stock`, and the stock field shows `A consultar` with its hint

#### Scenario: A stocked product still needs a stock

- **WHEN** a product loaded with stock 10 has its stock field cleared
- **THEN** the save action is disabled and the stock field shows its validation message

### Requirement: Product update

Saving SHALL update the product through `PUT /api/v1/products/{id}`, whose absent fields the backend keeps. The request SHALL carry the name, price, category, tags, the description (an empty string when the merchant cleared it, so it is removed) and the `version` read with the product. It SHALL carry `stock` only when the merchant changed the stock field, so a sale confirmed while the form was open is not undone. It SHALL NOT carry fields the form does not expose (discount-inquiry flag, position, brand), so the backend keeps them. `availability` SHALL be omitted so the backend derives it from the stock.

When the backend answers `409` with `errorCode` `PRODUCT_VERSION_CONFLICT` or `CONCURRENT_MODIFICATION`, the app SHALL NOT show a generic error. It SHALL open the dialog `Otro usuario cambió este producto` with the text `Mientras lo editabas, otra persona (u otra venta) lo modificó.` and three actions:
- `Ver lo nuevo` SHALL discard the merchant's edits and reload the product.
- `Guardar lo mío` SHALL save the form again with the server's current version: the `currentVersion` from the error, or, when the error carries none, the version of a fresh read of the product. It SHALL still omit `stock` when the merchant did not change it.
- `Seguir editando` SHALL close the dialog and keep the edits.

When a save updated the product but the merchant stays on the form (a photo or discount step failed), the form SHALL adopt the product's current version before the next save, so retrying does not conflict with the merchant's own update.

#### Scenario: Successful save

- **WHEN** the merchant saves a valid form and the request succeeds
- **THEN** the app returns to the Inventory screen and signals that the product was updated

#### Scenario: Version travels with the update

- **WHEN** the merchant saves a product loaded with `version` 7
- **THEN** the update request carries `version` 7

#### Scenario: Fields the form does not expose survive the save

- **WHEN** the product is saved
- **THEN** the request carries neither the discount-inquiry flag, the position nor the brand, and the backend keeps the stored values

#### Scenario: Untouched stock is not sent

- **WHEN** the merchant changes only the name and saves
- **THEN** the update request carries no `stock`

#### Scenario: Clearing the description

- **WHEN** the merchant empties the description and saves
- **THEN** the update request carries `description` as an empty string

#### Scenario: Another user changed the product

- **WHEN** the update answers `409 PRODUCT_VERSION_CONFLICT` with `currentVersion` 8
- **THEN** the dialog `Otro usuario cambió este producto` opens, no error message is shown, and the edits stay on the form

#### Scenario: See the newer product

- **WHEN** the merchant chooses `Ver lo nuevo`
- **THEN** the dialog closes and the form is reloaded from the server, discarding the edits

#### Scenario: Keep my changes

- **WHEN** the merchant chooses `Guardar lo mío` after a conflict with `currentVersion` 8, having changed only the name
- **THEN** the update is sent again with `version` 8 and without `stock`

#### Scenario: Keep my changes after a concurrent write

- **WHEN** the conflict was `CONCURRENT_MODIFICATION` with no `currentVersion` and the merchant chooses `Guardar lo mío`
- **THEN** the product is read again and the update is sent with the version just read

#### Scenario: Keep editing

- **WHEN** the merchant chooses `Seguir editando`
- **THEN** the dialog closes and the form keeps the edits with unsaved changes

#### Scenario: Retry after a partial save

- **WHEN** the update succeeds, the discount step fails, and the merchant saves again
- **THEN** the second update carries the product's version after the first update, not the version loaded at first

#### Scenario: Save fails

- **WHEN** the update request fails for any other reason
- **THEN** the merchant stays on the form with the entered values intact and a message reflecting the cause (not found, invalid input, no connectivity, or a generic failure) is shown

### Requirement: Discount editing on product detail

Turning the toggle on, or tapping the discount summary, SHALL open the `Aplicar descuento` dialog for the price currently entered in the form, prefilled with the discount currently shown on the form when there is one. Applying in the dialog SHALL only update the form: it SHALL close the dialog, show the new summary and mark the form as having unsaved changes. Turning the toggle off SHALL likewise only update the form, hiding the summary and marking the form as having unsaved changes. No discount request SHALL be issued until the merchant saves the form.

Editing the price SHALL NOT change the discount on the form. While the price is empty or not yet a valid amount, the toggle SHALL stay on but disabled, and the summary SHALL be shown without a final price. When a fixed-amount discount is greater than the price entered, the row SHALL show, below the summary and in the error color, `El descuento es mayor que el precio. Edítalo o quítalo.`

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

#### Scenario: Clearing the price keeps the discount

- **WHEN** a product with a 35 % discount has its price cleared and then set to 200
- **THEN** the toggle stays on throughout, `Descuento de 35%` stays shown, and the final price becomes `S/ 130.00`

#### Scenario: Typing a decimal price keeps the discount

- **WHEN** the price goes from `150` through `150.` to `150.5` with a discount on the form
- **THEN** the discount stays on the form at every step

#### Scenario: Fixed amount greater than the price

- **WHEN** a product with a fixed discount of 900 has its price changed from 1000 to 100
- **THEN** the discount stays on the form and the row shows `El descuento es mayor que el precio. Edítalo o quítalo.`

### Requirement: Discount persisted on save

Saving the form SHALL persist the discount change after the product update succeeds: a new or changed discount SHALL be saved with replace semantics and the one-week duration, and a discount turned off SHALL be removed. When the discount shown on the form is the same as the one loaded, no discount request SHALL be issued. When the discount failed to load, saving SHALL NOT issue any discount request.

Before any request, when a fixed-amount discount on the form is greater than the price entered, saving SHALL remove the discount from the form, keep the form with unsaved changes, show `Quitamos el descuento porque es mayor que el nuevo precio. Revisa y vuelve a guardar.` and send nothing. The discount SHALL only be removed on the server when the merchant then saves again.

#### Scenario: Save with a new or changed discount

- **WHEN** the merchant saves a form whose discount differs from the one loaded and both requests succeed
- **THEN** the product is updated, the previous discount (if any) is deleted, the new one is created, and the app returns to the Inventory screen signalling that the product was updated

#### Scenario: Save with the discount turned off

- **WHEN** the merchant saves a form where a previously loaded discount was turned off
- **THEN** the product is updated, the discount is deleted, and the app returns to the Inventory screen

#### Scenario: Save with an untouched discount

- **WHEN** the merchant saves a form without having changed the discount
- **THEN** only the product update request is issued

#### Scenario: Price corrected with a stored discount

- **WHEN** a product with a stored 20 % discount has its price changed from 150 to 150.50 and then to 200 (clearing the field on the way) and is saved
- **THEN** only the product update request is issued and no discount is deleted

#### Scenario: Save with a fixed amount over the price

- **WHEN** the merchant saves with a fixed discount of 900 and a price of 100
- **THEN** no request is issued, the discount is removed from the form, the message `Quitamos el descuento porque es mayor que el nuevo precio. Revisa y vuelve a guardar.` is shown and the form keeps unsaved changes

#### Scenario: Product update fails

- **WHEN** the product update request fails
- **THEN** no discount request is issued and the merchant stays on the form with the entered values and the pending discount intact

#### Scenario: Product saved but the discount fails

- **WHEN** the product update succeeds but saving or removing the discount fails
- **THEN** the merchant stays on the form, a message states that the product was saved but the discount could not be updated, and the form remains with unsaved changes so saving can be retried
