## MODIFIED Requirements

### Requirement: Open the order detail as a dialog

Selecting **Ver detalle** on a card SHALL fetch `GET /api/v1/orders/{id}` and present the result as a dialog over the list, titled `Detalle de solicitud #<orderCode>`. The dialog MUST list every order item with its product name, unit price, quantity and line total, and MUST show the order total. An item whose `productStock` is `0` MUST display an out-of-stock warning reading `Sin existencias`, styled with the theme's error colours; it MUST NOT display the low-stock warning, because a stock-out is not "a few left". An item whose `productStock` is above `0` and at or below the low-stock threshold (5) MUST display the `Quedan pocos disponibles` warning. An item with no stock information (consult-mode products) MUST NOT display either warning. Dismissing the dialog SHALL only close it — with no snackbar and no list refresh.

#### Scenario: Detail is fetched when opened
- **WHEN** the merchant taps `Ver detalle` on an order
- **THEN** `GET /api/v1/orders/{id}` is requested and the dialog shows the returned items and total

#### Scenario: Low stock warning per item
- **WHEN** an item's product stock is between 1 and the low-stock threshold, inclusive
- **THEN** that item shows the `Quedan pocos disponibles` warning

#### Scenario: Sold-out item shows the out-of-stock warning
- **WHEN** an item's product stock is `0`
- **THEN** that item shows `Sin existencias` in the error colours and does not show `Quedan pocos disponibles`

#### Scenario: Stock above the threshold shows no warning
- **WHEN** an item's product stock is above the low-stock threshold
- **THEN** no stock warning is shown for that item

#### Scenario: Consult-mode item shows no stock warning
- **WHEN** an item's product stock is unknown (consult mode)
- **THEN** no stock warning is shown for that item

#### Scenario: Detail load failure
- **WHEN** the detail request fails
- **THEN** an error message is shown for that order and the list underneath remains intact

#### Scenario: Dismissing the dialog changes nothing
- **WHEN** the merchant closes the dialog without confirming or rejecting
- **THEN** the dialog closes, no snackbar is shown and the list is not refreshed

## ADDED Requirements

### Requirement: Solicitudes header has no back control

Solicitudes is a root bottom-navigation destination, so its header SHALL NOT show a back chevron or any control that suggests going back. The header SHALL NOT expose a back action to accessibility services. The title SHALL start at the header's horizontal padding.

#### Scenario: No back chevron in the header
- **WHEN** the Solicitudes tab is shown
- **THEN** the header shows the title, the last-update time and the refresh action, and no back chevron

#### Scenario: TalkBack announces no back action
- **WHEN** a screen-reader user explores the Solicitudes header
- **THEN** no element is announced as `Volver`
