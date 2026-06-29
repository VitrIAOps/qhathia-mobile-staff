## ADDED Requirements

### Requirement: Product registration form

The app SHALL provide a Product Registration screen, reached from the inventory registration FAB, with fields for product name, price, product quantity (stock), optional description, category selection, tag selection and image selection. The form SHALL submit through the authenticated HTTP client using the merchant's Bearer token.

#### Scenario: Open the registration form

- **WHEN** the merchant taps the registration FAB on the Inventory screen
- **THEN** the Product Registration screen opens with all fields empty and the submit action disabled

#### Scenario: Form reflects entered values

- **WHEN** the merchant enters a name, price and description and makes category, tag and image selections
- **THEN** the screen retains each entered/selected value and reflects it back in the form

### Requirement: Required field validation

The form SHALL require a non-blank product name, a price greater than zero, a quantity that is not negative, at least one category, at least one tag and at least one image before registration is allowed. Validation SHALL run per field as the merchant edits or selects it, and the submit action SHALL remain disabled until every rule is satisfied.

#### Scenario: Submit disabled while incomplete

- **WHEN** any required field (name, price, quantity, category, tag or image) is missing or invalid
- **THEN** the submit action is disabled and the offending field shows a validation message once the merchant has interacted with it

#### Scenario: Price must be greater than zero

- **WHEN** the merchant enters a price of zero, a negative value, or a non-numeric value
- **THEN** the price field shows a validation error and the form cannot be submitted

#### Scenario: Submit enabled when valid

- **WHEN** the merchant has provided a non-blank name, a price greater than zero, at least one category, at least one tag and at least one image
- **THEN** the submit action becomes enabled

### Requirement: Price input in Peruvian soles

The price field SHALL accept and display amounts in Peruvian soles (PEN) with two decimal places (cents), and the value sent to the API SHALL be the numeric amount in soles.

#### Scenario: Price formatted as soles with cents

- **WHEN** the merchant types a monetary amount into the price field
- **THEN** the field formats it as Peruvian soles with two decimal places, and the submitted value is the corresponding numeric amount

### Requirement: Product quantity

The form SHALL provide a product quantity (stock) field. The quantity SHALL NOT be negative; a negative quantity SHALL block registration. Because the products API exposes no quantity field yet, the value SHALL be captured and validated on the client but SHALL NOT be sent to the backend in this change.

#### Scenario: Negative quantity blocks registration

- **WHEN** the merchant enters a quantity less than zero
- **THEN** the quantity field shows a validation error and the form cannot be submitted

#### Scenario: Non-negative quantity is accepted

- **WHEN** the merchant enters a quantity of zero or greater
- **THEN** the quantity field is accepted and contributes to enabling submission

#### Scenario: Quantity is not transmitted to the backend

- **WHEN** the product is registered
- **THEN** the create-product request does not include the quantity field, and registration succeeds without it

### Requirement: Category selection

The form SHALL load the store's categories from `GET /api/v1/categories` and let the merchant select at least one category for the product.

#### Scenario: Select a category

- **WHEN** the categories load and the merchant selects one
- **THEN** the selected category is marked active and contributes its id to the product registration request

#### Scenario: Categories fail to load

- **WHEN** the categories request fails
- **THEN** the screen surfaces a retry affordance for categories and does not crash

### Requirement: Tag entry

The form SHALL provide a free-text tag field (per the Figma mockup) where the merchant types one or more tags separated by commas. At least one non-blank tag SHALL be required, and the entered tags SHALL be parsed into individual values for the product registration request.

#### Scenario: Enter at least one tag

- **WHEN** the merchant types one or more comma-separated tags
- **THEN** the tags are parsed into trimmed, non-blank values and included in the product registration request

#### Scenario: No tag entered

- **WHEN** the tag field is empty or contains only separators/whitespace
- **THEN** the tag requirement is shown as unmet and submission is blocked

### Requirement: Image selection restricted to JPG and PNG

The form SHALL let the merchant add one or more images either by capturing a new photo with the device camera or by choosing existing photos from the gallery, and SHALL accept only JPG and PNG images. At least one image is required.

#### Scenario: Choose the photo source

