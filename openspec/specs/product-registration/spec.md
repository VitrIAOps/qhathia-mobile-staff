# product-registration Specification

## Purpose

Defines how a store owner registers a new product in the Qhathia staff app: the Product Registration screen reached from the inventory FAB, its required fields (name, price in Peruvian soles, quantity, description, category, tags and images), per-field validation, JPG/PNG image selection with on-device compression, submitting the product and its images in a single atomic `POST /api/v1/products/with-images` request (the backend uploads the images to storage), sending the product quantity as `stock`, registration feedback and navigation, plan-limit handling, the unsaved-changes guard, and AI-assisted drafting/optimization of the description, category and tags from a product image.
## Requirements
### Requirement: Product registration form

The app SHALL provide a Product Registration screen, reached from the inventory registration FAB, with fields for product name, price, product quantity (stock), optional description, category selection, tag selection and image selection. The form SHALL submit through the authenticated HTTP client using the merchant's Bearer token.

#### Scenario: Open the registration form

- **WHEN** the merchant taps the registration FAB on the Inventory screen
- **THEN** the Product Registration screen opens with all fields empty and the submit action disabled

#### Scenario: Form reflects entered values

- **WHEN** the merchant enters a name, price and description and makes category, tag and image selections
- **THEN** the screen retains each entered/selected value and reflects it back in the form

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

### Requirement: Price input in Peruvian soles

The price field SHALL accept and display amounts in Peruvian soles (PEN) with two decimal places (cents), and the value sent to the API SHALL be the numeric amount in soles.

#### Scenario: Price formatted as soles with cents

- **WHEN** the merchant types a monetary amount into the price field
- **THEN** the field formats it as Peruvian soles with two decimal places, and the submitted value is the corresponding numeric amount

### Requirement: Product quantity

The form SHALL provide a product quantity (stock) field. The quantity SHALL NOT be negative; a negative quantity SHALL block registration. The validated quantity SHALL be sent to the backend as the product's `stock` when the product is registered. The registration request SHALL NOT set `availability` explicitly, so that the backend derives it from the submitted stock per the API's `stock ↔ availability` sync rule.

#### Scenario: Negative quantity blocks registration

- **WHEN** the merchant enters a quantity less than zero
- **THEN** the quantity field shows a validation error and the form cannot be submitted

#### Scenario: Non-negative quantity is accepted

- **WHEN** the merchant enters a quantity of zero or greater
- **THEN** the quantity field is accepted and contributes to enabling submission

#### Scenario: Quantity is transmitted as stock

- **WHEN** the product is registered
- **THEN** the registration request includes the entered quantity as the product's `stock`, and does not send an explicit `availability` value

### Requirement: Category selection

The form SHALL load the store's categories from `GET /api/v1/categories` and let the merchant select at least one category for the product.

#### Scenario: Select a category

- **WHEN** the categories load and the merchant selects one
- **THEN** the selected category is marked active and contributes its id to the product registration request

#### Scenario: Categories fail to load

- **WHEN** the categories request fails
- **THEN** the screen surfaces a retry affordance for categories and does not crash

### Requirement: Tag entry

The form SHALL provide a free-text tag field (per the Figma mockup) where the merchant types zero or more tags separated by commas. Tags SHALL be optional. The entered tags SHALL be parsed into individual trimmed, non-blank values for the product registration request, and when none are entered the request SHALL carry an explicit empty tag list.

#### Scenario: Enter one or more tags

- **WHEN** the merchant types one or more comma-separated tags
- **THEN** the tags are parsed into trimmed, non-blank values and included in the product registration request

#### Scenario: No tag entered

- **WHEN** the tag field is empty or contains only separators/whitespace
- **THEN** submission is not blocked and the product registration request carries an empty tag list

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

Each selected image SHALL be compressed on-device to at most 500 KB while preserving good quality before it is submitted to the create-product endpoint, keeping every image and the overall request within the endpoint's per-image and total size limits.

