## Why

Two defects in the order detail dialog, found in the 2026-10-21 emulator pass:

- **"Confirmar pedido" is clipped.** The button has a fixed height (`.height(ButtonHeight)`) and shares the row with `Rechazar`; at the dialog's width the label wraps to two lines and the second line ("pedido") is cut off.
- **Resolved orders show actions.** Opening an accepted order shows `Rechazar` and `Confirmar pedido`, greyed out. They can never be used — confirming or rejecting a terminal order returns `422` — so they only suggest that something could be done. Product decision (2026-10-21): resolved orders show no actions.

## What Changes

- The dialog shows `Rechazar` and `Confirmar pedido` only for an order that can still be acted on: `PENDING` and not past its expiry. While a confirm or reject is in flight they stay visible, disabled, with the progress indicator.
- For any other order (confirmed, rejected, cancelled, expired, or pending but past its expiry) the actions are replaced by a one-line status: "Esta solicitud ya fue confirmada", "… rechazada", "… fue cancelada", "Esta solicitud venció".
- The confirm button grows to fit its label instead of clipping it.

## Capabilities

### New Capabilities

_None._

### Modified Capabilities

- `order-requests`: the detail dialog offers confirm/reject only on actionable orders and states the outcome on resolved ones.

## Impact

- **`features:order-requests`** — `OrderDetailDialog` (actions vs. status line, button sizing), strings (es/en), previews, and a small pure helper for "actionable / which status" with unit tests.
- **No domain, data or network changes.**
- **Not in scope:** the card-level `Rechazar` action (cards of resolved orders already show only `Ver detalle`), and the dark-theme work.
