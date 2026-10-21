package com.qhathia.mobile.staff.core.domain.usecase

import com.qhathia.mobile.staff.core.domain.model.UpdateProductError
import com.qhathia.mobile.staff.core.domain.model.UpdateProductException
import com.qhathia.mobile.staff.core.domain.repository.ProductRepository
import javax.inject.Inject

/**
 * Deletes a product for good: `DELETE /api/v1/products/{id}` is a soft delete with no reactivate
 * endpoint. Failures share the causes of an edit, so any failure that is not already an
 * [UpdateProductException] is normalized to one and the presentation layer can always branch on it.
 */
class DeleteProductUseCase @Inject constructor(
    private val repository: ProductRepository,
) {

    suspend operator fun invoke(productId: String): Result<Unit> =
        repository.deleteProduct(productId)
            .recoverCatching { throw it.asUpdateException() }

    private fun Throwable.asUpdateException(): UpdateProductException =
        this as? UpdateProductException
            ?: UpdateProductException(UpdateProductError.Unknown(message))
}
