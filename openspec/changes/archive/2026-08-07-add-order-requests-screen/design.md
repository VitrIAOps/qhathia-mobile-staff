## Context

The Solicitudes tab in `MainShell.kt` is a `PlaceholderScreen`. Everything below it already exists: an authenticated Ktor client with token refresh (`core:network`), the `NetworkResult → Result` boundary convention (`core:data`), and an FCM pipeline that already notifies merchants of `NEW_ORDER` / `ORDER_TAKEN` (`core:notifications`). What is missing is the screen those notifications point at.

The backend side is fully specified in `docs/android/api-contract.md` §9 + §9.4 and needs no changes. Two properties of that contract shape this design:

1. **Orders expire server-side.** A `PENDING` order carries `expiresAt`; a job flips it to `EXPIRED` and that state is terminal. The app renders a countdown but is never the authority — the confirm/reject calls can lose the race and return `422`.
2. **List and detail are different shapes.** `GET /orders` returns a compact summary (no `items[]`); `GET /orders/{id}` returns the full order with `items[]`, `productStock` and `productAvailability`. The detail dialog therefore always fetches, it cannot be composed from the list row.

Design references: [list](https://www.figma.com/design/uLhEDY8ZMxdNiQZU4uYA1V/Qhatia-DS?node-id=1694-13008) · [detail dialog](https://www.figma.com/design/uLhEDY8ZMxdNiQZU4uYA1V/Qhatia-DS?node-id=1694-11964).

## Goals / Non-Goals

**Goals:**

- A Solicitudes tab where a merchant can find any order (by tab, by search), read its full detail, and confirm or reject it.
- Never show the merchant a decision that has already been made elsewhere: every terminal-state conflict from the backend resolves into a message plus a refresh.
- Keep the layering rules intact — no Ktor or DTO type escapes `core:data`, and the new feature module depends on no other feature module.

**Non-Goals:**

- `POST /orders/{id}/recover` for expired orders (deliberately deferred; `EXPIRED` orders are read-only in the Rechazados tab).
- Push-driven auto-refresh. `core:notifications` stays decoupled from this screen; `GET /orders` on entry/refresh is the source of truth, exactly as the contract advises.
- A rejection-reason input. `POST /reject` is called with no body.
- Pagination UI. The screen loads the first page at a size that covers realistic daily volume; infinite scroll is a follow-up.
- Offline caching. No Room, no local order store.

## Decisions

### Module layout

A new feature module `features:order-requests` (namespace `com.qhathia.mobile.staff.feature.orderrequests`), following the Feature template in `android-frontend-standards.md`. Order data lives in the shared core layers, matching how products are already split:

```
features:order-requests → core:domain, core:ui, core:common
core:data (OrderRepositoryImpl) → core:domain + core:network (OrderApiService)
:app → features:order-requests
```

*Alternative rejected:* putting the order data layer inside the feature module. It would keep the change smaller but break the `core:data` implements `core:domain` rule and block reuse when the Inicio dashboard needs a pending-order count.

### One ViewModel, dialog state nested in the list state

`OrderRequestsViewModel` owns both the list and the detail dialog. The dialog is a nullable branch of `OrderRequestsUiState` (`detail: OrderDetailUiState?`), not a separate route with its own ViewModel.

Rationale: the dialog's outcome *is* a list mutation (confirm/reject → snackbar + refetch). A separate ViewModel would need a `savedStateHandle` result channel to say "I confirmed, refresh yourself", which is the pattern `MainShell` already has to use for product registration and which is more machinery than a dialog warrants.

*Alternative rejected:* a `composable` route + `savedStateHandle` result key, as `productRegisterGraph` does. That pattern earns its cost for a full-screen form; here it would also fight the "dialog over the list" design.

### Tabs are server-side filters; the count comes from the request

Each tab maps to a `status` query value — `PENDING`, `CONFIRMED`, `REJECTED,EXPIRED` — and switching tabs re-queries. The `Pendientes (n)` badge is `totalElements` from the pending response, per the contract note that there is no dedicated count endpoint. Because only the Pendientes tab shows a count, the badge is refreshed by a `size=1` pending query issued alongside every load, so the badge stays correct while the merchant is looking at Aceptados or Rechazados.

*Alternative rejected:* counting the loaded list client-side. It is wrong as soon as the result set exceeds one page.

### Search is server-side and debounced

The `q` param searches customer name (partial) **and** order-code prefix — the backend does what the story asks for, and it reaches orders beyond the loaded page. Keystrokes are debounced (~350 ms) in the ViewModel via `flatMapLatest` over a query `MutableStateFlow`, so typing does not issue a request per character and a stale response can never overwrite a newer one.

*Alternative rejected:* client-side filtering of the loaded list, as `InventoryViewModel` does for products. Inventory can do it because it loads the whole catalogue; orders are paginated, so client-side search would silently hide matching orders.

### The countdown ticks in the UI, `expiresAt` is the truth

`Order.expiresAt` is carried as an `Instant`. A single `LaunchedEffect` ticking once per second at the list level drives a `now` state that every card derives its remaining time from — one ticker for the screen, not one per card. Below 3 minutes the label switches to the red/pill treatment; at zero the card renders as expired and its actions are disabled, but the row is not removed until the next refresh (the server decides, not the client).

*Alternative rejected:* a timer per card. It multiplies recompositions across a `LazyColumn` for a value that changes identically for every row.

### Errors map to domain types at the repository boundary

`ConfirmOrderUseCase` / `RejectOrderUseCase` return `Result<Order>`, with failures carrying a domain error rather than an `IllegalStateException`, following the `RegisterProductError` precedent:

| Backend | Domain error | UI |
|---|---|---|
| `422 STOCK_INSUFFICIENT` | `ConfirmOrderError.StockInsufficient(productName, available, requested)` | message naming the product, dialog closes, list refreshes |
| `422 INVALID_STATE_TRANSITION` | `…Error.AlreadyHandled` | "otro vendedor ya atendió este pedido", dialog closes, list refreshes |
| `404` | `…Error.NotFound` | generic message + refresh |
| network / other | `…Error.Network` / `Unknown` | retryable message, dialog stays open |

Parsing `STOCK_INSUFFICIENT` needs the ProblemDetail body, not just the status code, so `OrderApiService` reads the error body for the confirm call specifically and surfaces it as a typed DTO. Every conflict outcome ends in a refresh, so the screen cannot keep showing an order the backend has already closed.

### Display fallbacks are explicit, not incidental

- `customerName` blank → the card title renders `#<orderCode>` (matches the design's second card).
- `orderCode` null (orders predating backend V15) → the `Código:` line falls back to the order id's short prefix, and the dialog title omits the `#<code>` suffix.
- `productStock` null (CONSULT products) → no low-stock warning; a non-null `productStock` at or below the client-side threshold renders "Quedan pocos disponibles".
- Item thumbnails: the detail contract carries no image URL, so the slot renders a neutral placeholder that preserves the design's layout — no per-item product fetch (which would be an N+1 on dialog open).

### Header timestamp

"Última actualización a las HH:mm" is the device clock captured when a load succeeds, formatted with the device locale. It is a freshness cue for a screen without live updates, and it lives in `UiState` as an `Instant` so the composable stays a pure function of state.

## Risks / Trade-offs

- **The merchant acts on a stale list** (order expired or was handled on another phone since the last load) → every mutation goes through the backend, whose `422` responses are mapped to explicit messages and always followed by a refresh. The UI never assumes its own optimistic outcome.
- **No auto-refresh means an order can expire while it sits on screen.** → The countdown makes the deadline visible and turns red in the final 3 minutes; disabled actions on a zeroed countdown stop the merchant from firing a call that is guaranteed to `422`. Push-driven refresh remains the natural follow-up.
- **First page only.** An unusually busy day can push orders past the loaded page in the Aceptados/Rechazados tabs. → Server-side search reaches them by name or code; a generous page size limits the exposure, and pagination is a contained follow-up because filtering is already server-side.
- **Countdown accuracy depends on device clock skew** against the server's `expiresAt`. → Skew shows up as a countdown a few minutes off, never as a wrong decision: the server rejects any late confirm with `422`.
- **A one-second ticker recomposing a `LazyColumn`.** → The ticker updates a single `now` value read inside the card's countdown composable, with stable `key`s on `items()` so only the label subtree recomposes.
- **The story says 20-minute TTL; the backend default is 60.** → The app never hardcodes a TTL; it renders `expiresAt`. If the operator changes `ORDERS_TTL_MINUTES`, the app follows with no release.