- **WHEN** the merchant taps the add-photo control
- **THEN** the app offers capturing a photo with the camera or choosing one from the gallery

#### Scenario: Pick a valid image

- **WHEN** the merchant captures or chooses a JPG or PNG image
- **THEN** the image is added to the form as a preview thumbnail and satisfies the image requirement

#### Scenario: Reject an unsupported image format

- **WHEN** the merchant picks an image that is not JPG or PNG
- **THEN** the image is rejected with a message and is not added to the form

### Requirement: Image compression before upload

Each selected image SHALL be compressed on-device to at most 500 KB while preserving good quality before it is uploaded.

#### Scenario: Large image is compressed

- **WHEN** the merchant selects an image larger than 500 KB
- **THEN** the app re-encodes it to 500 KB or less before uploading it, keeping acceptable visual quality

#### Scenario: Image that cannot be compressed

- **WHEN** a selected image cannot be reduced to 500 KB or less
- **THEN** registration fails with an explanatory message and the form remains available

### Requirement: Image upload to Cloudflare R2

Selected images SHALL be uploaded to Cloudflare R2 object storage using credentials supplied through the build configuration (sourced from `local.properties`, never committed), and the resulting public URL SHALL be used to register the product media.

#### Scenario: Image uploaded and URL obtained

- **WHEN** an image is uploaded to R2 successfully
- **THEN** the app obtains the image's public URL for use in the media registration request

#### Scenario: Upload fails

- **WHEN** an image upload to R2 fails
- **THEN** registration does not complete and the merchant is shown an error, staying on the form

### Requirement: Product registration orchestration

On submit, the app SHALL create the product via `POST /api/v1/products` and then, for each image, upload it to R2 and attach it via `POST /api/v1/products/{id}/media`, marking the first image as primary. If media upload or attachment fails after the product was created, the app SHALL attempt to roll back the created product so that no product without its required image is left behind.

#### Scenario: Successful registration

- **WHEN** the product is created and all images are uploaded and attached
- **THEN** the registration is reported as successful

#### Scenario: Media step fails after product creation

- **WHEN** the product is created but a subsequent image upload or attachment fails
- **THEN** the app attempts to deactivate the created product and reports the registration as failed

### Requirement: Registration feedback and navigation

While registration is in progress the screen SHALL show a circular loading indicator. On success the app SHALL navigate back to the inventory list and show a success snackbar using the design system's success (green) tones. On a generic failure the app SHALL keep the merchant on the registration screen and show an error snackbar describing the reason.

#### Scenario: Loader during registration

- **WHEN** registration is in progress
- **THEN** a circular loading indicator is shown and form inputs are disabled

#### Scenario: Success returns to inventory

- **WHEN** registration succeeds
- **THEN** the app returns to the inventory list and shows a success snackbar in the design system's green tones

#### Scenario: Generic failure keeps the form

- **WHEN** registration fails for a reason other than an exceeded plan
- **THEN** the merchant stays on the registration screen and an error snackbar describes the reason

### Requirement: Plan-limit handling

When product creation is rejected because the store's plan product limit has been reached (HTTP 403 on create), the app SHALL NOT keep the merchant on the registration form; instead it SHALL return to the inventory list and surface a distinct plan-exceeded message.

#### Scenario: Plan limit reached

- **WHEN** product creation is rejected because the plan's product limit is exceeded
- **THEN** the app returns to the inventory list and shows a message indicating the plan limit was exceeded

### Requirement: Unsaved-changes guard

When the merchant attempts to leave the registration screen (system back or cancel) with unsaved changes, the app SHALL show a confirmation dialog warning that the changes will be discarded, and SHALL only leave the screen if the merchant confirms.

#### Scenario: Confirm discard

- **WHEN** the merchant backs out or cancels with a dirty form and confirms the dialog
- **THEN** the changes are discarded and the registration screen is left

#### Scenario: Cancel discard

- **WHEN** the merchant backs out or cancels with a dirty form and dismisses the dialog
- **THEN** the merchant stays on the registration screen with the entered data intact

#### Scenario: Leaving a pristine form

- **WHEN** the merchant backs out with no unsaved changes
- **THEN** the screen is left without showing the confirmation dialog
