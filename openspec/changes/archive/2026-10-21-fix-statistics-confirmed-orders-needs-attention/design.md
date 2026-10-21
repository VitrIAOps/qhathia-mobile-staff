## Context

Both fixes follow the pattern behind most of the 2026-10-18 triage: the backend sends the data, the app drops it at the mapper.

- `GET /dashboard/activity` returns `confirmedOrders` and `confirmedOrdersDeltaPercent` next to `uniqueSessions` and `productViews` (`api-contract.md` §26). `StoreActivityDto` has no field for them, so `toStoreActivity()` cannot map them.
- `GET /dashboard/ranking` returns `needsAttention.totalCount`. `NeedsAttentionDto` has only `totalImmobilized` and `items`. The contract is explicit: **`totalImmobilized` is the sum of the returned `items`**, not of every product without sales; `totalCount` is the size of that universe.
- `ActivityTab` renders a single `Row` with two `ActivityFigure`s (icon, value, bold label, caption, optional `DeltaPill`) split by a `VerticalDivider`. `RankingTab` builds the needs-attention caption from `R.plurals.statistics_needs_attention_caption` with `needsAttention.size`.

Module flow is the usual one and is unchanged: `core:network` (DTO) → `core:data` (mapper) → `core:domain` (model) → `features:sales-statistics` (UI). `core:network` still does not see domain types.

## Goals / Non-Goals

**Goals:**
- The Actividad card shows the whole funnel, so "Clientes" is read next to how many actually ordered.
- The needs-attention caption describes immobilized stock and never implies the listed products are all of them.
- Older backends keep working (no crash, no invented zero).

**Non-Goals:**
- Renaming `Clientes` or restyling the existing two figures.
- Changing the ranking or activity requests, or the period logic.
- Suggesting the monthly period for the needs-attention card.

## Decisions

### 1. New fields are nullable end to end

```kotlin
// core:network
data class StoreActivityDto(…, val confirmedOrders: Int? = null, val confirmedOrdersDeltaPercent: Double? = null)
data class NeedsAttentionDto(…, val totalCount: Int? = null)

// core:domain
data class StoreActivity(…, val confirmedOrders: Int? = null, val confirmedOrdersDeltaPercent: Double? = null)
data class SalesRanking(…, val needsAttentionTotalCount: Int? = null)
```

`null` means "the backend did not say", which is different from `0`. For `confirmedOrders`, `0` is a real reading (shown as `0`) and `null` hides the figure: a default of `0` would show an older backend's missing field as "no orders", the kind of invented number this triage is about. Domain defaults are `null` and placed last, so existing constructors (fixtures, previews) still compile. The delta goes through the existing `finiteOrNull()`. `totalCount` is mapped as-is; a negative value is treated as missing.

### 2. The third figure sits under the first two, full width

Three `ActivityFigure`s in one row do not fit a 360 dp phone: each carries a value, a two-line label/caption and a pill like `+ 13% vs semana pasada`. The card becomes:

```
Row   [ Clientes · usaron la tablet ] | [ Productos · consultados ]
HorizontalDivider (rowDivider)
Row   [ Pedidos confirmados · concretaron una compra ]
```

The bottom figure reuses `ActivityFigure` unchanged (icon `Icons.Outlined.ShoppingBag`, from `material-icons-extended`, which the module already depends on), label `statistics_activity_orders` ("Pedidos" / "Orders") and caption `statistics_activity_orders_caption` ("confirmados" / "confirmed"), mirroring the two-part "Clientes / usaron la tablet" wording. Order matches the funnel the contract describes (people → views → orders). When `confirmedOrders == null` the divider and the bottom row are not drawn, which is today's card exactly.

### 3. The caption is chosen in one pure function

```kotlin
internal sealed interface NeedsAttentionCaption {
    data class All(val listed: Int) : NeedsAttentionCaption           // "Inmovilizados en los N productos sin ventas"
    data class Partial(val listed: Int, val total: Int) : NeedsAttentionCaption // "… en N de los M productos sin ventas"
}
internal fun needsAttentionCaption(listed: Int, totalCount: Int?): NeedsAttentionCaption =
    if (totalCount != null && totalCount > listed) Partial(listed, totalCount) else All(listed)
```

A pure function in `features:sales-statistics` keeps the decision unit-testable without Compose. `All` uses the existing plural resource, reworded (es: one = "Inmovilizados en el producto sin ventas", other = "Inmovilizados en los %1$d productos sin ventas"; en: "Tied up in the unsold product" / "Tied up in the %1$d unsold products" — the English already said "tied up"). `Partial` uses a new plural keyed by the total, `statistics_needs_attention_caption_partial` ("Inmovilizados en %1$d de los %2$d productos sin ventas" / "Tied up in %1$d of the %2$d unsold products"); its `one` form is unreachable (`total > listed ≥ 1`) but present so lint and translators see a complete plural. `totalCount < listed` (inconsistent backend) falls back to `All`, which never claims more than is on screen.

## Risks / Trade-offs

- **Figma has no third figure** → the layout in Decision 2 is new. It reuses the card's own components and tokens, and is flagged for the manual Figma pass; if design wants a different arrangement, only `ActivityTab` changes.
- **`Clientes` keeps its label** → the misreading is fixed by context (the confirmed-orders figure right below), not by renaming, to stay on the approved design. If the manual pass still finds it misleading, renaming is a string change.
- **Archive dependency** → this change modifies the `sales-statistics` main spec, which exists only after the `fix/inventory-snackbar-and-notification-badge` branch (archive of `add-sales-statistics-screen`) is merged. Archive this change after that merge.
