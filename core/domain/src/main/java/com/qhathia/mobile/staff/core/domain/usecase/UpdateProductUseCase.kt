package com.qhathia.mobile.staff.core.domain.usecase

import com.qhathia.mobile.staff.core.domain.model.ProductDetail
import com.qhathia.mobile.staff.core.domain.model.ProductUpdate
import com.qhathia.mobile.staff.core.domain.model.UpdateProductError
import com.qhathia.mobile.staff.core.domain.model.UpdateProductException
import com.qhathia.mobile.staff.core.domain.repository.ProductRepository
import javax.inject.Inject

/**
 * Saves the edited product and returns it as the server stored it (with its new `version`). The
 * update is partial: see [ProductUpdate]. Any failure that is not already an [UpdateProductException] is normalized to
 * one so the presentation layer can always branch on the cause.
 */
class UpdateProductUseCase @Inject constructor(
    private val repository: ProductRepository,
) {

    suspend operator fun invoke(productId: String, product: ProductUpdate): Result<ProductDetail> =
        repository.updateProduct(productId, product)
            .recoverCatching { throw it.asUpdateException() }

    private fun Throwable.asUpdateException(): UpdateProductException =
        this as? UpdateProductException
            ?: UpdateProductException(UpdateProductError.Unknown(message))
}
