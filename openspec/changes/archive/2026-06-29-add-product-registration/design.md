## Context

The `add-product-list` change established the authenticated read stack and the inventory shell: a `@AuthenticatedClient` Ktor `HttpClient` with bearer load + refresh, the `NetworkResult → Result` boundary convention, the multi-module Clean Architecture layout (`:core:network|domain|data|ui|common|testing`, `:features:auth`, `:features:product-inventory`), and a single-`NavHost` shell in `:app` with a bottom navigation bar. The inventory screen already renders a registration FAB that emits an **inert** `InventoryUiEvent.RegisterProductClicked`, and `ProductApiService`/`CategoryApiService` already exist.

This change adds the first **write** flow — registering a product — as a new vertical feature slice plus the create/attach/tag/storage contracts in the core layers. It introduces one genuinely new concern the codebase has not handled before: uploading a binary asset to **Cloudflare R2** (S3-compatible object storage) and feeding the resulting URL to the products media API. The UI follows the Figma mockups (`node-id=1255-6379` form, `node-id=1255-3836` supporting states) on the `Qhatia-DS` file.

Constraints: Kotlin + Compose + MVI, Hilt (KSP2), Ktor CIO, min SDK 26; every feature/layer is a proper Gradle module (never a package in `:app`); all app API calls go through the existing authenticated client so token refresh and timeouts are reused, not reimplemented. Prices are PEN with two decimals; images are JPG/PNG only and must reach the media API at ≤ 500 KB.

## Goals / Non-Goals

**Goals:**
- A registration form capturing name, price, optional description, category, tags and one-or-more images, with per-field validation and a submit gated on validity.
- Category selection from `GET /api/v1/categories` (reused) and free-text, comma-separated tag entry (per the Figma mockup; the tag-catalog repository exists but the form does not consume it).
- Image selection from the device camera (via a FileProvider temp URI) or the photo gallery (Android Photo Picker, no storage permission), JPG/PNG only, compressed on-device to ≤ 500 KB preserving quality.
- Upload images to Cloudflare R2 with AWS SigV4-signed PUTs, credentials sourced from `local.properties` via `BuildConfig`.
- Orchestrate create → upload → attach-media with a best-effort rollback, returning to inventory with a success snackbar + list refresh on success.
- Reuse the authenticated client, `NetworkResult` mapping, DS components (`QhathiaTextField`, `QhathiaButton`, `Dimens`, theme) and existing repository/use-case patterns.

**Non-Goals:**
- AI description generation and AI image enhancement (explicitly out per the task).
- Video media — images only.
- Editing or deleting existing products (registration only).
- Creating new categories/tags from the form (selection from existing catalog only); free-text custom tags.
- A backend presigned-URL upload path (recommended hardening, but the task mandates client-side credentials — see Open Questions).

## Decisions

### New feature module `:features:product-register`

Registration is a distinct vertical slice with its own form, image-picking, validation and orchestration, so it gets its own Android-library module (Compose + Hilt), matching the per-feature-module rule and the `feature/product-register` branch. It holds `ProductRegisterScreen` (stateful), `ProductRegisterContent` (stateless), the form composables (text fields, category chips, free-text tag field, photos row, loader, discard dialog), `ProductRegisterViewModel`, the MVI contract (`ProductRegisterUiState` / `UiEvent` / `UiEffect`), and `productRegisterGraph(...)` + route constant. Depends on `:core:domain`, `:core:ui`, `:core:common` (plus `androidx.activity.compose` for the Photo Picker and `coil-compose` for image previews). **Never** on `:core:network`/`:core:data` or on `:features:product-inventory`.

Cross-feature coupling (return to inventory + snackbar + refresh) is handled in `:app` via a navigation result, **not** a module dependency — see "Inventory ↔ registration handoff".

_Alternative considered:_ adding registration into `:features:product-inventory`. Rejected — it would bloat a screen module with an unrelated write flow and image/storage concerns, against the per-feature-module rule.

### R2 upload lives in `:core:network`; compression in `:core:data`

R2 is the only new infrastructure. Rather than add a `:core:storage` module, the S3-compatible transport goes in `:core:network` (the module that already owns every `HttpClient` and all transport knowledge), and the Android bitmap work goes in `:core:data` (an Android library that can touch `ContentResolver`/`Bitmap`). This mirrors the existing split exactly (network owns clients/DTOs, data owns mappers + Android specifics) and adds no new module to the graph.

