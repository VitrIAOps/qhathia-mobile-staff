## ADDED Requirements

### Requirement: Actions only on actionable orders in the detail dialog

The order detail dialog SHALL show the `Rechazar` and `Confirmar pedido` actions only when the order is actionable: its status is `PENDING` and it has not reached its `expiresAt`. While a confirm or reject request for that order is in flight, the actions SHALL remain visible but disabled, with progress shown on the confirm button. For any order that is not actionable the dialog SHALL NOT show either action, and SHALL instead show one line stating the outcome:

- `CONFIRMED` → "Esta solicitud ya fue confirmada"
- `REJECTED` → "Esta solicitud ya fue rechazada"
- `CANCELLED` → "Esta solicitud fue cancelada"
- `EXPIRED`, or `PENDING` past its `expiresAt` → "Esta solicitud venció"

The `Confirmar pedido` label SHALL never be clipped: if it does not fit on one line, the button SHALL grow to show it in full.

#### Scenario: Pending order
- **WHEN** the merchant opens the detail of a pending order that has not expired
- **THEN** `Rechazar` and `Confirmar pedido` are shown and enabled

#### Scenario: Confirmed order
- **WHEN** the merchant opens the detail of a confirmed order
- **THEN** no action buttons are shown and the dialog reads "Esta solicitud ya fue confirmada"

#### Scenario: Rejected order
- **WHEN** the merchant opens the detail of a rejected order
- **THEN** no action buttons are shown and the dialog reads "Esta solicitud ya fue rechazada"

#### Scenario: Pending order past its expiry
- **WHEN** the merchant opens the detail of an order still marked `PENDING` whose `expiresAt` has passed
- **THEN** no action buttons are shown and the dialog reads "Esta solicitud venció"

#### Scenario: Request in flight
- **WHEN** the merchant has tapped `Confirmar pedido` and the request has not answered yet
- **THEN** both actions are visible and disabled, and the confirm button shows progress

#### Scenario: Long confirm label
- **WHEN** the confirm label does not fit on one line (narrow screen or large font)
- **THEN** the button grows and the whole label is visible
