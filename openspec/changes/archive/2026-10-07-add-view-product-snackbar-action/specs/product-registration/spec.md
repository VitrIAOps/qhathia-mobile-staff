## MODIFIED Requirements

### Requirement: Registration feedback and navigation

While registration is in progress the screen SHALL show a circular loading indicator. On success the app SHALL navigate back to the inventory list and show a success snackbar using the design system's success (green) tones. Whenever the product was created — whether or not an applied discount could be created for it — the app SHALL hand the id of the newly registered product to the inventory list on the way back, so the inventory can locate it. On a generic failure the app SHALL keep the merchant on the registration screen and show an error snackbar describing the reason.

#### Scenario: Loader during registration

- **WHEN** registration is in progress
- **THEN** a circular loading indicator is shown and form inputs are disabled

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
