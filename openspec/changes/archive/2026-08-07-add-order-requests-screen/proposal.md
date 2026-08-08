## Why

The Solicitudes tab of the staff app is still a `PlaceholderScreen`: a merchant gets a push when the kiosk tablet creates a purchase order, opens the app — and finds nothing to act on. Orders expire on their own (backend TTL, `status=EXPIRED`), so every order the merchant cannot see is a sale silently lost. The backend shipped everything this screen needs in Fase 16 (`api-contract.md` §9 and §9.4): a searchable, status-filtered order list with `orderCode` and `expiresAt`, a detail with live per-item stock, and confirm/reject transitions.

## What Changes

- Replace the Solicitudes placeholder with a real **purchase-order requests screen**: header with store-local "última actualización" timestamp and a manual refresh action, a search box, and three status tabs.
- **Status tabs** map to `GET /api/v1/orders?status=…`: Pendientes → `PENDING`, Aceptados → `CONFIRMED`, Rechazados → `REJECTED,EXPIRED`. **Pendientes** is selected by default and is the only tab whose label carries a count (`Pendientes (2)`).
- **Search** by customer name or order code is delegated to the backend `q` param (debounced), not filtered client-side — it must reach orders outside the loaded page.
- Each list card shows customer name (falling back to `#<orderCode>` when the name is absent), `Código: <orderCode>`, the order total, an **expiry countdown** derived from `expiresAt` that ticks locally and turns red **in the last 3 minutes**, plus **Rechazar** and **Ver detalle** actions.
- **Detail opens as a dialog** over the list (`GET /api/v1/orders/{id}`): title `Detalle de solicitud #<orderCode>`, one row per item (name, unit price, quantity, line total), a "Quedan pocos disponibles" warning per item derived client-side from `productStock`, the order total, and **Rechazar** / **Confirmar pedido** actions.
- **Confirm / reject** call `POST /orders/{id}/confirm` and `POST /orders/{id}/reject`. Either one closes the dialog, shows a bottom snackbar — green **"Solicitud confirmada"**, red **"Solicitud rechazada"** — and **re-queries the list**. Simply dismissing the dialog closes it with no refresh and no snackbar.
- Handle the two contract-documented conflicts explicitly: `422 STOCK_INSUFFICIENT` (another sale consumed the stock) and `422 INVALID_STATE_TRANSITION` (another seller already handled the order, or it expired) — surface a message and refresh instead of leaving stale state on screen.
- Empty tabs render a centered message worded per tab (`Sin solicitudes pendientes` / `aceptadas` / `rechazadas`).

**Not in this change** (decided with the requester): `POST /orders/{id}/recover` for expired orders, push-driven auto-refresh of the list, and a rejection-reason input — reject sends no reason. Item thumbnails are drawn as a neutral placeholder because the order detail contract carries no product image.

## Capabilities

### New Capabilities
- `order-requests`: browsing purchase orders by status, searching them by customer name or order code, watching each one's expiry countdown, opening its detail, and confirming or rejecting it from the staff app.

### Modified Capabilities
<!-- None. `order-notifications` is untouched: this change deliberately does not couple the screen to push delivery. -->

## Impact

- **New module:** `features:order-requests` (Android library, Compose + Hilt) — screen, dialog, ViewModel, MVI contract, navigation graph. Registered in `settings.gradle.kts` and consumed by `:app`.
- **`core:domain`:** `Order`, `OrderItem`, `OrderStatus`, `OrderPage` models; `ConfirmOrderError` / `RejectOrderError` domain errors; `OrderRepository` port; `GetOrdersUseCase`, `GetOrderDetailUseCase`, `ConfirmOrderUseCase`, `RejectOrderUseCase`.
- **`core:network`:** `OrderApiService` (`GET /orders`, `GET /orders/{id}`, `POST /orders/{id}/confirm`, `POST /orders/{id}/reject`) on the authenticated client + order DTOs, including the `STOCK_INSUFFICIENT` ProblemDetail shape.
- **`core:data`:** `OrderRepositoryImpl` + `OrderMappers`, bound in `DataModule`.
- **`core:testing`:** `FakeOrderRepository` and order fixtures.
- **`:app`:** the `REQUESTS_ROUTE` placeholder in `MainShell` is replaced by `orderRequestsGraph()`; `:app` gains a dependency on the new feature module.
- **APIs consumed:** `GET /api/v1/orders` (`status`, `q`, `page`, `size`), `GET /api/v1/orders/{id}`, `POST /api/v1/orders/{id}/confirm`, `POST /api/v1/orders/{id}/reject` — §9 and §9.4 of `api-contract.md`.
- **Known gaps carried into implementation:** `orderCode` is `null` for orders created before backend V15 (needs a display fallback), and the effective TTL is the server's `ORDERS_TTL_MINUTES` (default 60 min) rather than the 20 min quoted in the ClickUp story — the countdown is rendered from `expiresAt`, so the server stays the authority either way.
