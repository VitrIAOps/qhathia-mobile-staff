## Why

The Inventory screen ships with a registration FAB, but it is inert — a store owner cannot yet add a product to their catalog. Registration is the first write operation in the staff app and the natural next step after read-only inventory: without it the merchant has no way to grow the catalog that powers the kiosk's conversational search. This change delivers the product-registration flow end to end (form → image upload → API persistence → return to inventory).

## What Changes

- Introduce a **Product Registration ("Registro de producto") screen** reached from the inventory FAB: a form capturing name, price (PEN), product quantity (stock), optional description, category, tags and at least one photo.
- Enforce the **business validation rules**: name required, price required and greater than zero, quantity required and not negative (a negative quantity blocks registration), at least one category, at least one tag and at least one image (JPG/PNG only). Validation runs per-field as the merchant edits, and the submit action is gated on a fully valid form.
- Capture **product quantity** in the form and domain model, but **do not send it to the backend yet** — `POST /api/v1/products` has no quantity/stock field today. The field is collected and validated client-side now; wiring it to the API is a follow-up pending a backend conversation. New products still default to `availability = AVAILABLE`.
- Add **category selection** (reusing `GET /api/v1/categories`) and **free-text tag entry** (comma-separated, per the Figma mockup; at least one tag required).
- Add **image selection** from the **device camera or the photo gallery**, restricted to JPG/PNG, with on-device **compression to ≤ 500 KB** preserving quality before upload.
- Add **Cloudflare R2 image upload**: each selected image is uploaded to R2 (S3-compatible, AWS SigV4-signed PUT) and the resulting public URL is used to register media. R2 credentials live in `local.properties` and are surfaced through `BuildConfig`, never committed.
- Orchestrate the **registration transaction**: `POST /api/v1/products` to create the product, then upload each image to R2 and attach it via `POST /api/v1/products/{id}/media` (first image marked `primary`). A best-effort rollback (soft-delete) runs if media attachment fails after the product was created.
- Add **user feedback**: a circular loader while registering; on success, navigate back to the inventory list, refresh it, and show a green success snackbar; on error, stay on the form and show an error snackbar with the reason — **except** a plan-limit error (HTTP 403 on create), which returns the merchant to inventory with a distinct plan-exceeded message.
- Add an **unsaved-changes guard**: backing out or cancelling a dirty form shows a confirmation dialog warning that changes will be discarded.
- Extend the network/domain/data layers with the create-product, attach-media, tag-catalog and R2-upload contracts, reusing the existing authenticated Ktor client and `NetworkResult → Result` convention.

**Out of scope (per the task):** AI description generation, AI image enhancement, and video media (images only).

## Capabilities

### New Capabilities
- `product-registration`: The registration form and its validation, category/tag selection, image picking + compression, Cloudflare R2 upload, the create-product/attach-media orchestration with rollback, success/error feedback, and the unsaved-changes guard.

### Modified Capabilities
- `product-inventory`: The registration FAB now navigates to the registration screen (previously inert), and after a successful registration the inventory list refreshes and shows a success snackbar.

## Impact

- **New Gradle module:** `:features:product-register` (form screen, ViewModel, MVI contract, image picker, nav graph).
- **`:core:network`:** new `TagApiService` + `TagDtos`; `ProductApiService` gains `createProduct` (`POST /api/v1/products`) and `attachMedia` (`POST /api/v1/products/{id}/media`) plus a `deactivateProduct` rollback call; a dedicated, AWS-SigV4-signed R2 `HttpClient` + `R2StorageService` and `R2Config`.
- **`:core:domain`:** new `Tag`, `NewProduct` (incl. a `quantity` field), `SelectedImage`, `RegisterProductError` models; `MediaStorageRepository` and `TagRepository` ports; `ProductRepository` gains create/attach/deactivate; new `RegisterProductUseCase` (orchestration) and `GetTagsUseCase`. `quantity` is held on `NewProduct` but intentionally omitted from the create-product DTO until the API supports it.
- **`:core:data`:** new `TagRepositoryImpl`, `MediaStorageRepositoryImpl` (delegates to the R2 service), `ProductRepositoryImpl` create/attach/rollback mapping, an Android `ImageCompressor`, and DTO↔domain mappers; bound in `DataModule`.
- **`:app`:** depends on the new feature module; `R2Config` provided from `BuildConfig`; `app/build.gradle.kts` reads R2 credentials from `local.properties`; the post-login `NavHost` wires inventory → registration and back-with-result.
- **APIs consumed:** `GET /api/v1/categories`, `POST /api/v1/products`, `POST /api/v1/products/{id}/media`, `DELETE /api/v1/products/{id}` (rollback), and R2 object `PUT` (S3 API). All app APIs require the Bearer JWT already injected by the authenticated client. (Tags are entered free-text per the mockup, so `GET /api/v1/tags/suggestions` is not consumed by the form; the tag-catalog repository remains available for future autocomplete.)
- **Permissions:** none new — the Android Photo Picker needs no storage permission, and camera capture goes through the system camera app (`TakePicture` + a `FileProvider` temp URI), so no `CAMERA` permission is declared or requested. A `FileProvider` is added to the app manifest. `INTERNET` already declared.
- **Security note:** embedding R2 credentials in the app (even via `local.properties`/`BuildConfig`) ships them inside the APK. This honors the task's explicit instruction but is a known trade-off; a backend-issued presigned-URL flow is the recommended hardening and is captured as an open question in design.md.
