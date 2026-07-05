## 1. Network layer — products, tags, media (`:core:network`)

- [x] 1.1 Add request/response DTOs in `dto/ProductDtos.kt`: `CreateProductRequestDto` (`name`, `description?`, `price: Double`, `availability: String = "AVAILABLE"`, `allowDiscountInquiry: Boolean = false`, `categoryId: String?`, `tags: List<String>`, `position: Int = 0`), `ProductResponseDto` (at least `id`), `CreateMediaRequestDto` (`url`, `mediaType: String = "IMAGE"`, `position: Int`, `primary: Boolean`) and `MediaResponseDto` — all `@Serializable`
- [x] 1.2 Add `dto/TagDtos.kt`: `TagSuggestionsResponseDto(shoppingCenterId, storeCategory, tags: List<TagDto>)` and `TagDto(id, value, label, description?, group?, position, active)` — `@Serializable`, nullable where the API allows
- [x] 1.3 Extend `ProductApiService` with `suspend fun createProduct(body: CreateProductRequestDto): NetworkResult<ProductResponseDto>` (`client.post("api/v1/products") { setBody(...) }`), `suspend fun attachMedia(productId: String, body: CreateMediaRequestDto): NetworkResult<MediaResponseDto>` (`POST api/v1/products/{id}/media`), and `suspend fun deactivateProduct(productId: String): NetworkResult<Unit>` (`DELETE api/v1/products/{id}`), each wrapped in `runCatching { ... }.toNetworkResult()`
- [x] 1.4 Add `TagApiService` with `suspend fun getTags(): NetworkResult<TagSuggestionsResponseDto>` (`GET api/v1/tags/suggestions`)
- [x] 1.5 Provide `TagApiService` in `NetworkModule` over the existing `@AuthenticatedClient`

## 2. Network layer — Cloudflare R2 upload (`:core:network`)

- [x] 2.1 Add `R2Config(endpoint, bucket, region, accessKeyId, secretAccessKey, publicBaseUrl)` (plain data class, mirroring `NetworkConfig`)
- [x] 2.2 Add `@R2Client` qualifier in `di/NetworkQualifiers.kt`
- [x] 2.3 Add `AwsV4Signer` (computes the SigV4 `Authorization` header + `x-amz-date`/`x-amz-content-sha256` for a single `PUT`); keep it self-contained and unit-testable
- [x] 2.4 Extend `KtorClientFactory` (or add a small factory) to build the R2 `HttpClient` (Ktor CIO, `expectSuccess = true`, longer socket/request timeout, no bearer Auth plugin) pointed at `R2Config.endpoint`
- [x] 2.5 Add `R2StorageService` with `suspend fun upload(key: String, bytes: ByteArray, contentType: String): NetworkResult<String>` — signed `PUT` of the object, returning `"${publicBaseUrl}/$key"`
- [x] 2.6 Provide `@R2Client HttpClient` and `R2StorageService` in `NetworkModule` (consuming the `R2Config` provided by `:app`)

## 3. Domain layer (`:core:domain`)

- [x] 3.1 Add `model/Tag.kt` (`value`, `label`, `group: String?`)
- [x] 3.2 Add `model/SelectedImage.kt` (`uri: String`, `mimeType: String`) and `model/NewProduct.kt` (`name`, `price: Double`, `quantity: Int`, `description: String?`, `categoryId: String`, `tags: List<String>`, `images: List<SelectedImage>`) — `quantity` is held but not yet mapped to the API
- [x] 3.3 Add `model/RegisterProductError.kt` (sealed: `PlanLimitReached`, `InvalidInput`, `ImageTooLargeOrInvalid`, `Network`, `Unknown(message)`) + `RegisterProductException(error)`, mirroring `LoginError`/`LoginException`
- [x] 3.4 Add `repository/TagRepository.kt` (`suspend fun getTags(): Result<List<Tag>>`)
- [x] 3.5 Add `repository/MediaStorageRepository.kt` (`suspend fun uploadImage(image: SelectedImage): Result<String>` — compresses + uploads, returns public URL)
- [x] 3.6 Extend `repository/ProductRepository.kt` with `suspend fun createProduct(product: NewProduct): Result<String>` (returns product id), `suspend fun attachMedia(productId: String, url: String, position: Int, primary: Boolean): Result<Unit>`, and `suspend fun deactivateProduct(productId: String): Result<Unit>`
- [x] 3.7 Add `usecase/GetTagsUseCase.kt`
- [x] 3.8 Add `usecase/RegisterProductUseCase.kt`: compress+upload each image via `MediaStorageRepository`, `createProduct` first (map create HTTP 403 → `PlanLimitReached`), then attach media (first = `primary`); on any post-create failure call `deactivateProduct` (best-effort) and return `Result.failure(RegisterProductException(...))`

