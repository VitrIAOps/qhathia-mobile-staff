## 1. Footer decision

- [x] 1.1 `features:order-requests` — add `OrderDetailFooter` and `orderDetailFooter(status, expiresAt, now)` (design Decision 1)
- [x] 1.2 Strings (es/en): `order_detail_resolved_confirmed`, `_rejected`, `_cancelled`, `_expired` ("Esta solicitud ya fue confirmada" / "This request was already confirmed", etc.)
- [x] 1.3 Unit tests: PENDING not expired → Actions; PENDING expired → Resolved(expired); CONFIRMED / REJECTED / CANCELLED / EXPIRED → their messages; null `expiresAt` on PENDING → Actions

## 2. Dialog

- [x] 2.1 `OrderDetailBody` — render the footer from `orderDetailFooter(...)`: actions row (`enabled = !isSubmitting`) or the resolved line (design Decision 2); remove `canAct`
- [x] 2.2 Confirm button — `.heightIn(min = Dimens.ButtonHeight)` and a centered label (design Decision 3)
- [x] 2.3 Previews: pending (existing), confirmed, expired, submitting; light and dark for the confirmed one

## 3. Verification

- [x] 3.1 `./gradlew :features:order-requests:testDebugUnitTest :features:order-requests:lintDebug`
- [x] 3.2 Full build: `./gradlew testDebugUnitTest lintDebug assembleDebug`

## QA (manual, run by QA)

- Aceptados and Rechazados: opening an order shows no buttons and the matching "Esta solicitud ya fue …" line
- Pendientes: buttons shown; "Confirmar pedido" fully visible on a small screen and at large font scale
- Confirming a pending order: both buttons stay visible and disabled with progress until the result arrives
