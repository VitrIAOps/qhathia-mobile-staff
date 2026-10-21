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

The Product Detail screen SHALL let the merchant edit the product's name, description, category, price, stock and tags, validating each field as it is edited using the same rules as product registration. Tags SHALL be optional: clearing the tag field SHALL save the product with an explicit empty tag list. Photos SHALL be editable as described in "Photo editing on product detail".

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

A form with unsaved changes — photo additions and removals included — SHALL intercept cancel and system back with a discard-confirmation dialog; a pristine form SHALL navigate back immediately.

#### Scenario: Leaving a dirty form

- **WHEN** the merchant presses back or cancel after editing any field
- **THEN** a dialog asks whether to discard the changes, and only confirming leaves the screen

#### Scenario: Leaving a pristine form

- **WHEN** the merchant presses back or cancel without editing anything
- **THEN** the app returns to the Inventory screen without a dialog

#### Scenario: Leaving after changing photos

- **WHEN** the merchant adds or removes a photo and then presses back or cancel
- **THEN** the discard dialog is shown, and discarding leaves the product's photos as they were

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

### Requirement: Preview action on the product detail form

The Product Detail form SHALL show a `Vista previa` button directly below the Tags field, above the cancel and save actions. The button SHALL be enabled only when the form holds a valid name and a valid price and the product has at least one registered photo, and SHALL be disabled while a save is in progress. While it is disabled for missing data, a hint below it SHALL state that a name, a price and at least one photo are needed. Tapping the enabled button SHALL open the product preview built from the form's current values — including edits that have not been saved — and the product's registered photos, without saving the product.

#### Scenario: Button shown below Tags

- **WHEN** the product has loaded and the detail form is displayed
- **THEN** a `Vista previa` button is shown below the Tags field

#### Scenario: Enabled for a loaded product with photos

- **WHEN** the loaded product has a valid name, a valid price and at least one registered photo
- **THEN** the `Vista previa` button is enabled

#### Scenario: Product with no photos

- **WHEN** the loaded product has no registered photos
- **THEN** the `Vista previa` button is disabled and the hint about the required name, price and photo is shown

#### Scenario: Disabled when the name or price becomes invalid

- **WHEN** the merchant clears the name or enters an invalid price
- **THEN** the `Vista previa` button becomes disabled

#### Scenario: Preview shows unsaved edits

- **WHEN** the merchant changes the price without saving and taps `Vista previa`
- **THEN** the product preview opens showing the new price, and no update request is issued

#### Scenario: Preview does not affect the save action

- **WHEN** the merchant opens and closes the preview on a pristine form
- **THEN** the save action stays disabled and leaving the screen shows no discard dialog

#### Scenario: Disabled while saving

- **WHEN** a save is in progress
- **THEN** the `Vista previa` button is disabled

### Requirement: Detalle de producto header bell opens Solicitudes

The bell in the Detalle de producto header SHALL open the Solicitudes tab and SHALL be announced to accessibility services as "Ver solicitudes". Because it leaves the form, it SHALL go through the same rule as back and cancel ("Discard guard"): with no unsaved changes it SHALL leave immediately; with unsaved changes it SHALL show the discard-confirmation dialog and SHALL only leave if the merchant confirms. Leaving this way SHALL close the Detalle de producto screen, so that returning to Inventario shows the list and not the abandoned form. While a save is in progress the bell SHALL be disabled.

#### Scenario: Bell on a pristine form
- **WHEN** the merchant taps the bell without having changed anything
- **THEN** the Detalle de producto screen closes and the Solicitudes tab is shown

#### Scenario: Bell on a dirty form, confirmed
- **WHEN** the merchant has unsaved changes, taps the bell and confirms the discard dialog
- **THEN** the changes are discarded, the Detalle de producto screen closes and the Solicitudes tab is shown

#### Scenario: Bell on a dirty form, dismissed
- **WHEN** the merchant has unsaved changes, taps the bell and dismisses the discard dialog
- **THEN** the merchant stays on Detalle de producto with the entered data intact

#### Scenario: Back to Inventario afterwards
- **WHEN** the merchant left Detalle de producto through the bell and then taps the Inventario tab
- **THEN** the Inventario list is shown, not the Detalle de producto form

#### Scenario: Bell while saving
- **WHEN** a save is in progress
- **THEN** the bell is disabled

### Requirement: Delete the product from the edit screen

The product edit screen SHALL show an `Eliminar producto` button below the `Cancelar` and `Guardar` actions, full width and in the error colour. It SHALL be disabled while the product is being saved or deleted, and unavailable until the product has loaded.

Tapping it SHALL open the same confirmation as the Inventario row menu: "¿Eliminar producto?", naming the product, stating that it cannot be undone, with `Cancelar` and a destructive `Eliminar`. Nothing SHALL be requested until the merchant confirms.

On confirm the app SHALL call `DELETE /api/v1/products/{id}`. While the request is in flight, the dialog SHALL stay open with both buttons disabled and progress on `Eliminar`. Back, the header bell and dismissing the dialog SHALL be ignored, and saving SHALL be disabled.