- `:core:network` gains:
  - `R2Config(endpoint, bucket, region, accessKeyId, secretAccessKey, publicBaseUrl)` — supplied by `:app` from `BuildConfig`, mirroring how `NetworkConfig` is provided in `AppModule`.
  - A **dedicated R2 `HttpClient`** (Ktor CIO, qualified `@R2Client`) — separate from `@AuthenticatedClient`/`@PublicClient` because R2 authenticates with AWS SigV4 headers, not the app's bearer JWT, and points at the R2 endpoint, not the API base URL. It uses a longer socket/request timeout suited to uploads.
  - `R2StorageService.upload(key, bytes, contentType): NetworkResult<String>` — performs a single `PUT <endpoint>/<bucket>/<key>`, signed with a small `AwsV4Signer` (SigV4 for one PUT is a bounded, well-specified algorithm), and returns the public object URL (`<publicBaseUrl>/<key>`).
- `:core:data` gains `ImageCompressor` (decode the picked `Uri`, reject non-JPEG/PNG, downscale + re-encode JPEG with descending quality until ≤ 500 KB while keeping good quality) and `MediaStorageRepositoryImpl` that compresses then calls `R2StorageService`.

_Alternatives considered:_ (a) the `aws-sdk-kotlin` S3 client — correct but multi-MB and brings its own HTTP engine, redundant with the mandated Ktor CIO stack; kept as a fallback if hand-signing proves fragile. (b) A new `:core:storage` module — unnecessary indirection for one service; the existing network/data split already fits.

### Object key layout

Images are stored under `products/<storeOrUuid>/<uuid>.<ext>` to keep a flat, collision-free namespace. Because the product id does not exist until after `POST /products`, the key uses a client-generated UUID (not the product id), so compression/keying is independent of creation order.

### Registration orchestration + rollback (in `RegisterProductUseCase`)

Multi-repository orchestration belongs in a domain use case, not the ViewModel. `RegisterProductUseCase` runs:
1. **Compress** every selected image locally (no network). Fail fast on a non-JPEG/PNG or un-compressible image.
2. `POST /api/v1/products` (name, price, description, `categoryId`, `tags`, `availability=AVAILABLE`) → product id. This surfaces the **plan-limit 403 before any upload**, so a rejected create never leaves orphaned R2 objects.
3. For each compressed image: **upload to R2** → `POST /api/v1/products/{id}/media` with the URL, `mediaType=IMAGE`, ascending `position`, `primary=true` for the first.
4. If any upload/attach step fails after the product was created, **best-effort rollback** via `DELETE /api/v1/products/{id}` (soft-delete) so a photo-less product is not left behind, then return failure.

Errors are mapped to a domain `RegisterProductError` (`PlanLimitReached`, `InvalidInput`, `ImageTooLargeOrInvalid`, `Network`, `Unknown`) carried through `Result.failure` via a `RegisterProductException`, mirroring the existing `LoginError`/`LoginException` pattern, so the ViewModel branches on cause without seeing HTTP codes. `PlanLimitReached` maps from the create call's HTTP 403.

_Alternative considered:_ upload images first, then create the product. Rejected — it uploads bytes to R2 before the plan-limit check, orphaning objects whenever the plan cap is hit.

### Validation: per-field + submit gate

Field rules live in `:core:common` validators (next to `EmailValidator`) so they are unit-testable and reused by the ViewModel: `name` non-blank ≤ 200 chars; `price` parses to a positive `BigDecimal`/`Double` with ≤ 2 decimals; `quantity` parses to a non-negative integer (a negative value is invalid and blocks registration); ≥ 1 category; ≥ 1 tag; ≥ 1 image (JPG/PNG). The ViewModel validates a field on change/blur (task: validate "en cuanto el usuario seleccione un input") and derives `isSubmitEnabled` from the whole form. Price input uses a PEN currency visual transformation (S/ prefix, grouping, two decimals).

### Product quantity captured but not yet transmitted

The form captures a **product quantity (stock)** field. `POST /api/v1/products` exposes no quantity/stock field today (the same gap the `add-product-list` change worked around with a mocked stock indicator), so the value is **collected and validated on the client but deliberately omitted from `CreateProductRequestDto`**. It is carried on the domain `NewProduct` model (with a `// TODO: send once the API supports it` marker in `NewProduct.toCreateRequestDto()`) so that wiring it to the backend later is a one-line mapper change with no form or domain rework. `availability` continues to default to `AVAILABLE`; quantity does not drive availability in this slice. The backend conversation to add the field is tracked as a follow-up (Open Questions).

