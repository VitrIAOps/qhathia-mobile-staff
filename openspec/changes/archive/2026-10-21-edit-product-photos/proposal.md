## Why

A merchant can only choose a product's photos when registering it. On the edit screen they are read-only, so a blurry photo, a product that changed packaging, or a missing angle means deleting and re-registering the product. The backend already supports both operations (`POST /api/v1/products/{id}/images`, `DELETE /api/v1/products/{id}/media/{mediaId}`). The 2026-10-18 triage listed this as an incomplete screen.

Product decisions (2026-10-21):
- **Saving:** photo changes are part of the form. They are applied with **Guardar** and discarded with **Cancelar**, like the discount.
- **Minimum:** a product always keeps **at least one photo**, like registration.

## What Changes

- The edit screen's photo strip becomes editable. An `Agregar` tile opens the same camera / gallery choice as registration, and each photo gets an ✕ to remove it.
- Changes stay local until `Guardar`. Adding or removing a photo marks the form as changed, so the discard guard covers it.
- **Limits:**
  - A product holds at most **10 photos** (the backend limit). The `Agregar` tile is disabled at 10, and picking more than fit keeps only what fits, with a message.
  - With one photo left, its ✕ is not offered, and a hint explains that the product needs at least one photo.
  - Only JPG and PNG are accepted, as in registration.
- **Saving:** after the product update succeeds, the app first deletes the removed photos, then uploads the new ones in one request, then applies the discount change.
- **If the photo step fails:** the product is already saved, so the merchant stays on the form with a message. The photos are re-read from the server, and whatever did not go through stays pending, so `Guardar` retries it.
- New photos are added at the end. The main photo does not change unless it is removed.
- The camera / gallery strip moves to `core:ui` as a shared component. Registration uses it with no visible change.

## Capabilities

### New Capabilities

_None._

### Modified Capabilities

- `product-detail`:
  - "Product editing" no longer says photos are read-only.
  - Adds a requirement for editing photos on the form, and one for persisting them on save.
  - The discard guard covers photo changes.

## Impact

- **`core:domain`**:
  - `ProductDetail.imageUrls` becomes `images: List<ProductImage(id, url)>`, since the id is needed to delete.
  - New `ProductRepository.addProductImages` and `deleteProductImage`.
  - New `UpdateProductPhotosUseCase`.
- **`core:data`**: the mapper keeps the media id. The repository compresses and uploads images (reusing `ImageCompressor`) and deletes media.
- **`core:network`**: `ProductApiService.addProductImages` (multipart `images`) and `deleteProductMedia`.
- **`core:ui`**: `QhathiaPhotoStrip` (add tile, thumbnails with ✕, camera / gallery sheet) and its strings. Adds the `activity-compose` and `core-ktx` dependencies.
- **`core:testing`**: fake and fixture updates.
- **`features:product-register`**: `ImagePickerSection` delegates to `QhathiaPhotoStrip`. Its photo strings move to `core:ui`.
- **`features:product-detail`**: form state, events, ViewModel save flow, photo section UI, preview mapper, strings (es/en) and tests.
- **Not in scope:**
  - Reordering photos or choosing the main photo. There is no endpoint for it.
  - Videos.
