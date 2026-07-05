## 1. Network layer — multipart create endpoint (`:core:network`)

- [x] 1.1 In `dto/ProductDtos.kt`, add `stock: Int` to `CreateProductRequestDto` and remove the hard-coded `availability` field (omit it from the payload so the backend derives availability from stock).
- [x] 1.2 In `dto/ProductDtos.kt`, remove `CreateMediaRequestDto` and `MediaResponseDto` (media is no longer attached in a separate call). Keep `ProductResponseDto` (its `id` is the created product id).
- [x] 1.3 Add a `MultipartImage(bytes: ByteArray, contentType: String, filename: String)` transport-input type in `:core:network` (e.g. `dto/MultipartImage.kt`).
- [x] 1.4 In `service/ProductApiService.kt`, add `createProductWithImages(product: CreateProductRequestDto, images: List<MultipartImage>): NetworkResult<ProductResponseDto>` that POSTs `api/v1/products/with-images` as `MultiPartFormDataContent`: a `product` part with the JSON-serialized body and `Content-Type: application/json`, plus one `images` part per photo with its `Content-Type` and `filename`. Serialize the product part with the injected `Json`.
- [x] 1.5 In `service/ProductApiService.kt`, remove `createProduct`, `attachMedia`, and `deactivateProduct`.
- [x] 1.6 Inject `Json` into `ProductApiService` (constructor) and update the `provideProductApiService` provider in `di/NetworkModule.kt` accordingly.

## 2. Remove Cloudflare R2 from the network layer (`:core:network`)

- [x] 2.1 Delete `storage/R2StorageService.kt`, `storage/AwsV4Signer.kt`, and `R2Config.kt`.
- [x] 2.2 In `KtorClientFactory.kt`, remove `createR2()` (and any R2-only config/timeouts it introduced).
- [x] 2.3 In `di/NetworkQualifiers.kt`, remove the `@R2Client` qualifier.
- [x] 2.4 In `di/NetworkModule.kt`, remove `provideR2Client`, `provideAwsV4Signer`, and `provideR2StorageService`, and their now-unused imports.
- [x] 2.5 Delete the test `storage/AwsV4SignerTest.kt`.

## 3. Domain layer (`:core:domain`)

- [x] 3.1 In `repository/ProductRepository.kt`, replace `createProduct`, `attachMedia`, and `deactivateProduct` with a single `suspend fun registerProduct(product: NewProduct): Result<String>` (returns the created product id; failures carry a `RegisterProductException`). Keep `getProducts`.
- [x] 3.2 Delete `repository/MediaStorageRepository.kt`.
- [x] 3.3 Simplify `usecase/RegisterProductUseCase.kt` to delegate to `productRepository.registerProduct(product)` — remove the image loop, the `MediaStorageRepository` dependency, and the `rollback`/`deactivateProduct` logic. Keep normalization of any non-`RegisterProductException` failure to `RegisterProductException(Unknown)`.
- [x] 3.4 Update the KDoc on `model/NewProduct.kt` to state that `quantity` is now sent as `stock` (remove the "not yet sent" note).

## 4. Data layer (`:core:data`)

- [x] 4.1 In `mapper/ProductMappers.kt`, update `NewProduct.toCreateRequestDto()` to map `quantity → stock` and remove the `// TODO: send quantity` marker and the `availability` default.
- [x] 4.2 In `repository/ProductRepositoryImpl.kt`, implement `registerProduct(product)`: for each `SelectedImage` call `imageCompressor.compress(...)` (short-circuit to failure on a compression error), map each `CompressedImage` to a `MultipartImage` (filenames `image_0.jpg`, `image_1.jpg`, …), then call `apiService.createProductWithImages(product.toCreateRequestDto(), images)`; map `NetworkResult` → `Result<String>`.
- [x] 4.3 In `repository/ProductRepositoryImpl.kt`, inject `ImageCompressor`; remove `createProduct`, `attachMedia`, `deactivateProduct` and update the HTTP-status mapping: `402`/`403 → PlanLimitReached`, `400`/`422 → InvalidInput`, `503 → Network`, `NetworkError → Network`, else `Unknown`.
- [x] 4.4 Delete `repository/MediaStorageRepositoryImpl.kt` and remove its binding/provider from `di/DataModule.kt`.
- [x] 4.5 Confirm `ImageCompressor` is unchanged and still produces `image/jpeg` bytes ≤ 500 KB (no change expected; verify it is provided to `ProductRepositoryImpl`).

## 5. App shell (`:app`)

- [x] 5.1 In `app/build.gradle.kts`, remove the six `R2_*` `buildConfigField`s and their `localProp("R2_*")` reads (and the `localProp` helper if it becomes unused).
- [x] 5.2 In `di/AppModule.kt`, remove `provideR2Config` and the `R2Config`/`BuildConfig.R2_*` imports/usages.
- [x] 5.3 Remove the documented `R2_*` keys from the `local.properties` template/docs if present.

## 6. Test doubles and unit tests (`:core:testing` + module tests)

- [x] 6.1 Delete `core/testing/FakeMediaStorageRepository.kt`.
- [x] 6.2 Update `core/testing/FakeProductRepository.kt` to the new `ProductRepository` port (single `registerProduct`), keeping configurable success/failure for tests.
- [x] 6.3 Update `RegisterProductUseCaseTest` to cover: successful delegation returns the product id; a repository failure propagates as `RegisterProductException`. Remove rollback/upload-orchestration assertions.
- [x] 6.4 Update `ProductRepositoryImplTest` to cover: `stock` is sent and `availability` is omitted; images are compressed and passed as multipart; status mapping (402/403 → plan limit, 400/422 → invalid input, 503 → network); compression failure short-circuits before the network call.
- [x] 6.5 Update `ProductMappersTest` to assert `toCreateRequestDto()` maps `quantity → stock` and sets no `availability`.
- [x] 6.6 Update `AuthApiServiceTest` / auth tests only if touched — expected no change (auth contract unchanged; verify green).

## 7. Verification

- [x] 7.1 Build the affected modules: `./gradlew :core:network:assemble :core:domain:assemble :core:data:assemble :app:assembleDebug`.
- [x] 7.2 Run unit tests: `./gradlew :core:network:test :core:domain:test :core:data:test :features:product-register:test`.
- [x] 7.3 Run `./gradlew lint` and confirm no new warnings from the removed R2 code (no dangling references, no unused `local.properties` keys).
- [x] 7.4 Manually verify (or via test) the registration happy path issues exactly one `POST /api/v1/products/with-images` request with a `product` JSON part carrying `stock` and one `images` part per photo, and that a plan-limit (402) response returns to inventory with the plan-exceeded message.
