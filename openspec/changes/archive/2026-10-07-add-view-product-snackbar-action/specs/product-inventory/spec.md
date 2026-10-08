## MODIFIED Requirements

### Requirement: Inventory refresh after successful registration

After a product is successfully registered and the app returns to the Inventory screen, the inventory list SHALL refresh so the newly registered product can appear, and a success snackbar reading `Producto registrado correctamente` SHALL be shown using the design system's success (green) tones. That snackbar SHALL carry a text action labelled `Ver producto`, SHALL stay visible long enough to be tapped, and SHALL still dismiss on its own when the action is not used. The plan-limit and product-updated messages SHALL NOT carry this action.

#### Scenario: Returning from a successful registration

- **WHEN** the merchant completes a product registration and returns to the Inventory screen
- **THEN** the inventory list re-queries its products and a green success snackbar confirms the registration with a `Ver producto` action

#### Scenario: Snackbar dismisses without the action

- **WHEN** the success snackbar is shown and the merchant does not tap `Ver producto`
- **THEN** the snackbar dismisses on its own and the list keeps its scroll position and filters

#### Scenario: Returning after a plan-limit rejection

- **WHEN** the merchant returns to the Inventory screen because registration was rejected for exceeding the plan's product limit
- **THEN** a message indicating the plan limit was exceeded is shown on the Inventory screen, without a `Ver producto` action

### Requirement: Inventory feedback when a registration's discount fails

When the app returns to the Inventory screen after a product was registered but its discount could not be created, the inventory list SHALL refresh and SHALL show a message distinct from the plain registration success message. Because the product exists, that message SHALL carry the same `Ver producto` action as the registration success snackbar, with the same visibility and dismissal behaviour.

#### Scenario: Returning from a registration with a failed discount

- **WHEN** the merchant returns to the Inventory screen because the product was registered but its discount could not be applied
- **THEN** the inventory list re-queries its products and a message states that the product was registered but the discount could not be applied, with a `Ver producto` action

## ADDED Requirements

### Requirement: View the registered product from the registration snackbar

Tapping `Ver producto` on the registration success snackbar, or on the discount-failed registration snackbar, SHALL locate the newly registered product, by its id, in the inventory list and SHALL scroll the list to that product's row. The action SHALL NOT navigate away from the Inventory screen. If the name search is hiding the product, the search SHALL be cleared so the row can be shown, without issuing a request. If the active category or availability filter excludes the product from the loaded list, the category filter SHALL return to `Todas`, the availability sub-filter and the search SHALL be cleared, the list SHALL be re-queried without filters and SHALL then scroll to the product's row. If the post-registration refresh is still in flight when the action is tapped, the scroll SHALL happen once the refreshed list is available. If the product is still not in the list with no filters active, the list SHALL NOT scroll and a message SHALL say the product could not be found in the list.

#### Scenario: Product is in the visible list

- **WHEN** the merchant taps `Ver producto` and the registered product is among the visible rows of the list
- **THEN** the list scrolls to that product's row and the merchant stays on the Inventory screen

#### Scenario: Product is hidden by the search

- **WHEN** the merchant taps `Ver producto` while a search text hides the registered product, which is in the loaded list
- **THEN** the search field is cleared, the full loaded list is shown and it scrolls to that product's row, without a new request

#### Scenario: Product is excluded by the active filters

- **WHEN** the merchant taps `Ver producto` and the registered product is not in the loaded list because of the selected category or availability filter
- **THEN** the `Todas` category becomes selected, the availability sub-filter and the search are cleared, the list is re-queried without filters and it scrolls to that product's row

#### Scenario: Action tapped while the list is still refreshing

- **WHEN** the merchant taps `Ver producto` before the post-registration refresh has finished
- **THEN** the list scrolls to the product's row as soon as the refreshed list arrives

#### Scenario: Action from the discount-failed snackbar

- **WHEN** the merchant taps `Ver producto` on the snackbar stating the product was registered but the discount could not be applied
- **THEN** the list scrolls to that product's row exactly as it does from the success snackbar

#### Scenario: Load fails after the action was tapped

- **WHEN** the merchant taps `Ver producto` and the list load it is waiting for fails
- **THEN** the list does not scroll and the usual inventory load error is shown

#### Scenario: Product is not in the unfiltered list

- **WHEN** the merchant taps `Ver producto`, no category or availability filter is active and the registered product is not in the loaded list
- **THEN** the list does not scroll, no further request is issued and a message says the product could not be found in the list
