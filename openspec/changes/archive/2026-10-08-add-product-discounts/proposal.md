## Why

Merchants cannot promote a product from the staff app: the Products API already exposes per-product discounts (`/api/v1/products/{id}/discounts`), but neither the registration form, the product detail nor the inventory list offer a way to create, see or change one. ClickUp task [Descuentos de productos](https://app.clickup.com/t/86e3mp889) asks for discount registration and editing, plus two small unrelated fixes bundled into the same delivery.

## What Changes

- **Discount on product registration**: a `Descuento` toggle below the price field (default off, disabled until a valid price is entered). Turning it on opens the `Aplicar descuento` dialog; once applied, a summary line appears under the toggle (`Descuento de {n}%` or `Descuento de {monto} soles`). Turning it off removes the discount. The discount is created right after the product is registered.
- **`Aplicar descuento` dialog** (shared): shows the original price struck through and the resulting price in bold; two modes — `Porcentaje` (quick options 10 %, 20 %, 50 % or a custom value up to 100 %) and `Monto` (a free amount in soles, up to the product's price); `Cancelar` / `Aplicar` actions.
- **Discount on product detail**: the same toggle and summary line, seeded from the product's current discount. Tapping the summary reopens the dialog to edit it and turning the toggle off removes it; like any other field on the form, the change is only persisted when the merchant taps `Guardar`, and it is covered by the discard-confirmation dialog.
- **Product row actions menu**: the inventory row's `⋮` becomes a real menu, anchored at the icon, with `Editar producto` (opens the product detail, as tapping the row does today) and `Editar descuento` (opens the dialog for that product).
- **Replace semantics**: applying a discount to a product that already has one deletes the existing discount(s) first and then creates the new one. Every created discount lasts one week (`startsAt = now`, `endsAt = now + 7 days`).
- **Unrelated fixes**:
  - Text search bars (Inventario and Solicitudes) get a trailing `x` that clears the entered text.
  - The `Cantidad` field label becomes `Stock` on the product registration and product detail forms.
  - Tags become optional on both forms; with none entered the product is sent with an empty tag list.
  - Form field placeholders are drawn fainter so an empty field does not read as a filled one.

## Capabilities

### New Capabilities

- `product-discounts`: the discount model and rules (percentage vs. fixed amount, validation, resulting price, one-week duration), the shared `Aplicar descuento` dialog, the summary label, and the read / replace / remove operations against the product discounts API.

### Modified Capabilities

- `product-registration`: adds the discount toggle and summary to the form, creating the discount after the product is registered and handling a discount failure after a successful registration; renames the quantity field label to `Stock`.
- `product-detail`: adds loading, editing and removing the product's discount from the detail form, persisted on save; renames the quantity field label to `Stock`.
- `product-inventory`: adds the per-row actions menu (`Editar producto`, `Editar descuento`) and the clear action on the search field.
- `order-requests`: adds the clear action on the search field.

## Impact

- **API** (already documented in `docs/android/api-contract.md`, no backend change): `GET /api/v1/products/{id}/discounts`, `POST /api/v1/products/{id}/discounts`, `DELETE /api/v1/products/{id}/discounts/{discountId}`.
- **Modules** (all existing — no new Gradle module):
  - `:core:network` — discount DTOs and three `ProductApiService` calls.
  - `:core:domain` — `Discount` model, discount draft/validation rules, `ProductDiscountRepository` port, get / save / remove use cases.
  - `:core:data` — `ProductDiscountRepositoryImpl`, mappers, Hilt binding.
  - `:core:ui` — shared `QhathiaDiscountDialog` and discount toggle row.
  - `:core:testing` — `FakeProductDiscountRepository` and fixtures.
  - `:features:product-register`, `:features:product-detail`, `:features:product-inventory` — state, events, ViewModel logic and UI for the discount flows; label and search-clear fixes.
  - `:features:order-requests` — search-clear fix only.
  - `:app` — one additional inventory result string for "registered, but the discount could not be applied".
- **No new dependencies, permissions or manifest entries.**
