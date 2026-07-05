## ADDED Requirements

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
