## 1. Network layer (`:core:network`)

- [x] 1.1 Add `ProductDto` (`id`, `categoryId`, `categoryName`, `name`, `price`, `availability`, `allowDiscountInquiry`, `position`, `primaryImageUrl` — all `@Serializable`, nullable where the API allows) in `core/network/.../dto/ProductDtos.kt`
- [x] 1.2 Add `CategoryDto` (`id`, `storeId`, `name`, `slug`, `position`, `active`) in `core/network/.../dto/CategoryDtos.kt`
- [x] 1.3 Add `ProductApiService` with `suspend fun getProducts(categoryId: String?, availability: String?): NetworkResult<List<ProductDto>>` — `client.get("api/v1/products") { parameter(...) }` for non-null params, wrapped in `runCatching { ... }.toNetworkResult()`
- [x] 1.4 Add `CategoryApiService` with `suspend fun getCategories(): NetworkResult<List<CategoryDto>>`
- [x] 1.5 Provide `ProductApiService` and `CategoryApiService` in `NetworkModule` using the existing `@AuthenticatedClient` HttpClient

## 2. Domain layer (`:core:domain`)

- [x] 2.1 Add `Availability` enum (`AVAILABLE`, `LOW_STOCK`, `CONSULT`) in `core/domain/.../model/Availability.kt`
- [x] 2.2 Add `Product` domain model (`id`, `categoryId`, `categoryName`, `name`, `price`, `availability`, `primaryImageUrl`) in `core/domain/.../model/Product.kt`
- [x] 2.3 Add `Category` domain model (`id`, `name`) in `core/domain/.../model/Category.kt`
- [x] 2.4 Add `ProductRepository` interface: `suspend fun getProducts(categoryId: String?, availability: Availability?): Result<List<Product>>`
- [x] 2.5 Add `CategoryRepository` interface: `suspend fun getCategories(): Result<List<Category>>`
- [x] 2.6 Add `GetProductsUseCase` and `GetCategoriesUseCase` (constructor-injected, `operator fun invoke`)

## 3. Data layer (`:core:data`)

- [x] 3.1 Add `productMappers.kt` / `categoryMappers.kt` with `ProductDto.toDomain()` (map `availability` string → `Availability`, defaulting unknown to `CONSULT`) and `CategoryDto.toDomain()`
- [x] 3.2 Add `ProductRepositoryImpl` mapping `NetworkResult → Result<List<Product>>` (HttpError/NetworkError → `Result.failure`), mirroring `AuthRepositoryImpl`
- [x] 3.3 Add `CategoryRepositoryImpl` with the same mapping pattern
- [x] 3.4 Bind `ProductRepository` and `CategoryRepository` in `DataModule` (`@Binds @Singleton`)

## 4. Create the feature module (`:features:product-inventory`)

- [x] 4.1 Create the module directory `features/product-inventory/src/main/java/com/qhathia/mobile/staff/feature/productinventory/` (and matching `src/test/...`)
- [x] 4.2 Write `features/product-inventory/build.gradle.kts` from the Feature module template (namespace `com.qhathia.mobile.staff.feature.productinventory`); depend on `:core:domain`, `:core:ui`, `:core:common`; add `libs.coil.compose` for thumbnails
- [x] 4.3 Register `include(":features:product-inventory")` in the root `settings.gradle.kts`
- [x] 4.4 Create `features/product-inventory/src/main/AndroidManifest.xml` (`<manifest />`)

## 5. Feature MVI contract & ViewModel (`:features:product-inventory`)

- [x] 5.1 Add `InventoryUiState` (`products`/visible list, `categories` incl. synthetic `Todas`, `selectedCategoryId`, `selectedAvailability`, `searchQuery`, `productCount`, `isLoading`, `errorMessage`) using `kotlinx.immutable` lists
- [x] 5.2 Add `InventoryUiEvent` (`Load`, `Retry`, `SelectCategory`, `SelectAvailability`, `ClearAvailability`, `Search`, `RegisterProductClicked`)
- [x] 5.3 Add `InventoryUiEffect` (`ShowError`) over `SharedFlow`
- [x] 5.4 Add `InventoryViewModel` (`@HiltViewModel`): load categories + products on init, re-query products on category/availability change, filter loaded products by name on search, recompute count; map failures to error state + effect
- [x] 5.5 Map `Availability` → localized labels via `strings.xml` (Disponible / Stock bajo / Consultar)

## 6. Feature UI (`:features:product-inventory`)

- [x] 6.1 Add `InventoryHeader` (title + notification icon) and `InventorySearchBar` (reuse `QhathiaTextField`)
- [x] 6.2 Add `CategoryFilterRow` (horizontal scrollable chips, `Todas` first, active state)
- [x] 6.3 Add `ProductCountAndFilterBar` (count label + availability filter control)
- [x] 6.4 Add `ProductRow` (Coil thumbnail, name, formatted price, mocked "N stock" indicator via a single `mockedStock(...)` helper, chevron) and a `LazyColumn` with stable `key = { it.id }`
- [x] 6.5 Add registration `FloatingActionButton` (inert; emits `RegisterProductClicked`)
- [x] 6.6 Add `InventoryContent` (stateless, handles loading/empty/error/list states) and `InventoryScreen` (stateful: `collectAsStateWithLifecycle`, snackbar effect handling) following `LoginScreen`/`LoginContent`
- [x] 6.7 Add `@Preview` (light + dark) for `InventoryContent` with sample data, empty, and error states
- [x] 6.8 Add `inventoryGraph(...)` + route constant in a `navigation/InventoryNavigation.kt`, mirroring `authGraph`

## 7. App shell wiring (`:app`)

- [x] 7.1 Add `implementation(project(":features:product-inventory"))` to `app/build.gradle.kts`
- [x] 7.2 Add a bottom-navigation `Scaffold` (Inicio · Inventario · Solicitudes · Dispositivos · Perfil) hosting the post-login destinations; mark Inventario active on the inventory route
- [x] 7.3 Extend `QhathiaNavHost`: post-login start destination = inventory route via `inventoryGraph(...)`; Inicio → `HomePlaceholderScreen`; Solicitudes/Dispositivos/Perfil → lightweight placeholders
- [x] 7.4 Verify login success navigates into the bottom-nav shell with Inventario selected

## 8. Tests & Definition of Done

- [x] 8.1 Add `FakeProductRepository` / `FakeCategoryRepository` to `:core:testing` and product/category fixtures in `TestFixtures`
- [x] 8.2 Unit-test `ProductRepositoryImpl` / `CategoryRepositoryImpl` mapping (success, HttpError, NetworkError) like `AuthRepositoryImplTest`
- [x] 8.3 Unit-test `InventoryViewModel` (Turbine): load success/empty/error, category filter re-query, availability sub-filter combine, search filtering, count updates
- [x] 8.4 Run `./gradlew :features:product-inventory:test :core:data:test`, `./gradlew lint`, and `./gradlew assembleDebug`
- [x] 8.5 Confirm Definition of Done: unit tests pass, lint clean, `InventoryContent` has working light + dark `@Preview`
