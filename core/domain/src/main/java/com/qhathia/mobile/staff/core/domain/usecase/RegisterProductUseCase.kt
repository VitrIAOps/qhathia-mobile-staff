package com.qhathia.mobile.staff.core.domain.usecase

import com.qhathia.mobile.staff.core.domain.model.NewProduct
import com.qhathia.mobile.staff.core.domain.model.RegisterProductError
import com.qhathia.mobile.staff.core.domain.model.RegisterProductException
import com.qhathia.mobile.staff.core.domain.repository.ProductRepository
import javax.inject.Inject

/**
 * Registers a product. The atomic `POST /api/v1/products/with-images` endpoint creates the product
 * and uploads its images in a single transaction, so this use case simply delegates to the
 * repository. Any failure that is not already a [RegisterProductException] is normalized to one so
 * the presentation layer can always branch on the cause.
 */
class RegisterProductUseCase @Inject constructor(
    private val productRepository: ProductRepository,
) {

    suspend operator fun invoke(product: NewProduct): Result<String> =
        productRepository.registerProduct(product)
            .recoverCatching { throw it.asRegisterException() }

    private fun Throwable.asRegisterException(): RegisterProductException =
        this as? RegisterProductException
            ?: RegisterProductException(RegisterProductError.Unknown(message))
}
