package com.qhathia.mobile.staff.core.domain.usecase

import com.qhathia.mobile.staff.core.domain.model.SelectedImage
import com.qhathia.mobile.staff.core.domain.model.UpdateProductError
import com.qhathia.mobile.staff.core.domain.model.UpdateProductException
import com.qhathia.mobile.staff.core.domain.repository.ProductRepository
import javax.inject.Inject

/**
 * Applies the photo changes of the edit form. Removed photos are deleted first, one request each, so
 * the product never goes over the backend's per-product limit in between; then every added photo is
 * uploaded in a single request. A photo that is already gone (404) counts as deleted. It stops at
 * the first other failure, normalized to an
 * [UpdateProductException], and sends nothing when there is nothing to change.
 */
class UpdateProductPhotosUseCase @Inject constructor(
    private val repository: ProductRepository,
) {

    suspend operator fun invoke(
        productId: String,
        removedImageIds: List<String>,
        addedImages: List<SelectedImage>,
    ): Result<Unit> {
        for (mediaId in removedImageIds) {
            repository.deleteProductImage(productId, mediaId).onFailure { cause ->
                val error = cause.asUpdateException()
                // Already gone (e.g. removed by an earlier, half-finished save): nothing to undo.
                if (error.error != UpdateProductError.NotFound) return Result.failure(error)
            }
        }
        if (addedImages.isEmpty()) return Result.success(Unit)
        return repository.addProductImages(productId, addedImages)
            .recoverCatching { throw it.asUpdateException() }
    }

    private fun Throwable.asUpdateException(): UpdateProductException =
        this as? UpdateProductException
            ?: UpdateProductException(UpdateProductError.Unknown(message))
}
