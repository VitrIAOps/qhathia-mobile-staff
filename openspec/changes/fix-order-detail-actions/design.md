## Context

`OrderDetailBody` computes `canAct = status == PENDING && !isExpired(expiresAt, now()) && !isSubmitting` and always draws a `Row` with a `TextButton(Rechazar)` and a `Button(Confirmar pedido)` both `enabled = canAct`; the confirm button is `.weight(1f).height(Dimens.ButtonHeight)` (48 dp). So:

- resolved orders get two permanently disabled buttons, indistinguishable from "wait, something is loading";
- at half the dialog width, "Confirmar pedido" wraps and the fixed 48 dp height clips the second line.

`isExpired` and `OrderStatus` already exist in the feature; `OrderStatus` documents `PENDING` as the only actionable state.

## Goals / Non-Goals

**Goals:**
- Actions only where they can work; a clear outcome where they cannot.
- No clipped labels at any font scale.

**Non-Goals:**
- Changing confirm/reject behaviour, errors or snackbars.
- Showing who or when an order was resolved (the order DTO does not carry it).

## Decisions

### 1. One pure function decides what the footer shows

```kotlin
internal sealed interface OrderDetailFooter {
    data object Actions : OrderDetailFooter
    data class Resolved(@StringRes val messageRes: Int) : OrderDetailFooter
}
internal fun orderDetailFooter(status: OrderStatus, expiresAt: Instant?, now: Instant): OrderDetailFooter
```

`PENDING` and not expired → `Actions`; otherwise `Resolved` with the message for the status (an expired `PENDING` maps to the expired message). Unit-tested on the JVM. `isSubmitting` is deliberately not an input: an in-flight request keeps `Actions` (disabled), so the buttons do not vanish under the merchant's finger.

### 2. Footer rendering

- `Actions` → the existing row, `enabled = !isSubmitting`.
- `Resolved` → a single `Text` (`bodyMedium`, `onSurfaceVariant`, centered, full width) in the footer's place.

### 3. Button sizing

Replace `.height(Dimens.ButtonHeight)` with `.heightIn(min = Dimens.ButtonHeight)` and center the label (`textAlign = TextAlign.Center`). Short labels keep today's 48 dp; long ones grow instead of clipping. `maxLines = 1` with ellipsis was rejected: a truncated "Confirmar pe…" on the primary action is worse than a taller button.

## Risks / Trade-offs

- **Expiry decided on the device clock** → same rule the card countdown already uses; if the clock is off, the backend still returns `422` and the existing "already handled" message covers it.
- **Taller button at large font scales** → intended; the dialog content already scrolls.
