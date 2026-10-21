## MODIFIED Requirements

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

## ADDED Requirements

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