_Alternative considered:_ withholding the field until the API supports it. Rejected — capturing it now lets the merchant enter stock from day one and keeps the form aligned with the mockup; the only deferred part is the network mapping.

### Feedback, plan-limit special case, and discard guard

- While registering, `ProductRegisterUiState.isSubmitting=true` drives a circular loader and disables inputs.
- **Success:** emit a `NavigateBackWithSuccess` effect; `:app` pops to inventory, which refreshes and shows a green success snackbar (DS success tonal color).
- **Error (generic):** stay on the form, show an error snackbar with the reason.
- **Error (plan exceeded):** per the task, do **not** stay on the form — emit a distinct effect that returns to inventory carrying a plan-exceeded message shown there.
- **Discard guard:** a dirty form intercepts system back / cancel (`BackHandler` + cancel button) and shows a confirmation dialog before discarding.

### Inventory ↔ registration handoff (`:app`)

`InventoryUiEvent.RegisterProductClicked` becomes a navigation effect; `:app` registers a `productRegisterGraph` route and navigates to it from inventory. On success the registration screen sets a result flag on the inventory back-stack entry's `SavedStateHandle`; the inventory screen observes it on resume to trigger a reload and show the success snackbar. This keeps the two feature modules decoupled (wiring only in `:app`), consistent with how `authGraph`/`inventoryGraph` expose graph builders.

### R2 credentials via `local.properties` → `BuildConfig`

`app/build.gradle.kts` reads R2 keys from `local.properties` (kept out of VCS) and emits them as `buildConfigField`s; `AppModule` builds `R2Config` from `BuildConfig`, exactly like `NetworkConfig`. A `local.properties` template / documented keys are added (`R2_ENDPOINT`, `R2_BUCKET`, `R2_ACCESS_KEY_ID`, `R2_SECRET_ACCESS_KEY`, `R2_PUBLIC_BASE_URL`). Missing keys default to empty so CI builds compile.

## Risks / Trade-offs

- **[R2 credentials ship inside the APK]** → `local.properties`/`BuildConfig` only keeps secrets out of source control, not out of the distributed binary; a determined attacker can extract them. Mitigation: scope the R2 key to a write-only bucket prefix; document and recommend migrating to backend-issued presigned URLs (Open Questions). Implemented as the task explicitly requires.
- **[Hand-rolled SigV4 can be subtly wrong]** → signing bugs surface as opaque R2 403s. Mitigation: unit-test `AwsV4Signer` against known AWS test vectors; keep the surface to a single PUT; fall back to `aws-sdk-kotlin` S3 if needed.
- **[Partial registration]** → product created but a later image upload/attach fails. Mitigation: best-effort soft-delete rollback; if the rollback itself fails, surface a clear error and leave the product recoverable from inventory. Documented trade-off: a rolled-back upload may orphan an R2 object (harmless, prefix-cleanable).
- **[Multi-store JWT without an active store]** → create/tag calls may 400/403. Mitigation: treat as the standard error state; the store-switch flow is a separate change.
- **[Upload time on slow networks]** → ≤ 500 KB images keep uploads short; the R2 client uses a longer socket timeout, and the loader communicates progress. No chunked/multipart upload needed at this size.
- **[Plan-limit copy lives across two screens]** → the plan-exceeded message is raised on registration but shown on inventory; tested via the effect contract to avoid a silent drop.

## Open Questions

- **Presigned URLs vs. embedded credentials** — strongly recommended to move R2 auth to a backend-issued presigned PUT so no secrets ship in the APK. Out of scope here (task mandates `local.properties` credentials); flagged for a follow-up once backend can issue them.
- **Multiple images vs. single** — the business rule requires *at least one* photo; the Figma form should confirm the max count and reorder/primary-selection affordances. Design supports N images (first = primary) and can be capped during implementation.
- **Tag source** — confirm whether the form should also offer vision auto-tag (`POST /api/v1/tags/suggest-from-image`); treated as out of scope (AI), manual catalog selection only for now.
- **Availability selector** — the task lists only name/price/photo/tag/category; availability defaults to `AVAILABLE`. Confirm against Figma whether an availability control belongs on the form.
- **Product quantity → backend** — the quantity field is captured and validated client-side but not sent, because `POST /api/v1/products` has no quantity/stock field. Pending a backend conversation to add it (and likely reconcile with the mocked stock indicator from `add-product-list`); once available, map it in `NewProduct.toCreateRequestDto()`.
