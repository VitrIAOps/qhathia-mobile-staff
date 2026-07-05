## 1. Network layer (`core:network`)

- [x] 1.1 In `dto/ProductDtos.kt`, add `@Serializable DraftFromImageResponseDto(description: String, categoryId: String?, tags: List<String>, source: String)` with `@SerialName` bindings for `description`, `categoryId`, `tags`, `source`.
- [x] 1.2 In `dto/ProductDtos.kt`, add `@Serializable RegenerateDescriptionRequestDto(currentDescription: String)` and `@Serializable RegenerateDescriptionResponseDto(description: String)`.
- [x] 1.3 In `service/ProductApiService.kt`, add `suspend fun draftFromImage(image: MultipartImage): NetworkResult<DraftFromImageResponseDto>` posting multipart to `api/v1/products/draft-from-image` with a single `image` part (reuse the `formData`/`MultiPartFormDataContent` pattern from `createProductWithImages`), wrapped in `runCatching { … }.toNetworkResult()`.
- [x] 1.4 In `service/ProductApiService.kt`, add `suspend fun regenerateDescription(body: RegenerateDescriptionRequestDto): NetworkResult<RegenerateDescriptionResponseDto>` posting JSON to `api/v1/products/regenerate-description`, wrapped in `runCatching { … }.toNetworkResult()`.

## 2. Domain layer (`core:domain`)

- [x] 2.1 Add domain model `ProductDraft(description: String, categoryId: String?, tags: List<String>)` under `core/domain/.../model/`.
- [x] 2.2 Add repository port `ProductAiRepository` under `core/domain/.../repository/` with `suspend fun draftFromImage(image: SelectedImage): Result<ProductDraft>` and `suspend fun optimizeDescription(currentDescription: String): Result<String>`.
- [x] 2.3 Add `DraftProductFromImageUseCase @Inject constructor(repository: ProductAiRepository)` with `suspend operator fun invoke(image: SelectedImage): Result<ProductDraft>`.
- [x] 2.4 Add `OptimizeDescriptionUseCase @Inject constructor(repository: ProductAiRepository)` with `suspend operator fun invoke(currentDescription: String): Result<String>`.

## 3. Data layer (`core:data`)

- [x] 3.1 Add DTO→domain mapper `DraftFromImageResponseDto.toDomain(): ProductDraft` in `mapper/ProductMappers.kt`.
- [x] 3.2 Add `ProductAiRepositoryImpl @Inject constructor(apiService: ProductApiService, imageCompressor: ImageCompressor)` implementing `ProductAiRepository`; run on `Dispatchers.IO`, compress the `SelectedImage` via `ImageCompressor`, wrap as `MultipartImage`, call `draftFromImage`, and map `NetworkResult → Result` (Success→`toDomain()`; HttpError/NetworkError→`Result.failure`).
- [x] 3.3 Implement `optimizeDescription` in the impl: call `apiService.regenerateDescription(RegenerateDescriptionRequestDto(currentDescription))` and map `NetworkResult → Result<String>` returning `data.description`.

## 4. Dependency injection wiring

- [x] 4.1 In `core:data`'s `DataModule.kt`, add a `@Binds`/`@Provides` for `ProductAiRepository` → `ProductAiRepositoryImpl` (match the existing `ProductRepository` binding style).
- [x] 4.2 Confirm `ProductApiService` (authenticated client) is already provided and reused; add no new client — no changes expected beyond verifying the binding resolves.

## 5. Presentation layer (`features:product-register`)

- [x] 5.1 Extend `ProductRegisterUiState` with `isAiLoading: Boolean = false` and `showAiReplaceDialog: Boolean = false`; add a derived `isAiActionEnabled` (`images.isNotEmpty() && !isAiLoading && !isSubmitting`) and a derived `isOptimizeMode` (`description`, `selectedCategoryId` and `parseTags(tags)` all non-empty).
- [x] 5.2 Extend `ProductRegisterUiEvent` with `ConfirmAiReplace` and `DismissAiReplace` (keep the existing `GenerateDescriptionWithAi`).
- [x] 5.3 In `ProductRegisterViewModel`, inject `DraftProductFromImageUseCase` and `OptimizeDescriptionUseCase`.
- [x] 5.4 Replace the inert `GenerateDescriptionWithAi` handler: if `isOptimizeMode` → run optimize; else if description/tags/category has any data → set `showAiReplaceDialog = true`; else run drafting.
- [x] 5.5 Handle `ConfirmAiReplace` (clear dialog flag, run drafting) and `DismissAiReplace` (clear dialog flag only).
- [x] 5.6 Implement drafting: set `isAiLoading = true`, call `DraftProductFromImageUseCase` on the first image; on success apply description, apply `categoryId` only if it matches a loaded category id (else leave selection), and set `tags` from the returned list joined by `", "`; treat an empty draft as a no-op; set `isAiLoading = false`. On failure emit `ShowError` and clear loading.
- [x] 5.7 Implement optimize: set `isAiLoading = true`, call `OptimizeDescriptionUseCase` with the current description; on success replace only `description`; on failure emit `ShowError`; clear loading. Mark AI-driven field changes as `isDirty = true`.
- [x] 5.8 In `ProductRegisterContent`/form composables, make the AI button label switch between "Generate with AI" and "Optimize with AI" from `isOptimizeMode`, show a loading indicator and disable it from `isAiActionEnabled`/`isAiLoading`.
- [x] 5.9 Add the replace-confirmation dialog composable driven by `showAiReplaceDialog`, wired to `ConfirmAiReplace`/`DismissAiReplace` (mirror the existing discard dialog).
- [x] 5.10 Add the required string resources: generate label, optimize label, replace-warning title/body/confirm/cancel, and AI error message.

## 6. Tests & Definition of Done

- [x] 6.1 Add a `FakeProductAiRepository` in `core:testing` for use case and ViewModel tests.
- [x] 6.2 Unit-test `ProductAiRepositoryImpl` mapping: success→`ProductDraft`/description, HttpError and NetworkError→`Result.failure`; verify `ImageCompressor` is invoked for drafting.
- [x] 6.3 Unit-test `ProductRegisterViewModel`: generate populates fields, unmatched category is not applied, empty draft is a no-op, replace-dialog shows only when fields have data and confirm/dismiss behave, optimize replaces only the description, and a network failure emits `ShowError` without mutating fields.
- [x] 6.4 Add/refresh a `@Preview` (light + dark) covering the AI loading state and the replace dialog.
- [x] 6.5 Run `./gradlew test` and `./gradlew lint` and confirm both pass.
