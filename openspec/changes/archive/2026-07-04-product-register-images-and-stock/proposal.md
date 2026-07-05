## Why

The `2026-06-29-add-product-registration` change shipped against an API that had no stock field and no server-side image upload, so it worked around both: it captured product quantity but never sent it, and it uploaded photos to Cloudflare R2 **directly from the app** using credentials baked into the APK (a known security trade-off). The API contract has since advanced (Fases 11–12): `POST /api/v1/products` now accepts a numeric `stock` field with automatic `stock ↔ availability` sync, and a new atomic multipart endpoint `POST /api/v1/products/with-images` lets the backend receive the photos, upload them to R2 itself, and create the product + media rows in one transaction. This change realigns the registration flow with the new contract — closing the stock gap and removing app-side R2 credentials entirely.

## What Changes

- **Register through the atomic multipart endpoint.** Replace the three-call orchestration (`POST /api/v1/products` → R2 `PUT` → `POST /api/v1/products/{id}/media`) with a single `POST /api/v1/products/with-images` multipart request: a `product` JSON part plus one `images` part per photo. The backend uploads to R2 and creates product + media atomically (full rollback on any failure), so the client no longer needs a create-then-attach sequence or a best-effort deactivate rollback.
- **Send product quantity as `stock`.** The captured, validated quantity is now transmitted in the `product` part as `stock`. `availability` is no longer hard-coded to `AVAILABLE` — it is omitted so the backend derives it from stock (`0 → OUT_OF_STOCK`, `>0 → AVAILABLE`) per the contract's sync rule.
- **BREAKING (internal): remove app-side Cloudflare R2 upload.** Delete `R2StorageService`, `AwsV4Signer`, `R2Config`, the `@R2Client` Ktor client, the R2 Hilt providers, and the `R2_*` `local.properties`/`BuildConfig` credentials. The app never touches the bucket again — resolving the prior change's "credentials ship inside the APK" open question.
- **Keep on-device image compression**, now feeding the multipart parts (instead of an R2 PUT), staying well under the endpoint's 5 MB-per-image / 60 MB-total limits.
- **Remap registration errors** to the new endpoint's statuses: plan limit is **402** (`PLAN_LIMIT_EXCEEDED`) not 403; `400 MULTIPART_INVALID` / `422 VALIDATION_ERROR` → invalid input; `503 STORAGE_UNAVAILABLE` → a transient/storage failure kept on the form.
- **Authentication: no change required.** The contract diff makes no changes to the auth/token endpoints (login, refresh, switch-store); the existing `AuthApiService`/`AuthDtos` already match. Verified as part of this change; no code change.

**Out of scope:** read-side `stock`/`OUT_OF_STOCK` surfacing in the inventory list (the list still uses its mocked stock indicator); adding stock to `GET /api/v1/products` mapping; the `draft-from-image` / `regenerate-description` AI endpoints; attaching images to existing products (`POST /api/v1/products/{id}/images`).

## Capabilities

### New Capabilities

_None._

### Modified Capabilities

- `product-registration`: Product quantity is now **sent** to the backend as `stock` (the previous "not transmitted" behavior is reversed). Image handling changes from **client-side upload to Cloudflare R2** to **submitting photos to the atomic multipart create endpoint**, with the backend performing the upload; the create-then-attach-media orchestration and its rollback are replaced by a single atomic call. Plan-limit handling keys off HTTP 402 (the new endpoint's status) rather than 403.

## Impact

- **`:core:network`** — `ProductApiService` gains `createProductWithImages(...)` (multipart) and drops `createProduct`/`attachMedia`/`deactivateProduct`; `CreateProductRequestDto` gains `stock` and drops the hard-coded `availability`; `CreateMediaRequestDto`/`MediaResponseDto` removed. **Removed:** `R2Config`, `storage/R2StorageService`, `storage/AwsV4Signer`, `@R2Client` + `KtorClientFactory.createR2()`, and the R2 Hilt providers in `NetworkModule`.
- **`:core:domain`** — `ProductRepository` collapses `createProduct`/`attachMedia`/`deactivateProduct` into `registerProduct(NewProduct)`; `RegisterProductUseCase` becomes a thin delegate (no orchestration/rollback). **Removed:** `MediaStorageRepository` port. `NewProduct.quantity` is unchanged but now flows through to the request.
- **`:core:data`** — `ProductRepositoryImpl` compresses each image (reusing `ImageCompressor`) and calls the multipart service; `NewProduct.toCreateRequestDto()` now maps `quantity → stock`. **Removed:** `MediaStorageRepositoryImpl` and its `DataModule` binding.
- **`:app`** — remove `R2_*` `buildConfigField`s and `local.properties` reads from `app/build.gradle.kts`; remove `provideR2Config` from `AppModule`.
- **`:core:testing`** — remove `FakeMediaStorageRepository`; update `FakeProductRepository` to the new single-method port.
- **Presentation (`:features:product-register`)** — unchanged in behavior (it already captures quantity and depends only on `RegisterProductUseCase`); only affected if error copy is retuned.
- **APIs consumed:** `POST /api/v1/products/with-images` (new), `GET /api/v1/categories` (unchanged). No longer consumed: `POST /api/v1/products`, `POST /api/v1/products/{id}/media`, `DELETE /api/v1/products/{id}`, R2 object `PUT`.
- **Tests:** `RegisterProductUseCaseTest`, `ProductRepositoryImplTest`, `ProductMappersTest`, `AwsV4SignerTest` (deleted) updated/removed accordingly.
