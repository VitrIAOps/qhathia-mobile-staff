# product-detail Specification

## Purpose

Defines how a store owner opens one of the store's products from the inventory list and edits it in the Qhathia staff app: loading the full product from the Products API, the editable form (name, description with AI optimization, category, price, stock and tags), saving through a full-replace update, and the read-only view of the product's registered photos.
## Requirements
### Requirement: Product detail load

The Product Detail screen SHALL retrieve the product from `GET /api/v1/products/{id}` using the id passed on the navigation route, and seed its form from the response. Only the product id SHALL cross the navigation boundary — never the product itself. The request SHALL be authenticated with the merchant's Bearer token via the existing authenticated HTTP client.

When the product comes back with `active: false` (deleted, which staff can still read), the screen SHALL NOT show the form. It SHALL show "Este producto fue eliminado" with a `Volver` button that returns to Inventario.

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

#### Scenario: Opening a deleted product

- **WHEN** the product loads with `active: false`
- **THEN** the screen shows "Este producto fue eliminado" and a `Volver` button instead of the form, and `Volver` returns to Inventario

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

When the product update, a photo request or a discount request answers `404` (the product was deleted elsewhere), the app SHALL return to Inventario, which shows "Este producto ya había sido eliminado". No further request SHALL be sent.

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
- **THEN** the second save carries the product's version after the first update, not the version loaded at first

#### Scenario: Product deleted elsewhere

- **WHEN** the merchant changes the name and saves, and the update answers `404`
- **THEN** the app returns to Inventario showing "Este producto ya había sido eliminado"

#### Scenario: Product deleted between the update and the photos

- **WHEN** the update succeeds and the photo upload answers `404`
- **THEN** no discount request is sent and the app returns to Inventario showing "Este producto ya había sido eliminado"

#### Scenario: Save fails

- **WHEN** the update request fails for any other reason
- **THEN** the merchant stays on the form with the entered values intact and a message reflecting the cause (invalid input, no connectivity, or a generic failure) is shown

### Requirement: Discard guard

A form with unsaved changes — photo additions and removals included — SHALL intercept cancel and system back with a discard-confirmation dialog; a pristine form SHALL navigate back immediately.

While a save is in flight, system back, cancel, the header chevron and the header bell SHALL do nothing, and the loading scrim SHALL consume touches and be announced as "Guardando producto…". Leaving would cancel the save midway.

When the product was saved but its photos or its discount were not (a partial save), the discard dialog SHALL say so: "El descuento no se guardó. Si sales, el producto queda sin ese cambio." (or "Las fotos no se guardaron…").

#### Scenario: Leaving a dirty form

- **WHEN** the merchant presses back or cancel after editing any field
- **THEN** a dialog asks whether to discard the changes, and only confirming leaves the screen

#### Scenario: Leaving a pristine form

- **WHEN** the merchant presses back or cancel without editing anything
- **THEN** the app returns to the Inventory screen without a dialog

#### Scenario: Leaving after changing photos

- **WHEN** the merchant adds or removes a photo and then presses back or cancel
- **THEN** the discard dialog is shown, and discarding leaves the product's photos as they were

#### Scenario: Back while saving

- **WHEN** the merchant presses system back or the header chevron while the save is in flight
- **THEN** nothing happens, the save continues, and its result is shown

#### Scenario: Leaving after a partial save

- **WHEN** the discount step failed after the product was saved and the merchant presses back
- **THEN** the discard dialog says "El descuento no se guardó. Si sales, el producto queda sin ese cambio."

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

When the product update succeeded but the discount step failed:
- The merchant SHALL stay on the form, and a persistent banner at the top of the form SHALL read "Se guardaron los datos del producto, pero no el descuento. Toca Guardar para reintentar." A long message SHALL say the same.
- The stored discount SHALL be read again. If that read also fails, the form SHALL treat the product as having no discount, so the next save creates the form's discount again.
- Saving again without changing the product fields SHALL send only the discount requests, not the product update.

The banner SHALL disappear once a save completes.

#### Scenario: Save with a new or changed discount

- **WHEN** the merchant saves a form whose discount differs from the one loaded and both requests succeed
- **THEN** the product is updated, the new discount is created, the previous one (if any) is deleted, and the app returns to the Inventory screen signalling that the product was updated

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
- **THEN** the merchant stays on the form, the banner "Se guardaron los datos del producto, pero no el descuento. Toca Guardar para reintentar." is shown, and the form remains with unsaved changes so saving can be retried

#### Scenario: Retrying only the discount

- **WHEN** the discount failed after the product was saved, and the merchant taps Guardar again without changing the product fields
- **THEN** no product update is sent, only the discount requests, and on success the app returns to Inventario

#### Scenario: The stored discount cannot be read after the failure

- **WHEN** the discount step failed and reading the stored discount also fails
- **THEN** the form treats the product as having no discount, and the next save creates the form's discount

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

Tapping it SHALL open the same confirmation as the Inventario row menu: "¿Eliminar producto?", naming the product **as it was loaded** (not the edited name), stating that it cannot be undone, with `Cancelar` and a destructive `Eliminar`. When the form has unsaved changes, the text SHALL also say "También se perderán los cambios que no guardaste." Nothing SHALL be requested until the merchant confirms.

On confirm the app SHALL call `DELETE /api/v1/products/{id}`. While the request is in flight, the dialog SHALL stay open with both buttons disabled and progress on `Eliminar`. Back, the header bell and dismissing the dialog SHALL be ignored, and saving SHALL be disabled.

