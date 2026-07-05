## Context

The product-registration slice (change `2026-06-29-add-product-registration`) is live: a `:features:product-register` module drives a form whose `ProductRegisterViewModel` builds a domain `NewProduct` and calls `RegisterProductUseCase`. That use case orchestrates three network steps against the old contract — `POST /api/v1/products` (create), an AWS-SigV4-signed `PUT` to Cloudflare R2 for each image, then `POST /api/v1/products/{id}/media` — plus a best-effort `DELETE` rollback. Two deliberate compromises were baked in because the API of the day forced them: the captured product **quantity was never sent** (no `stock` field existed), and **R2 credentials were embedded in the APK** via `local.properties` → `BuildConfig`.

The API contract has since moved to Fase 12/13. The relevant additions (per `docs/android/api-contract.md`):
- `POST /api/v1/products` (and the new multipart endpoint) accept a numeric `stock`, with automatic `stock ↔ availability` sync when `availability` is omitted (`0 → OUT_OF_STOCK`, `>0 → AVAILABLE`).
- **`POST /api/v1/products/with-images`** (multipart): a `product` JSON part + 1–10 `images` binary parts (≤ 5 MB each, ≤ 60 MB total). The backend validates, uploads to R2 itself, and creates product + media rows in **one atomic transaction** (total rollback on any failure). The frontend never touches the bucket.

This change is a **refactor of the existing slice** to that contract. It is confined to `:core:network`, `:core:data`, `:core:domain`, `:app` build config, and `:core:testing`; the presentation module is behaviorally untouched (it already captures and validates quantity and depends only on `RegisterProductUseCase`). Constraints from `android-frontend-standards.md` hold: Kotlin + Compose + MVI, Hilt (KSP2), Ktor CIO, the multi-module dependency graph (`features → domain ← data → network`), and the `NetworkResult → Result` boundary convention. No new Gradle module is needed — every layer already exists.

## Goals / Non-Goals

**Goals:**
- Register a product through the single atomic `POST /api/v1/products/with-images` multipart call (product JSON part + one `images` part per photo, first = primary).
- Transmit the captured quantity as `stock`; omit `availability` so the backend derives it.
- Remove all app-side Cloudflare R2 code and credentials (SigV4 signer, R2 client, R2 config, media-storage repository, `R2_*` build config).
- Preserve on-device compression (≤ 500 KB/image) so uploads stay small and safely within the endpoint limits.
- Remap registration errors to the new endpoint's statuses (402 plan limit, 400/422 invalid, 503 storage) without changing the `RegisterProductError` surface the presentation layer branches on.
- Keep the presentation module and its UX (loader, success/plan-limit/discard flows) unchanged.

**Non-Goals:**
- Surfacing real `stock` or the new `OUT_OF_STOCK` state in the inventory list (it keeps its mocked stock indicator); reading `stock` from `GET /api/v1/products`.
- Consuming the AI endpoints (`draft-from-image`, `regenerate-description`) or the "attach images to an existing product" endpoint (`POST /api/v1/products/{id}/images`).
- Any change to auth/token endpoints — the contract diff does not touch them and the current auth code already matches (verified, no-op).
- Editing/deleting products, or multi-store switching.

## Decisions

### Single multipart call replaces create → upload → attach → rollback

`RegisterProductUseCase` collapses to a thin delegate to `ProductRepository.registerProduct(NewProduct): Result<String>`; the per-image loop, the R2 upload step, the media-attach step, and the `deactivateProduct` rollback all disappear because the backend now guarantees atomicity. This mirrors the existing thin use cases (`GetProductsUseCase`) and keeps the domain seam (error normalization to `RegisterProductException`) intact.

The `ProductRepository` port drops `createProduct`, `attachMedia`, and `deactivateProduct` in favor of one `registerProduct`. The `MediaStorageRepository` port and `MediaStorageRepositoryImpl` are deleted — there is no separate upload step to abstract anymore.

_Alternative considered:_ keep the three-method port and just no-op attach/deactivate. Rejected — it leaves dead contract surface and misleading tests; the atomic endpoint is a genuinely simpler shape and the port should reflect it.

### Multipart construction lives in `:core:network`; compression stays in `:core:data`

`ProductApiService.createProductWithImages(product: CreateProductRequestDto, images: List<MultipartImage>)` builds a Ktor `MultiPartFormDataContent` on the `@AuthenticatedClient`: a `product` part carrying the serialized JSON with `Content-Type: application/json`, and one `images` part per photo with `Content-Type: image/jpeg` and a filename. `MultipartImage(bytes, contentType, filename)` is a small transport-input type owned by `:core:network`. This keeps all HTTP/transport knowledge (and the `kotlinx.serialization` encode of the product part) in the network module, consistent with the existing split.

