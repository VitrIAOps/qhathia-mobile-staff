## MODIFIED Requirements

### Requirement: Product registration orchestration

On submit, the app SHALL register the product in a single atomic request to `POST /api/v1/products/with-images`, sending the product data (including `stock`, category, tags and price) as the `product` part and each compressed image as an `images` part, with the first image treated as primary. The app SHALL NOT create the product and attach media in separate calls, and SHALL NOT perform a client-side rollback, because the backend creates the product and its media atomically and rolls back entirely if any image upload fails.

Each registration form SHALL have one idempotency key, a UUID created when the form opens and kept across process death. The key SHALL be sent as the `Idempotency-Key` header on every submit of that form, retries included. A `200` replay (`Idempotent-Replayed: true`, the product already created by an earlier attempt) SHALL be handled exactly like a `201`.

#### Scenario: Successful registration

- **WHEN** the merchant submits a valid form
- **THEN** the app sends one multipart request with the product data and all images, and the registration is reported as successful when the backend returns the created product

#### Scenario: Backend rejects the registration atomically

- **WHEN** the backend fails to create the product or upload any image
- **THEN** no partial product is created, the app reports the registration as failed, and no client-side rollback call is made

#### Scenario: Retry after a lost answer

- **WHEN** the first submit reached the backend but the answer was lost (no connection), and the merchant submits again
- **THEN** the second request carries the same `Idempotency-Key`, the backend answers `200` with the same product, and only one product exists

### Requirement: Registration feedback and navigation

While registration is in progress the screen SHALL show a circular loading indicator. On success the app SHALL navigate back to the inventory list and show a success snackbar using the design system's success (green) tones. Whenever the product was created — whether or not an applied discount could be created for it — the app SHALL hand the id of the newly registered product to the inventory list on the way back, so the inventory can locate it. On a generic failure the app SHALL keep the merchant on the registration screen and show an error snackbar describing the reason.

While registration is in progress, system back, cancel, the header chevron and the header bell SHALL do nothing, and the loading scrim SHALL consume touches and be announced as "Guardando producto…". Leaving would cancel the request while the backend may still create the product.

#### Scenario: Loader during registration

- **WHEN** registration is in progress
- **THEN** a circular loading indicator is shown and form inputs are disabled

#### Scenario: Back while registering

- **WHEN** the merchant presses system back or the header chevron right after tapping `Agregar`
- **THEN** the screen stays, the loader keeps running, and the result is shown when the request ends

#### Scenario: Success returns to inventory

- **WHEN** registration succeeds
- **THEN** the app returns to the inventory list and shows a success snackbar in the design system's green tones

#### Scenario: Success identifies the new product

- **WHEN** registration succeeds and the app returns to the inventory list
- **THEN** the id returned by the backend for the created product is passed to the Inventory screen together with the success result

#### Scenario: Failed discount still identifies the new product

- **WHEN** the product registration succeeds but creating its discount fails, and the app returns to the inventory list
- **THEN** the id returned by the backend for the created product is passed to the Inventory screen together with the discount-failed result

#### Scenario: Generic failure keeps the form

- **WHEN** registration fails for a reason other than an exceeded plan
- **THEN** the merchant stays on the registration screen and an error snackbar describes the reason

### Requirement: Image selection restricted to JPG and PNG

The form SHALL let the merchant add one or more images either by capturing a new photo with the device camera or by choosing existing photos from the gallery, and SHALL accept only JPG and PNG images. At least one image is required, and at most 10 (the backend's per-product limit). The photos section SHALL say "Máximo 10 fotos".

When a pick would go over 10, only the photos that fit SHALL be added, and a message SHALL read "Solo se pueden agregar 10 fotos; algunas no se agregaron". With 10 photos the add control SHALL be disabled.

#### Scenario: Choose the photo source

- **WHEN** the merchant taps the add-photo control
- **THEN** the app offers capturing a photo with the camera or choosing one from the gallery

#### Scenario: Pick a valid image

- **WHEN** the merchant captures or chooses a JPG or PNG image
- **THEN** the image is added to the form as a preview thumbnail and satisfies the image requirement

#### Scenario: Reject an unsupported image format

- **WHEN** the merchant picks an image that is not JPG or PNG
- **THEN** the image is rejected with a message and is not added to the form

#### Scenario: Picking more than ten

- **WHEN** the merchant picks 11 photos on an empty form
- **THEN** 10 are added, "Solo se pueden agregar 10 fotos; algunas no se agregaron" is shown, and the add control is disabled
