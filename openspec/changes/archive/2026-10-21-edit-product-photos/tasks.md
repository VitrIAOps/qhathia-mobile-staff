## 1. Network, domain and data

- [x] 1.1 `core:network` — `ProductApiService.addProductImages(id, images: List<MultipartImage>)` (multipart `images`, `201`) and `deleteProductMedia(id, mediaId)` (`204`)
- [x] 1.2 `core:domain` — `ProductImage(id, url)`. `ProductDetail.imageUrls` → `images` (design Decision 1). Update the usages and `TestFixtures`
- [x] 1.3 `core:domain` — `ProductRepository.addProductImages` / `deleteProductImage`, and `UpdateProductPhotosUseCase` (deletes first, then one upload; a `404` on a delete counts as deleted; stops at the first other failure; sends nothing when there is nothing to do)
- [x] 1.4 `core:data` — the mapper keeps the media id. `ProductRepositoryImpl` compresses off the main thread and uploads, and deletes. Errors go through `toUpdateError()`
- [x] 1.5 `core:testing` — `FakeProductRepository` records added images and deleted media ids, and has configurable results
- [x] 1.6 Tests:
  - mapper keeps ids and order
  - repository upload and delete: success and HTTP errors
  - use case: order of calls, stopping on failure, no-op

## 2. Shared photo strip (`core:ui`)

- [x] 2.1 `QhathiaPhotoStrip`, `PhotoTile` and `PickedPhoto`, moved from `ImagePickerSection` (add tile, ✕ thumbnails, camera / gallery sheet, `FileProvider` uri), with `canAdd` / `canRemove` / `enabled` (design Decision 4). Add `activity-compose` and `core-ktx` to `core:ui`
- [x] 2.2 Move the photo strings to `core:ui` (`photo_add`, `photo_add_cd`, `photo_remove_cd`, `photo_source_title`, `photo_source_camera`, `photo_source_gallery`; es/en). Remove them from `product-register`
- [x] 2.3 `features:product-register` — `ImagePickerSection` keeps its title and subtitle and delegates to `QhathiaPhotoStrip`, with no behaviour change
- [x] 2.4 Previews: strip empty, with photos, at the limit, with one photo; light and dark

## 3. Edit screen

- [x] 3.1 `FormPhoto` (`Saved` / `Pending`). In `ProductDetailUiState`: `photos`, `loadedImages`, `removedImageIds`, `addedImages`, `isPhotosChanged`, `canAddPhotos`, `canRemovePhotos` (design Decision 2)
- [x] 3.2 Events `PhotosPicked(List<SelectedImage>)` and `PhotoRemoved(key)`. The ViewModel filters JPG/PNG, caps at 10 with a message, ignores duplicates, refuses to remove the last photo, and sets `isDirty`
- [x] 3.3 Save flow (design Decision 3): `PUT` → `UpdateProductPhotosUseCase` → discount. On a photo failure, show the message, re-read the product, rebuild the strip, keep the pending photos, and stay dirty. On a discount failure after the photos succeeded, also re-read the photos
- [x] 3.4 UI: replace `ProductImagesRow` with the strip and its hints ("Máximo 10 fotos", "El producto necesita al menos una foto"). Keep the empty placeholder with the `Agregar` tile. Disable the strip while saving or deleting
- [x] 3.5 `toPreviewData` uses the form's photos (URLs and uris)
- [x] 3.6 Strings (es/en): hints, `detail_photos_limit_reached` (some photos not added), `detail_photos_unsupported`, `detail_error_photos_save`
- [x] 3.7 ViewModel tests:
  - pick adds as pending and marks dirty; unsupported and over-limit picks; duplicates
  - remove; the last photo cannot be removed
  - save sends nothing for photos when they are unchanged
  - save order is product → deletes → upload → discount
  - a product failure sends no photo request
  - an upload failure keeps pending photos, drops deleted ones and stays dirty; a retry uploads only what is left
  - the discard guard covers photo changes

## 4. Verification

- [x] 4.1 `./gradlew :core:domain:test :core:data:testDebugUnitTest :core:ui:lintDebug :features:product-detail:testDebugUnitTest :features:product-detail:lintDebug :features:product-register:testDebugUnitTest :features:product-register:lintDebug`
- [x] 4.2 Full build: `./gradlew testDebugUnitTest lintDebug assembleDebug`

## QA (manual, run by QA)

- Edit screen:
  - Add photos from the camera and from the gallery, remove one, and save. Back in Inventario and on the tablet, the product shows the new set
  - Cancel after changing photos → the discard dialog appears, and the photos are untouched
  - With 10 photos, `Agregar` is disabled. With 1 photo, there is no ✕ and the hint is shown
  - Remove the main photo and save → the product still shows a photo in Inventario
  - Airplane mode on save → "Se guardó el producto, pero no se pudieron actualizar las fotos". Saving again online completes the change
- Registration: the photo picker looks and works as before
- Light and dark, large font