- **On success or `404`:** the app SHALL return to Inventario. Unsaved edits are dropped without the discard dialog. Inventario SHALL reload and show "Producto eliminado", or "Este producto ya había sido eliminado" for a `404`.
- **On any other failure:** the merchant SHALL stay on the edit screen with the edits intact, the dialog SHALL close, and an error SHALL read "No se pudo eliminar el producto. Inténtalo de nuevo".

#### Scenario: Button placement
- **WHEN** the merchant opens a product's edit screen and it loads
- **THEN** an `Eliminar producto` button is shown below `Cancelar` and `Guardar`

#### Scenario: Asking for confirmation
- **WHEN** the merchant taps `Eliminar producto`
- **THEN** the confirmation names the product and no request is made yet

#### Scenario: Deleting
- **WHEN** the merchant confirms and the server answers `204`
- **THEN** the app returns to Inventario, the product is no longer listed and "Producto eliminado" is shown

#### Scenario: Deleting with unsaved edits
- **WHEN** the merchant has edited the name, then deletes and confirms
- **THEN** the product is deleted and the app returns to Inventario without asking to discard the edits

#### Scenario: Already deleted elsewhere
- **WHEN** the merchant confirms and the server answers `404`
- **THEN** the app returns to Inventario and "Este producto ya había sido eliminado" is shown

#### Scenario: Failure
- **WHEN** the merchant confirms and the request fails for any other reason
- **THEN** the edit screen stays with the edits intact and "No se pudo eliminar el producto. Inténtalo de nuevo" is shown

#### Scenario: Request in flight
- **WHEN** the delete request has not answered yet
- **THEN** back and the header bell do nothing, the dialog cannot be dismissed and `Guardar` is disabled

### Requirement: Photo editing on product detail

The photos section of the edit screen SHALL show the product's photos, primary first, each with a remove control, followed by an `Agregar` tile. `Agregar` SHALL offer taking a photo with the camera or choosing from the gallery, as in registration. Only JPG and PNG SHALL be accepted.

Photo changes SHALL be part of the form: they are shown immediately, nothing is sent until `Guardar`, and they mark the form as changed.

A product SHALL hold at most 10 photos:
- At 10, `Agregar` SHALL be disabled with the hint "Máximo 10 fotos".
- Picking more photos than fit SHALL add only those that fit and say the rest were not added.

A product SHALL keep at least one photo: when one remains, its remove control SHALL NOT be offered, and a hint SHALL read "El producto necesita al menos una foto".

#### Scenario: Adding photos
- **WHEN** the merchant picks two photos from the gallery on a product with three
- **THEN** the strip shows five photos, the two new ones last, `Guardar` is enabled, and no request is sent yet

#### Scenario: Removing a photo
- **WHEN** the merchant removes one of three photos
- **THEN** the strip shows two photos, `Guardar` is enabled, and no request is sent yet

#### Scenario: The last photo
- **WHEN** the product shows a single photo
- **THEN** that photo has no remove control and the hint "El producto necesita al menos una foto" is shown

#### Scenario: The photo limit
- **WHEN** the product shows 10 photos
- **THEN** `Agregar` is disabled and the hint "Máximo 10 fotos" is shown

#### Scenario: Picking more than fit
- **WHEN** the product shows 8 photos and the merchant picks 4
- **THEN** only the first 2 are added and a message says the others were not added

#### Scenario: Unsupported format
- **WHEN** the merchant picks an image that is not JPG or PNG
- **THEN** it is not added and a message says only JPG and PNG are accepted

### Requirement: Photo changes persisted on save

Saving SHALL persist photo changes after the product update succeeds and before the discount change:
1. Each removed photo is deleted with `DELETE /api/v1/products/{id}/media/{mediaId}`.
2. All added photos are compressed and uploaded in one `POST /api/v1/products/{id}/images` request.

When photos did not change, no photo request SHALL be sent.

If the product update fails, no photo request SHALL be sent.

If a photo request fails:
- The merchant SHALL stay on the form with the message "Se guardó el producto, pero no se pudieron actualizar las fotos".
- No discount request SHALL be sent.
- The photos SHALL be re-read from the server: saved photos are shown as the server has them, with the photos the merchant removed (and that still exist) hidden again. Photos that were not uploaded SHALL remain pending.
- The form SHALL remain with unsaved changes, so saving again retries only what is left.

#### Scenario: Saving added and removed photos
- **WHEN** the merchant removed one photo, added two, and saves, and every request succeeds
- **THEN** the product is updated, the photo is deleted, the two photos are uploaded in one request, and the app returns to Inventario signalling the product was updated

#### Scenario: Saving without photo changes
- **WHEN** the merchant edits only the name and saves
- **THEN** no photo request is sent

#### Scenario: Upload fails after the product saved
- **WHEN** the product update and the deletions succeed but the upload fails
- **THEN** the merchant stays on the form with the photos message, the deleted photo is gone from the strip, the new photos are still pending, and saving again uploads only them

#### Scenario: Product update fails
- **WHEN** the product update request fails
- **THEN** no photo request is sent and the form keeps the pending photo changes