- **On success or `404`:** the app SHALL return to Inventario. Unsaved edits are dropped without the discard dialog. Inventario SHALL reload and show "Producto eliminado", or "Este producto ya había sido eliminado" for a `404`.
- **On `409 PRODUCT_IN_PENDING_ORDERS`:** the dialog SHALL stay open and show the server's `detail` (for example "Este producto está en 1 pedido pendiente (#400776). Confírmalo o recházalo antes, o elimínalo igual."), with `Cancelar` and a destructive `Eliminar igual`. `Eliminar igual` SHALL call `DELETE /api/v1/products/{id}?force=true`, whose result is handled as above.
- **On any other failure:** the merchant SHALL stay on the edit screen with the edits intact, the dialog SHALL close, and an error SHALL read "No se pudo eliminar el producto. Inténtalo de nuevo".

#### Scenario: Button placement
- **WHEN** the merchant opens a product's edit screen and it loads
- **THEN** an `Eliminar producto` button is shown below `Cancelar` and `Guardar`

#### Scenario: Asking for confirmation
- **WHEN** the merchant taps `Eliminar producto`
- **THEN** the confirmation names the product and no request is made yet

#### Scenario: Asking with unsaved edits
- **WHEN** the merchant renamed "QA Sin foto det" to "QA Sin foto det x" without saving and taps `Eliminar producto`
- **THEN** the confirmation names "QA Sin foto det" and says the unsaved changes will be lost

#### Scenario: Deleting
- **WHEN** the merchant confirms and the server answers `204`
- **THEN** the app returns to Inventario, the product is no longer listed and "Producto eliminado" is shown

#### Scenario: Deleting with unsaved edits
- **WHEN** the merchant has edited the name, then deletes and confirms
- **THEN** the product is deleted and the app returns to Inventario without asking to discard the edits

#### Scenario: Already deleted elsewhere
- **WHEN** the merchant confirms and the server answers `404`
- **THEN** the app returns to Inventario and "Este producto ya había sido eliminado" is shown

#### Scenario: Product in pending orders
- **WHEN** the merchant confirms and the server answers `409 PRODUCT_IN_PENDING_ORDERS`
- **THEN** the dialog stays open showing the server's message, with `Cancelar` and `Eliminar igual`

#### Scenario: Deleting anyway
- **WHEN** the merchant taps `Eliminar igual`
- **THEN** the app calls `DELETE /api/v1/products/{id}?force=true` and, on `204`, returns to Inventario showing "Producto eliminado"

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
- The merchant SHALL stay on the form with a persistent banner at the top: "Se guardaron los datos del producto, pero no las fotos. Toca Guardar para reintentar." A long message SHALL say the same.
- No discount request SHALL be sent.
- The photos SHALL be re-read from the server: saved photos are shown as the server has them, with the photos the merchant removed (and that still exist) hidden again. Photos that were not uploaded SHALL remain pending.
- The form SHALL remain with unsaved changes. Saving again without changing the product fields SHALL send only what is left (photos, then the discount), not the product update.

#### Scenario: Saving added and removed photos
- **WHEN** the merchant removed one photo, added two, and saves, and every request succeeds
- **THEN** the product is updated, the photo is deleted, the two photos are uploaded in one request, and the app returns to Inventario signalling the product was updated

#### Scenario: Saving without photo changes
- **WHEN** the merchant edits only the name and saves
- **THEN** no photo request is sent

#### Scenario: Upload fails after the product saved
- **WHEN** the product update and the deletions succeed but the upload fails
- **THEN** the merchant stays on the form with the photos banner, the deleted photo is gone from the strip, the new photos are still pending, and saving again uploads only them without a new product update

#### Scenario: Product update fails
- **WHEN** the product update request fails
- **THEN** no photo request is sent and the form keeps the pending photo changes

### Requirement: Discount end countdown on product detail

The Product Detail screen SHALL show, below the discount summary line, how long remains until the discount in force ends, using `discountEndsAt` from `GET /api/v1/products/{id}`. The countdown SHALL be shown only while the form's price and discount are the ones loaded with the product and the server reports a discount in force (`discountedPrice` is not `null`) with a `discountEndsAt`. It SHALL read:

- `Termina en {d} días y {h} horas` when one day or more remains and the leftover hours are not zero, using the singular `día` / `hora` for one;
- `Termina en {d} días` (or `1 día`) when one day or more remains and the leftover hours are zero;
- `Termina en {h} horas` (or `1 hora`) when less than one day but at least one hour remains;
- `Termina en menos de 1 hora` when less than one hour remains.

Days and hours SHALL be whole units rounded down. The countdown SHALL refresh at least once a minute while the screen is open, and SHALL NOT be shown once the end has passed.

#### Scenario: Several days remaining

- **WHEN** the product loads with a discount in force that ends in 6 days and 4 hours and 20 minutes
- **THEN** the line below the summary reads `Termina en 6 días y 4 horas`

#### Scenario: Whole days remaining

- **WHEN** the discount ends in exactly 1 day and 30 minutes
- **THEN** the line reads `Termina en 1 día`

#### Scenario: Last day

- **WHEN** the discount ends in 5 hours and 59 minutes
- **THEN** the line reads `Termina en 5 horas`

#### Scenario: Last hour

- **WHEN** the discount ends in 40 minutes
- **THEN** the line reads `Termina en menos de 1 hora`

#### Scenario: Discount already ended

- **WHEN** the end of the discount passes while the screen is open
- **THEN** the countdown line is no longer shown

#### Scenario: Merchant edits the discount or the price

- **WHEN** the merchant applies a different discount or changes the price
- **THEN** the countdown line is hidden

#### Scenario: No discount in force on the server

- **WHEN** the product loads with a discount on the form but the server's `discountedPrice` is `null`
- **THEN** no countdown line is shown

#### Scenario: Product without a discount

- **WHEN** the product loads without a discount
- **THEN** no countdown line is shown

