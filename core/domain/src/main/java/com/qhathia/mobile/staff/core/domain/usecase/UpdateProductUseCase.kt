package com.qhathia.mobile.staff.core.domain.usecase

import com.qhathia.mobile.staff.core.domain.model.ProductUpdate
import com.qhathia.mobile.staff.core.domain.model.UpdateProductError
import com.qhathia.mobile.staff.core.domain.model.UpdateProductException
import com.qhathia.mobile.staff.core.domain.repository.ProductRepository
import javax.inject.Inject

/**
 * Saves the edited product. `PUT /api/v1/products/{id}` is a full replace, so [product] carries
 * every editable field. Any failure that is not already an [UpdateProductException] is normalized to
 * one so the presentation layer can always branch on the cause.
 */
class UpdateProductUseCase @Inject constructor(
    private val repository: ProductRepository,
) {

    suspend operator fun invoke(productId: String, product: ProductUpdate): Result<Unit> =
        repository.updateProduct(productId, product)
            .recoverCatching { throw it.asUpdateException() }

    private fun Throwable.asUpdateException(): UpdateProductException =
        this as? UpdateProductException
            ?: UpdateProductException(UpdateProductError.Unknown(message))
}
