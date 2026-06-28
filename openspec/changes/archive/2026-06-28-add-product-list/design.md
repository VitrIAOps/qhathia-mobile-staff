## Context

The app already has a working authenticated stack established by the `merchant-auth` and `session-management` changes: a `@AuthenticatedClient` Ktor `HttpClient` with bearer token load + refresh (`KtorClientFactory` / `SessionAuthTokenProvider`), the `NetworkResult → Result` mapping convention, a Clean Architecture multi-module layout (`:core:network`, `:core:domain`, `:core:data`, `:core:ui`, `:core:common`, `:features:auth`), and a single-`NavHost` shell in `:app` whose only post-login destination is `HomePlaceholderScreen`.

This change adds the **Inventory** screen — the first authenticated data screen — as a new vertical feature slice plus the Product/Category contracts in the core layers. The UI follows the Figma mockup (`node-id=1252-4020`): header with title + notification bell, search field, horizontal category chips, a "N productos" count + filter control, product rows (image · name · price · availability), an orange registration FAB, and a 5-item bottom navigation bar.

Constraints: Kotlin + Compose + MVI, Hilt (KSP2), Ktor CIO, min SDK 26, every feature/layer is a proper Gradle module (never a package in `:app`), and all reads go through the existing authenticated client so token refresh and timeouts are reused, not reimplemented.

## Goals / Non-Goals

**Goals:**
- List the store's products from `GET /api/v1/products` with thumbnail, name, price and availability.
- Filter by category (`GET /api/v1/categories`, default `Todas`) and by availability sub-filter, combinable, both as `categoryId` / `availability` query params.
- Client-side name search over the loaded list; live product-count label.
- Reuse the authenticated client, `NetworkResult` mapping, DS components (`QhathiaTextField`, `Dimens`, theme) and the existing repository/usecase patterns.
- Introduce the post-login app shell: bottom navigation bar + inventory header + registration FAB.

**Non-Goals:**
- Navigation to product detail or to the product-registration screen (FAB and row chevrons are inert).
- Functional behavior for the Solicitudes / Dispositivos / Perfil tabs or the notification bell (placeholders/visual only).
- Pagination, offline cache, or writing/editing products. `GET /api/v1/products` returns a flat array today, so no paging contract is introduced.

## Decisions

### Module layout (Dependency Graph Rules)

Follows the standard `features/* → core:domain ← core:data → core:network`, `features/* → core:ui/common`, `app → features/* + core:*` graph.