## 4. Data layer (`:core:data`)

- [x] 4.1 Extend `mapper/ProductMappers.kt` with `NewProduct.toCreateRequestDto()` (intentionally omit `quantity` — add a `// TODO: send once the API supports a quantity/stock field` marker) and `ProductResponseDto.toProductId()`; add `mapper/TagMappers.kt` (`TagDto.toDomain()`)
- [x] 4.2 Add `ImageCompressor` (Android): decode the picked `Uri` via `ContentResolver`, reject non-JPEG/PNG, downscale + re-encode JPEG with descending quality until ≤ 500 KB, return `ByteArray` + content type; surface an `ImageTooLargeOrInvalid`-style failure when impossible
- [x] 4.3 Extend `ProductRepositoryImpl` with `createProduct` / `attachMedia` / `deactivateProduct`, mapping `NetworkResult → Result` (HTTP 403 on create → `RegisterProductException(PlanLimitReached)`; others → typed failures) like `AuthRepositoryImpl`
- [x] 4.4 Add `TagRepositoryImpl` (same mapping pattern; map `TagSuggestionsResponseDto.tags`)
- [x] 4.5 Add `MediaStorageRepositoryImpl` delegating to `ImageCompressor` + `R2StorageService` (compress → `upload` → return URL), generating an object key `products/<uuid>.<ext>`
- [x] 4.6 Bind `TagRepository` and `MediaStorageRepository` in `DataModule` (`@Binds @Singleton`)

## 5. App configuration — R2 credentials & config (`:app`)

- [x] 5.1 In `app/build.gradle.kts`, read `R2_ENDPOINT`, `R2_BUCKET`, `R2_REGION`, `R2_ACCESS_KEY_ID`, `R2_SECRET_ACCESS_KEY`, `R2_PUBLIC_BASE_URL` from `local.properties` (defaulting to empty) and emit them as `buildConfigField`s
- [x] 5.2 Document the R2 keys (e.g. in `local.properties` example / `docs/android/android-development-guide.md`); confirm `local.properties` is git-ignored
- [x] 5.3 In `AppModule`, add `@Provides @Singleton fun provideR2Config(): R2Config` built from `BuildConfig`, mirroring `provideNetworkConfig()`

## 6. Create the feature module (`:features:product-register`)

- [x] 6.1 Create `features/product-register/src/main/java/com/qhathia/mobile/staff/feature/productregister/` (and matching `src/test/...`)
- [x] 6.2 Write `features/product-register/build.gradle.kts` from the Feature module template (namespace `com.qhathia.mobile.staff.feature.productregister`); depend on `:core:domain`, `:core:ui`, `:core:common`; add `libs.androidx.activity.compose` (Photo Picker) and `libs.coil.compose` (previews)
- [x] 6.3 Register `include(":features:product-register")` in the root `settings.gradle.kts`
- [x] 6.4 Create `features/product-register/src/main/AndroidManifest.xml` (`<manifest />`)

## 7. Feature MVI contract & ViewModel (`:features:product-register`)

- [x] 7.1 Add `ProductRegisterUiState` (name, price text, quantity text, description, `categories`, `selectedCategoryId`, `tags` grouped, `selectedTags`, `images`, per-field error flags, `isSubmitEnabled`, `isSubmitting`, `isDirty`, loading/error flags for categories+tags) using `kotlinx.immutable` lists
- [x] 7.2 Add `ProductRegisterUiEvent` (`NameChanged`, `PriceChanged`, `QuantityChanged`, `DescriptionChanged`, `SelectCategory`, `ToggleTag`, `ImagesPicked`, `RemoveImage`, `Submit`, `CancelRequested`, `ConfirmDiscard`, `DismissDiscard`, `RetryCatalog`)
- [x] 7.3 Add `ProductRegisterUiEffect` (`NavigateBackWithSuccess`, `NavigateBackWithPlanLimit`, `ShowError(messageRes/text)`, `ShowDiscardDialog`) over `SharedFlow`
- [x] 7.4 Add field validators in `:core:common` (next to `EmailValidator`): name non-blank ≤ 200, price > 0 with ≤ 2 decimals (PEN parse), quantity parses to a non-negative integer (reject negatives), with unit tests
- [x] 7.5 Add `ProductRegisterViewModel` (`@HiltViewModel`): load categories + tags on init; validate per field on change; derive `isSubmitEnabled`; on `Submit` set `isSubmitting`, call `RegisterProductUseCase`, branch results to success / plan-limit / error effects; track `isDirty` and gate cancel/back through the discard dialog

