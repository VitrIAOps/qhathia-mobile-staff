## Context

- **Edit screen today:** `ProductDetailUiState.imageUrls` is a plain URL list drawn by a read-only `ProductImagesRow`. The domain `ProductDetail` drops the media id in the mapper, so nothing can be deleted.
- **Save flow:** `PUT /products/{id}` (a full replace), then the discount change. If the discount fails after the product saved, the screen stays dirty and shows "product saved, but…".
- **Registration:** its `ImagePickerSection` already has the `Agregar` tile, the ✕ thumbnails and the camera / gallery sheet (`FileProvider` at `${applicationId}.fileprovider`). It only knows local `SelectedImage`s.
- **Backend:**
  - `POST /products/{id}/images` is multipart `images` with 1..N parts (5 MB each, 60 MB per request). It appends with `primary=false`. A total over the limit of 10 is a `400`. On `503` it rolls back.
  - `DELETE /products/{id}/media/{mediaId}` answers `204`.
  - There is no endpoint to reorder photos or change the main one.

## Goals / Non-Goals

**Goals:**
- Add and remove photos on the edit form, applied atomically from the merchant's point of view: on `Guardar`, or never.
- One photo picker in the app, shared by registration and editing.
- Never exceed 10 photos, and never drop below 1.

**Non-Goals:**
- Reordering photos, choosing the main photo, or videos.

## Decisions

### 1. Domain keeps the media id

- `ProductDetail.images: List<ProductImage>`, where `ProductImage(id, url)` keeps today's order (primary first, then by position).
- **Repository:**
  - `addProductImages(productId, images: List<SelectedImage>): Result<Unit>` compresses each image off the main thread with `ImageCompressor`, as registration does, and sends a single multipart request.
  - `deleteProductImage(productId, mediaId): Result<Unit>`.
  - Failures carry `UpdateProductException`, using the existing `toUpdateError()` mapping.
- **`UpdateProductPhotosUseCase(productId, removedIds, added)`:**
  - Deletes first, one request per id, so the server never goes over 10 in between. Then it uploads all additions in one request.
  - A photo that is already gone (`404`) counts as deleted, so a retry after a half-finished save does not fail on it.
  - It stops at the first other failure. With nothing to do, it sends nothing.

### 2. Form model

```kotlin
sealed interface FormPhoto { val key: String
    data class Saved(val image: ProductImage) : FormPhoto      // key = media id
    data class Pending(val image: SelectedImage) : FormPhoto   // key = content uri
}
```

- `photos: ImmutableList<FormPhoto>` is what the strip shows. `loadedImages` is what the server has.
- `removedImageIds` = loaded ids no longer present in `photos`. `addedImages` = the `Pending` photos.
  - `isPhotosChanged` is true when either is non-empty.
- **Adding:**
  - Only JPG / PNG are kept.
  - At most `MAX_PHOTOS - photos.size` are added, and the rest are dropped with a message.
  - Duplicates (the same uri) are ignored.
- **Removing:** ignored when only one photo is left.
- Any change sets `isDirty`, so `isSaveEnabled` and the discard guard pick it up with no new code.
- **Alternative rejected:** uploading on pick and deleting on ✕. That breaks `Cancelar`, and an upload abandoned by `Cancelar` would leave orphan photos (product decision: save on `Guardar`).

### 3. Save order and partial failure

`PUT product` → photos → discount.

- **If the product update fails:** nothing else is sent, as today.
- **If the photos fail:**
  - Stay on the form with "Se guardó el producto, pero no se pudieron actualizar las fotos".
  - Re-read the product (`GET /products/{id}`) and rebuild the strip:
    - Saved photos are the server's list, minus the ids the merchant had removed that still exist there.
    - Every `Pending` photo is kept. The upload is a single all-or-nothing request, so none of them landed.
  - The form stays dirty, so `Guardar` retries only what is left.
  - The discount is not attempted, so one message covers one failure.
- **If the photos succeed but the discount fails:** follow today's discount path. The photos are re-read as well, so the strip shows the saved photos as `Saved` and a retry does not upload them twice.
- **Alternative rejected:** uploading photos before the `PUT`. A photo would then be saved even if the product's own fields failed validation on the server.

### 4. Shared `QhathiaPhotoStrip` in `core:ui`

- **Inputs:**
  - `photos: ImmutableList<PhotoTile(key, model: Any)>`, where `model` is a URL or a content uri, both understood by Coil.
  - `onPick: (List<PickedPhoto(uri, mimeType)>) -> Unit` and `onRemove(key)`.
  - `canAdd`, `canRemove` and `enabled`.
- It holds the camera / gallery sheet, the `TakePicture` and `PickMultipleVisualMedia` launchers and the `FileProvider` uri. These move unchanged from `ImagePickerSection`.
- `core:ui` stays free of `core:domain`. Callers map `PickedPhoto` ↔ `SelectedImage`.
- Registration's `ImagePickerSection` keeps its title and subtitle and delegates to the strip. Its photo strings move to `core:ui` with neutral names (`photo_add`, `photo_add_cd`, `photo_remove_cd`, `photo_source_title`, `photo_source_camera`, `photo_source_gallery`).
- **Edit screen:**
  - With 10 photos, `canAdd = false` and a hint reads "Máximo 10 fotos".
  - With one photo, `canRemove = false` and a hint reads "El producto necesita al menos una foto".
  - A legacy product with no photos keeps the empty placeholder, plus the `Agregar` tile.

## Risks / Trade-offs

- **[Risk] Removing the main photo:** the contract does not say which photo becomes main. → Inventario reloads after a save and shows whatever the server picked. QA checks that some photo is shown.
- **[Risk] A partial upload despite the rollback promise.** → After any photo failure the strip is rebuilt from the server, so the worst case is a duplicate the merchant can remove. It is never a lost photo.
- **[Trade-off] One request per removed photo.** There is no bulk delete, and the count is at most 9.
- **[Trade-off] The legacy zero-photo product:** it can save its other fields without adding a photo. The minimum only stops removing the last one.

## Open Questions

_None._ Both product questions were answered on 2026-10-21.
