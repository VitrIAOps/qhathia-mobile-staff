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

The Product Detail screen SHALL let the merchant edit the product's name, description, category, price, stock and tags, validating each field as it is edited using the same rules as product registration. Photos SHALL be shown read-only; adding or removing them is not offered on this screen.

#### Scenario: Saving is blocked until the form is valid

- **WHEN** any editable field is empty or invalid, or no category is selected, or no tag is entered
- **THEN** the save action is disabled and the offending field shows its validation message

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