## 8. Feature UI (`:features:product-register`)

- [x] 8.1 Add `strings.xml` (labels, validation messages, discard dialog copy, success/plan-limit/error snackbar text — Spanish)
- [x] 8.2 Add form composables: name/description fields (reuse `QhathiaTextField`), price field with a PEN currency `VisualTransformation` (S/ prefix, two decimals), and a quantity field (numeric keyboard, integer ≥ 0 with inline validation error)
- [x] 8.3 Add `CategorySelector` (chips/dropdown, single required selection, loading/retry state) and `TagSelector` (chips grouped by `group`, multi-select, ≥1 required, loading/retry state)
- [x] 8.4 Add `ImagePickerSection`: launch the Android Photo Picker (`ActivityResultContracts.PickVisualMedia`, image-only), preview selected images with Coil, allow removal, mark the first as primary
- [x] 8.5 Add a circular loader overlay while `isSubmitting`, and a discard-confirmation `AlertDialog` plus a `BackHandler` gating back navigation when `isDirty`
- [x] 8.6 Add `ProductRegisterContent` (stateless: form + validation messages + loader + dialog) and `ProductRegisterScreen` (stateful: `collectAsStateWithLifecycle`, effect handling) following `LoginScreen`/`LoginContent`
- [x] 8.7 Add `@Preview` (light + dark) for `ProductRegisterContent`: empty, filled-valid, validation-error and submitting states
- [x] 8.8 Add `navigation/ProductRegisterNavigation.kt` with `PRODUCT_REGISTER_ROUTE` and `fun NavGraphBuilder.productRegisterGraph(onRegistered, onPlanLimit, onCancelled)`, mirroring `inventoryGraph`

## 9. Inventory & app shell wiring (`:app` + `:features:product-inventory`)

- [x] 9.1 In `:features:product-inventory`, make `InventoryUiEvent.RegisterProductClicked` emit a navigation `InventoryUiEffect` (e.g. `NavigateToRegister`) instead of a no-op; expose an inventory refresh trigger and a success/plan-limit snackbar hook on the screen
- [x] 9.2 Add `implementation(project(":features:product-register"))` to `app/build.gradle.kts`
- [x] 9.3 Wire navigation in `MainShell`/`QhathiaNavHost`: inventory FAB → `productRegisterGraph`; on success pop back to inventory setting a `SavedStateHandle` result; on plan-limit pop back with a plan-limit result
- [x] 9.4 In the inventory screen, observe the `SavedStateHandle` result on resume to refresh the list and show the green success snackbar (or the plan-limit message)

## 10. Tests & Definition of Done

- [x] 10.1 Add `FakeTagRepository` and `FakeMediaStorageRepository` to `:core:testing`, extend `FakeProductRepository` with create/attach/deactivate, and add tag/new-product fixtures in `TestFixtures`
- [x] 10.2 Unit-test `AwsV4Signer` against known SigV4 test vectors
- [x] 10.3 Unit-test `RegisterProductUseCase`: happy path (create → upload → attach, first primary), plan-limit (create 403 → `PlanLimitReached`, no upload), media failure after create triggers `deactivateProduct` rollback
- [x] 10.4 Unit-test the name/price/quantity validators (incl. negative quantity rejected) and `ProductRepositoryImpl`/`TagRepositoryImpl` mapping (success, HttpError incl. 403, NetworkError); assert `NewProduct.toCreateRequestDto()` omits `quantity`
- [x] 10.5 Unit-test `ProductRegisterViewModel` (Turbine): per-field validation + submit gate (incl. negative quantity blocking submit), submit success effect, plan-limit effect, generic error stays on form, discard-guard flow, catalog load/retry
- [x] 10.6 Run `./gradlew :features:product-register:test :core:data:test :core:network:test :core:common:test`, `./gradlew lint`, and `./gradlew assembleDebug`
- [x] 10.7 Confirm Definition of Done: unit tests pass, lint clean, `ProductRegisterContent` has working light + dark `@Preview`
