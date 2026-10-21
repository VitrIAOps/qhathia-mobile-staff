## Context

- `ProductCountAndFilterBar` renders `stringResource(R.string.inventory_product_count, productCount)` with `inventory_product_count` = `%1$d productos` / `%1$d products`. It is the only `%1$d <plural noun>` plain string in the app's resources.
- `RankingTab` builds both cards with a private `RankingCard(title, totalLabel, caption, accent, trendIcon, emptyMessage, isEmpty, rows)`. The card always draws title → bordered summary row (trend icon, total, caption) → empty message or rows. With no rows the summary reads `S/ 0` plus a caption computed from a count of zero.

## Goals / Non-Goals

**Goals:**
- Correct number agreement for the Inventario count in both languages.
- An empty Ranking card says one thing — that it is empty — instead of a zero total, a trend glyph and a "0 products" caption.

**Non-Goals:**
- Changing the empty messages' wording.
- Changing non-empty cards in any way.
- Auditing every string for plurals beyond the one found (the grep for `%1$d <noun>s` plain strings returns only this one).

## Decisions

### 1. `inventory_product_count` becomes a `<plurals>` resource

Same name, now a plural (`one` = "%1$d producto" / "%1$d product", `other` = "%1$d productos" / "%1$d products"), read with `pluralStringResource(R.plurals.inventory_product_count, productCount, productCount)`. Keeping the name keeps the diff to the resource type and the one call site. Spanish uses `one` only for 1, so 0 reads "0 productos", which is correct.

### 2. `RankingCard` skips the summary when empty

Wrap the summary `Row` in `if (!isEmpty)`. The card keeps its title (so the tab still shows both sections and their headings for screen readers) and its empty message. `totalLabel` and `caption` are still computed by the caller — cheap, and it keeps the call sites unchanged; they are just not drawn. Alternative considered: hiding the whole card when empty — rejected, the empty messages ("Aún no hay ventas en este periodo.", "Todo tu inventario se movió en este periodo.") are useful information, especially the second one.

### 3. Datos personales subtitle

`menu_personal_subtitle` changes from "Tu cuenta y los datos de tu tienda" to "Nombre, correo y teléfono" (en: "Name, email and phone"): the three fields the screen shows. It stays a static string. Store data (name, WhatsApp, address) lives in `PUT /stores/{id}` and has no screen in the app yet; if one is added, the subtitle can grow then.

## Risks / Trade-offs

- **Layout height changes when a card goes from empty to non-empty** → acceptable; the content genuinely changes.
- **Captions for zero no longer reachable** → the plural resources still define them; no cleanup needed, they remain correct if ever shown.
