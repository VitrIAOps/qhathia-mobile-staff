## MODIFIED Requirements

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