#### Scenario: Large image is compressed

- **WHEN** the merchant selects an image larger than 500 KB
- **THEN** the app re-encodes it to 500 KB or less before submitting it, keeping acceptable visual quality

#### Scenario: Image that cannot be compressed

- **WHEN** a selected image cannot be reduced to 500 KB or less
- **THEN** registration fails with an explanatory message and the form remains available

### Requirement: Product registration orchestration

On submit, the app SHALL register the product in a single atomic request to `POST /api/v1/products/with-images`, sending the product data (including `stock`, category, tags and price) as the `product` part and each compressed image as an `images` part, with the first image treated as primary. The app SHALL NOT create the product and attach media in separate calls, and SHALL NOT perform a client-side rollback, because the backend creates the product and its media atomically and rolls back entirely if any image upload fails.

#### Scenario: Successful registration

- **WHEN** the merchant submits a valid form
- **THEN** the app sends one multipart request with the product data and all images, and the registration is reported as successful when the backend returns the created product

#### Scenario: Backend rejects the registration atomically

- **WHEN** the backend fails to create the product or upload any image
- **THEN** no partial product is created, the app reports the registration as failed, and no client-side rollback call is made

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

When product registration is rejected because the store's plan product limit has been reached (HTTP 402 from the create-with-images endpoint), the app SHALL NOT keep the merchant on the registration form; instead it SHALL return to the inventory list and surface a distinct plan-exceeded message.

#### Scenario: Plan limit reached

- **WHEN** registration is rejected because the plan's product limit is exceeded
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

### Requirement: AI drafting availability

The Product Registration screen SHALL expose an AI assist action whose availability follows the images: the action SHALL be disabled while no image is loaded and SHALL become enabled once at least one image is present. The action SHALL be disabled while an AI request is already in flight.

#### Scenario: No image loaded

- **WHEN** the registration form has no images
- **THEN** the AI assist action is disabled

#### Scenario: Image loaded enables the action

- **WHEN** the merchant has loaded at least one image and no AI request is in flight
- **THEN** the AI assist action is enabled

#### Scenario: Action disabled during a request

- **WHEN** an AI generate or optimize request is in flight
- **THEN** the AI assist action is disabled until the request completes

### Requirement: Generate vs. optimize button behavior

The AI assist action SHALL present as "Generate with AI" whenever the description, category and tags are not all filled, and SHALL present as "Optimize with AI" only when description, category and tags are all already provided. Triggering the action SHALL run the drafting flow in the generate state and the description-optimization flow in the optimize state.

#### Scenario: Generate label when fields incomplete

- **WHEN** at least one of description, category or tags is empty
- **THEN** the AI assist action reads "Generate with AI" and triggers the drafting flow

#### Scenario: Optimize label when fields complete

- **WHEN** description, category and tags are all filled
- **THEN** the AI assist action reads "Optimize with AI" and triggers the description-optimization flow

### Requirement: Draft product fields from image

When the drafting flow runs, the app SHALL send the first loaded image to `POST /api/v1/products/draft-from-image` using the authenticated client, keeping the image within the endpoint's 5 MB limit, and SHALL populate the description, category and tags fields from the response. The returned `categoryId` SHALL be applied only when it matches one of the store's loaded categories; when it matches none, the category selection SHALL be left unchanged/unselected. The returned tags SHALL be applied as the form's tags. A fail-open response (empty description, null category, no tags) SHALL leave the fields untouched.

#### Scenario: Draft populates the form

- **WHEN** the drafting flow returns a description, a recognized `categoryId` and tags
- **THEN** the description, the matching category selection and the tags fields are filled from the response

#### Scenario: Unrecognized category is not applied

- **WHEN** the drafting response carries a `categoryId` that is not among the store's loaded categories
- **THEN** no category is selected and the other returned fields are still applied

#### Scenario: Fail-open response changes nothing