- **New module `:features:product-inventory`** (Android Library, Compose + Hilt). Holds `InventoryScreen` (stateful), `InventoryContent` (stateless), row/chip/header/FAB composables, `InventoryViewModel`, the MVI contract (`InventoryUiState` / `InventoryUiEvent` / `InventoryUiEffect`), the Hilt module for `@Binds` repository wiring if any, and `inventoryGraph` (nav). Depends on `:core:domain`, `:core:ui`, `:core:common`. **Never** on `:core:network`/`:core:data` directly and never on another feature module.
- **`:core:network`** gains `ProductDto`, `CategoryDto`, `ProductApiService`, `CategoryApiService`; the services are constructed in `NetworkModule` over the existing `@AuthenticatedClient` (products/categories require a JWT — unlike auth's `@PublicClient`).
- **`:core:domain`** gains `Product`, `Category`, `Availability` models; `ProductRepository` / `CategoryRepository` ports; `GetProductsUseCase` / `GetCategoriesUseCase`.
- **`:core:data`** gains `ProductRepositoryImpl`, `CategoryRepositoryImpl`, DTO→domain mappers, bound in the existing `DataModule`.
- **`:app`** adds `implementation(project(":features:product-inventory"))` and wraps the post-login destinations in a bottom-navigation `Scaffold`.

Alternative considered: putting the API services in the feature module — rejected, it violates the layer rules (`core:network` owns all Ktor/DTO knowledge; features never see Ktor).

### Filtering: server-side category + availability, client-side search

`categoryId` and `availability` are sent as query params to `GET /api/v1/products` (the API supports both), so changing either re-queries the server. Name search filters the already-loaded list in the ViewModel (no `name` query param exists on the endpoint, and the merchant catalog is plan-capped at ≤100 products, so client-side filtering is cheap and instant). The product count is derived from the post-search visible list.

Alternative considered: client-side category filtering too. Rejected — the API offers `categoryId` natively and server filtering keeps the loaded set small and authoritative.

### Mocked stock indicator (interim)

The mockup shows "23 stock", but `GET /api/v1/products` returns only an `availability` enum, no quantity. Backend will add a numeric stock field later (confirmed). For this slice the row shows a mocked "N stock" value produced by a single helper (e.g. a `mockedStock(product)` function or a defaulted `stock` field on the domain `Product`), so the layout matches the mockup now and the swap to the real field is a one-line change in the mapper/helper — the row composable stays untouched. The `availability` enum is still mapped (`AVAILABLE`/`LOW_STOCK`/`CONSULT`) and drives the availability sub-filter.

### MVI state shape

`InventoryUiState` holds the loaded domain products (immutable list), the categories (with a synthetic `Todas`), `selectedCategoryId: String?`, `selectedAvailability: Availability?`, `searchQuery: String`, plus `isLoading` / `errorMessage`. The visible list and count are computed from these. Category/availability changes call the use case again; search updates state only. One-shot concerns (error snackbar) go through `InventoryUiEffect` over `SharedFlow`, matching the auth feature.

### App shell / bottom navigation in `:app`

The bottom-nav `Scaffold` lives in `:app` (the only module that owns the `NavHost` and theme entry, per the standards). `QhathiaNavHost` is extended: the inventory destination becomes the post-login start destination and is rendered inside the bottom-bar scaffold; Inicio reuses `HomePlaceholderScreen`, and the remaining tabs route to lightweight placeholders. The feature module exposes `inventoryGraph(...)`/route constants the way `authGraph` does, so `:app` wires routes without reaching into feature internals.

### Reuse of existing conventions

- DS: `QhathiaTextField` for search, `Dimens` spacing, `core:ui` theme; Coil 3 for thumbnails (add `coil-compose` to the feature module, already in the version catalog).
- Errors: `ProductApiService` returns `NetworkResult`; `RepositoryImpl` maps to `Result<T>` exactly like `AuthRepositoryImpl`.
- Timeouts/auth/refresh: inherited from `KtorClientFactory` — not re-specified per call.

## Risks / Trade-offs

- **[Mockup shows numeric stock the API lacks]** → Show a mocked "N stock" value behind a single helper; backend will add the real field later, making the swap a one-line mapper change with no row-layout rework.
- **[Multi-store JWTs may carry no active `storeId`]** → A 2+ store login yields a token without an active store; product/category calls could 403/empty. Mitigation: treat a 401/403 as the standard retryable error state; the dedicated store-switch flow is a separate change.
- **[Client-side search assumes the full list is loaded]** → Valid only because the plan caps catalogs at ≤100 items and the endpoint is unpaged. If pagination is later introduced, search must move server-side. Documented as a non-goal boundary.
- **[Categories request failing could block the screen]** → Category load is independent of product load; a category failure degrades to `Todas`-only and still shows products.
- **[Bottom-nav placeholders look unfinished]** → Acceptable for this slice; only Inventario is in scope. Other tabs are visually present, non-functional.

## Open Questions

- Should the availability also be shown as a colored badge/chip (DS) on the row, alongside the stock indicator? Pending DS confirmation; not required for this slice.
- Backend will expose a numeric stock quantity later (confirmed) — the mocked helper is the single swap point when that field lands.
- Exact category-strip "active chip" styling and the filter control's presentation (bottom sheet vs. inline menu) — to be confirmed against the DS during implementation.