Android bitmap work stays in `:core:data`: `ProductRepositoryImpl` reuses the existing `ImageCompressor` to turn each `SelectedImage` into `CompressedImage(bytes, "image/jpeg")`, maps those to `MultipartImage`s (filenames `image_0.jpg`, `image_1.jpg`, …), maps `NewProduct → CreateProductRequestDto` (now with `stock`), calls the service, and maps the result. A compression failure short-circuits to `RegisterProductError.ImageTooLargeOrInvalid` before any network call — same behavior as today, just relocated from `MediaStorageRepositoryImpl` into the repository's `registerProduct`.

_Alternative considered:_ compress inside the network service. Rejected — `:core:network` must stay free of Android `Bitmap`/`ContentResolver` types; compression is correctly an Android-data concern.

### `stock` sent, `availability` omitted

`CreateProductRequestDto` gains `stock: Int` and **drops** the hard-coded `availability = "AVAILABLE"` default (the field is removed from the DTO, not sent). Per the contract, when the caller sends `stock` without `availability` the backend derives it (`0 → OUT_OF_STOCK`, `> 0 → AVAILABLE`). `NewProduct.toCreateRequestDto()` maps `quantity → stock` and drops the `// TODO: send quantity` marker. Because the form already forbids negative quantity, `stock` is always ≥ 0, so the `422 stock negative` path is unreachable from a valid form (still mapped defensively).

_Alternative considered:_ keep sending `availability = AVAILABLE` alongside `stock`. Rejected — the contract says an explicit `availability` wins over derivation, which would make a `stock = 0` product incorrectly `AVAILABLE`. Omitting it is the whole point of "integrate stock".

### Error remapping for the new endpoint

`ProductRepositoryImpl.registerProduct` maps HTTP status → `RegisterProductError`:
- `402` (`PLAN_LIMIT_EXCEEDED`) **and** `403` → `PlanLimitReached` (the new endpoint uses 402 where the old create used 403; keep 403 for safety).
- `400` (`MULTIPART_INVALID`) / `422` (`VALIDATION_ERROR`) → `InvalidInput`.
- `503` (`STORAGE_UNAVAILABLE`) → `Network` (transient; merchant stays on the form and can retry), reusing the existing "kept on the form" generic-failure path.
- `NetworkError` → `Network`; anything else → `Unknown`.

The `RegisterProductError` sealed surface is unchanged, so the ViewModel's branching (plan-limit → return to inventory; everything else → error snackbar on the form) needs no edit.

### Remove R2 end to end

Delete `core/network/R2Config.kt`, `core/network/storage/R2StorageService.kt`, `core/network/storage/AwsV4Signer.kt` (+ `AwsV4SignerTest`), the `@R2Client` qualifier and `KtorClientFactory.createR2()`, and the three R2 `@Provides` in `NetworkModule`. Remove `provideR2Config` from `:app`'s `AppModule` and the six `R2_*` `buildConfigField`s + `local.properties` reads from `app/build.gradle.kts`. Remove `MediaStorageRepositoryImpl` and its `DataModule` binding, plus `FakeMediaStorageRepository`. This directly closes the prior change's headline open question (secrets in the APK).

## Risks / Trade-offs

- **[Larger multipart request on slow networks]** → all images travel in one request now, so a flaky connection fails the whole registration rather than one image. Mitigation: ≤ 500 KB/image compression keeps typical payloads small; the `@AuthenticatedClient` timeouts already cover it; the form stays populated on failure so retry is one tap. (The dedicated long-timeout R2 client is gone, but payloads are far smaller than R2's raw uploads.)
- **[Losing the R2 SigV4 unit tests]** → deleting `AwsV4SignerTest` drops coverage, but the signed-PUT code it guarded is also deleted, so there is nothing left to break.
- **[`stock = 0` ⇒ `OUT_OF_STOCK` and product hidden from catalog]** → a merchant registering with zero stock produces a product that will not appear in the public catalog until restocked. This is the contract's intended semantics; acceptable and arguably desirable. Noted so it is not mistaken for a bug.
- **[Read side still shows mocked stock]** → the inventory list does not yet reflect the real `stock`/`OUT_OF_STOCK` returned by `GET /api/v1/products`, so a just-registered out-of-stock product may look inconsistent there. Deliberately out of scope; tracked as a follow-up.
- **[Multipart `product` part content-type]** → the backend requires the JSON part tagged `application/json`; if omitted it may 400. Mitigation: set the part's `Content-Type` header explicitly and cover it in the API-service test.

## Open Questions

- **Read-side stock** — when should the inventory list consume the real `stock` from `GET /api/v1/products` and render `OUT_OF_STOCK`, replacing the mocked indicator? Recommended as the immediate follow-up.
- **Retry affordance for 503** — is a transient `STORAGE_UNAVAILABLE` worth a dedicated "try again" message versus the generic error snackbar? Treated as generic for now.
