## Why

A store owner currently has no way to see the products registered in their store. After logging in they land on an empty Home placeholder. The "Inventario" screen is the operational heart of the Qhathia staff app — it gives the merchant real-time stock visibility and is the launch point for every other inventory action (register, edit, view detail). This change delivers that screen as the first read-only slice: list products, filter them, and see how many there are.

## What Changes

- Introduce an **Inventory ("Inventario") screen** that lists the store's products from `GET /api/v1/products`, rendering each row with thumbnail, name, price and availability indicator.
- Add a **search field** to filter the listed products by name (client-side over the loaded list).
- Add a **horizontal category filter** sourced from `GET /api/v1/categories`, with a default `Todas` chip (no filter) — selecting a category re-queries products by `categoryId`.
- Add an **availability sub-filter** (Disponibles / Stock bajo / Consultar disponibilidad → `AVAILABLE` / `LOW_STOCK` / `CONSULT`) surfaced through a filter control, alongside a **product count** label ("N productos").
- Add a **bottom navigation bar** (Inicio · Inventario · Solicitudes · Dispositivos · Perfil) as the post-login app shell, with Inventario as the active destination.
- Add an **application header** (title + notification bell) on the inventory screen.
- Add a **floating action button** for product registration (visual + reachable; the registration screen itself is out of scope).
- Extend the network/domain/data layers with Product and Category contracts, reusing the existing authenticated Ktor client and token-refresh plumbing.

**Out of scope (per the task):** navigation to product detail and to the product-registration screen. The FAB and row chevrons are present but do not navigate yet.

## Capabilities

### New Capabilities
- `product-inventory`: Listing the store's products with category and availability filtering, name search, product count, the inventory app shell (header, bottom navigation, registration FAB), and the underlying Product/Category data contracts.

### Modified Capabilities
<!-- None. merchant-auth and session-management are unchanged; this change reuses the authenticated client they established. -->

## Impact

- **New Gradle module:** `:features:product-inventory` (feature slice — screen, ViewModel, MVI contract, nav graph).
- **`:core:network`:** new `ProductDto`, `CategoryDto`, `ProductApiService`, `CategoryApiService`; provided in `NetworkModule` over the existing `@AuthenticatedClient`.
- **`:core:domain`:** new `Product`, `Category`, `Availability` models; `ProductRepository` / `CategoryRepository` interfaces; `GetProductsUseCase` / `GetCategoriesUseCase`.
- **`:core:data`:** new `ProductRepositoryImpl`, `CategoryRepositoryImpl`, DTO→domain mappers; bound in `DataModule`.
- **`:app`:** depends on the new feature module; the `NavHost` gains the inventory destination wrapped in a bottom-navigation scaffold.
- **APIs consumed:** `GET /api/v1/products` (query params `categoryId`, `availability`), `GET /api/v1/categories`. Both require a Bearer JWT (already injected by the authenticated client).
- **Contract note:** the mockup row shows a numeric "23 stock", but `GET /api/v1/products` exposes no stock quantity yet — only an `availability` enum. Backend will add the stock field later; for now the row shows a mocked "N stock" value behind a single helper, swappable in one line when the real field lands (see design.md).