- **WHEN** the drafting response is empty (no description, null category, no tags)
- **THEN** the form fields are left as they were and no error is shown

### Requirement: Replace-confirmation warning

Before the drafting flow overwrites existing input, the app SHALL show a warning dialog whenever the description, tags or category already hold data, stating that those fields will be replaced, and SHALL only proceed with drafting if the merchant confirms. When none of those fields hold data, drafting SHALL proceed without a dialog.

#### Scenario: Warn before overwriting entered data

- **WHEN** the merchant triggers generation while description, tags or category already have values
- **THEN** a warning dialog explains the fields will be replaced and drafting runs only after the merchant confirms

#### Scenario: Cancel keeps existing data

- **WHEN** the merchant dismisses the replace-confirmation dialog
- **THEN** drafting does not run and the existing field values are preserved

#### Scenario: No warning when fields are empty

- **WHEN** the merchant triggers generation while description, tags and category are all empty
- **THEN** drafting proceeds immediately without a warning dialog

### Requirement: Optimize existing description

When the optimize flow runs, the app SHALL send the current description to `POST /api/v1/products/regenerate-description` using the authenticated client and SHALL replace the description field with the returned paraphrase, leaving category, tags and all other fields unchanged. A fail-open response (the endpoint echoes the original description) SHALL simply leave the description effectively unchanged.

#### Scenario: Optimization replaces the description

- **WHEN** the optimize flow returns a paraphrased description
- **THEN** only the description field is replaced with the returned text and category and tags are unchanged

#### Scenario: Optimization echo leaves description unchanged

- **WHEN** the optimize flow echoes the original description (fail-open)
- **THEN** the description is unchanged and no error is shown

### Requirement: AI request feedback

While an AI generate or optimize request is in flight the screen SHALL show a loading indication on the AI assist action and prevent re-triggering it. If the AI network call itself fails (no successful response received), the app SHALL surface a transient error message and leave the form fields unchanged.

#### Scenario: Loading during the request

- **WHEN** an AI generate or optimize request is in flight
- **THEN** the AI assist action shows a loading indication and cannot be re-triggered

#### Scenario: Network failure surfaces an error

- **WHEN** the AI request fails to receive a successful response
- **THEN** a transient error message is shown and the form fields are left unchanged

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

### Requirement: Preview action on the registration form

The Product Registration form SHALL show a `Vista previa` button directly below the Tags field, above the cancel and submit actions. The button SHALL be enabled only when the form holds a valid name, a valid price and at least one image, and SHALL be disabled while registration is in progress. While it is disabled for missing data, a hint below it SHALL state that a name, a price and at least one photo are needed. Tapping the enabled button SHALL open the product preview built from the form's current values, without registering the product.

#### Scenario: Button shown below Tags

- **WHEN** the registration form is displayed
- **THEN** a `Vista previa` button is shown below the Tags field

#### Scenario: Disabled on an empty form

- **WHEN** the registration form has just opened
- **THEN** the `Vista previa` button is disabled and the hint about the required name, price and photo is shown

#### Scenario: Enabled with the minimum data

- **WHEN** the merchant has entered a valid name and price and added at least one image, with no category, description, stock or tags
- **THEN** the `Vista previa` button is enabled and the hint is hidden

#### Scenario: Disabled again when a photo is removed

- **WHEN** the merchant removes the only image
- **THEN** the `Vista previa` button becomes disabled

#### Scenario: Opening the preview

- **WHEN** the merchant taps the enabled `Vista previa` button
- **THEN** the product preview opens showing the entered name, price and selected images, and no registration request is issued

#### Scenario: Preview does not require a submittable form

- **WHEN** the form has a name, a price and an image but no category, so the submit action is disabled
- **THEN** the `Vista previa` button is still enabled

#### Scenario: Disabled while submitting

- **WHEN** registration is in progress
- **THEN** the `Vista previa` button is disabled

