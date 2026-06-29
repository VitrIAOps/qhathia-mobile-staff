package com.qhathia.mobile.staff.core.domain.usecase

import com.qhathia.mobile.staff.core.domain.model.NewProduct
import com.qhathia.mobile.staff.core.domain.model.RegisterProductError
import com.qhathia.mobile.staff.core.domain.model.RegisterProductException
import com.qhathia.mobile.staff.core.domain.repository.MediaStorageRepository
import com.qhathia.mobile.staff.core.domain.repository.ProductRepository
import javax.inject.Inject

/**
 * Orchestrates product registration: create the product first (so a plan-limit rejection surfaces
 * before any upload and leaves no orphaned R2 objects), then upload and attach each image (the first
 * marked primary). If any post-create step fails, the product is rolled back (best-effort soft
 * delete) and the original failure is returned. All failures are normalized to a
 * [RegisterProductException] so the presentation layer can branch on the cause.
 */
class RegisterProductUseCase @Inject constructor(
    private val productRepository: ProductRepository,
    private val mediaStorageRepository: MediaStorageRepository,
) {

    suspend operator fun invoke(product: NewProduct): Result<String> {
        val productId = productRepository.createProduct(product)
            .getOrElse { return Result.failure(it.asRegisterException()) }

        product.images.forEachIndexed { index, image ->
            val url = mediaStorageRepository.uploadImage(image)
                .getOrElse { return rollback(productId, it) }

            productRepository.attachMedia(
                productId = productId,
                url = url,
                position = index,
                primary = index == 0,
            ).getOrElse { return rollback(productId, it) }
        }

        return Result.success(productId)
    }

    private suspend fun rollback(productId: String, cause: Throwable): Result<String> {
        // Best-effort: a product registered without its required image must not be left behind.
        productRepository.deactivateProduct(productId)
        return Result.failure(cause.asRegisterException())
    }

    private fun Throwable.asRegisterException(): RegisterProductException =
        this as? RegisterProductException
            ?: RegisterProductException(RegisterProductError.Unknown(message))
}
