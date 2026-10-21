## Why

The sales statistics screen shows two numbers that merchants read as something they are not (triage 2026-10-18, `docs/handoff/android-pendientes.md` §3.2 and §3.3):

- **"Clientes" reads as "customers who bought"** (BUG-003). The Actividad tab shows `uniqueSessions` — everyone who used the tablet, bought or not — next to product views. With nothing that says how many actually ordered, "Clientes: 72" is read as 72 sales. Since 2026-10-18 `GET /dashboard/activity` also returns `confirmedOrders` and its delta, the number that *is* "concretó", but the app drops it in the mapper.
- **"Lo que necesita atención" reads as income** (BUG-004). The card shows `S/ <total>` captioned `Generados por los N productos no vendidos`, in the same visual language as the revenue card above it. That amount is money standing still on the shelf, the opposite of revenue. It also adds up only the products listed (at most three), while the backend now sends `needsAttention.totalCount` — how many products did not sell in total — which the app does not read, so "3 productos" hides "3 of 17".

## What Changes

- Map `confirmedOrders` and `confirmedOrdersDeltaPercent` from `GET /dashboard/activity` into `StoreActivity`.
- The Actividad card shows a third figure, **Pedidos confirmados**, with its variation pill, under the two existing figures, so the card reads as a funnel: people who used the tablet → products viewed → orders confirmed. The figure is hidden only if the backend omits the field.
- Map `needsAttention.totalCount` from `GET /dashboard/ranking` into `SalesRanking`.
- The `Lo que necesita atención` caption stops saying "Generados": it reads `Inmovilizados en los N productos sin ventas`, and `Inmovilizados en N de los M productos sin ventas` when more products did not sell than the card lists.

## Capabilities

### New Capabilities

_None._

### Modified Capabilities

- `sales-statistics`: the Actividad tab gains the confirmed-orders figure; the needs-attention caption describes immobilized stock (not revenue) and states how many of the total are listed.

## Impact

- **`core:network`** — `StoreActivityDto` gains `confirmedOrders` and `confirmedOrdersDeltaPercent`; `NeedsAttentionDto` gains `totalCount`. All nullable with defaults, so an older backend still deserializes.
- **`core:domain`** — `StoreActivity` gains `confirmedOrders: Int?` and `confirmedOrdersDeltaPercent: Double?`; `SalesRanking` gains `needsAttentionTotalCount: Int?`.
- **`core:data`** — `toStoreActivity()` and `toSalesRanking()` map the new fields; mapper tests.
- **`core:testing`** — `TestFixtures.storeActivity(...)` / `salesRanking(...)` gain the new parameters with defaults.
- **`features:sales-statistics`** — `ActivityTab` (third figure), `RankingTab` (caption), strings (es/en), preview data.
- **No API changes**: the fields are in production since 2026-10-18 (`docs/android/api-contract.md` §26).
- **Archive order:** the `sales-statistics` main spec is created by the archive of `add-sales-statistics-screen`, which lives on the `fix/inventory-snackbar-and-notification-badge` branch. This change must be archived after that branch is merged.
- **Not in scope:** renaming the `Clientes` figure (the Figma label is kept; the third figure is what disambiguates it), and nudging the merchant toward the monthly view for this card.
