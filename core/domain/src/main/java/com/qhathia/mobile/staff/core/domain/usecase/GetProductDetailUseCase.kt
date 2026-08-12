package com.qhathia.mobile.staff.core.domain.usecase

import com.qhathia.mobile.staff.core.domain.model.ProductDetail
import com.qhathia.mobile.staff.core.domain.model.UpdateProductError
import com.qhathia.mobile.staff.core.domain.model.UpdateProductException
import com.qhathia.mobile.staff.core.domain.repository.ProductRepository
import javax.inject.Inject

/**
 * Loads one product for the detail/edit screen. Any failure that is not already an
 * [UpdateProductException] is normalized to one so the presentation layer can always branch on the
 * cause.
 */
class GetProductDetailUseCase @Inject constructor(
    private val repository: ProductRepository,
) {

    suspend operator fun invoke(productId: String): Result<ProductDetail> =
        repository.getProduct(productId)
            .recoverCatching { throw it.asUpdateException() }

    private fun Throwable.asUpdateException(): UpdateProductException =
        this as? UpdateProductException
            ?: UpdateProductException(UpdateProductError.Unknown(message))
}
