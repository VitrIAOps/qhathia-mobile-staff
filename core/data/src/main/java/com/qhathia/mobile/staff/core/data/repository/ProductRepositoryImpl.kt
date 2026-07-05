package com.qhathia.mobile.staff.core.data.repository

import com.qhathia.mobile.staff.core.data.local.ImageCompressor
import com.qhathia.mobile.staff.core.data.mapper.toCreateRequestDto
import com.qhathia.mobile.staff.core.data.mapper.toDomain
import com.qhathia.mobile.staff.core.data.mapper.toProductId
import com.qhathia.mobile.staff.core.domain.model.Availability
import com.qhathia.mobile.staff.core.domain.model.NewProduct
import com.qhathia.mobile.staff.core.domain.model.Product
import com.qhathia.mobile.staff.core.domain.model.RegisterProductError
import com.qhathia.mobile.staff.core.domain.model.RegisterProductException
import com.qhathia.mobile.staff.core.domain.repository.ProductRepository
import com.qhathia.mobile.staff.core.network.NetworkResult
import com.qhathia.mobile.staff.core.network.dto.MultipartImage
import com.qhathia.mobile.staff.core.network.service.ProductApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Maps transport [NetworkResult]s to domain [Result]s at the data boundary, so the domain and
 * presentation layers never see Ktor or DTO types. Registration compresses each image off the main
 * thread, then submits the product and its photos in a single atomic multipart request; failures
 * carry a [RegisterProductException] whose [RegisterProductError] is derived from the HTTP status
 * (402/403 → plan limit, 400/422 → invalid input, 503 → transient/storage).
 */
class ProductRepositoryImpl @Inject constructor(
    private val apiService: ProductApiService,
    private val imageCompressor: ImageCompressor,
) : ProductRepository {

    override suspend fun getProducts(
        categoryId: String?,
        availability: Availability?,
    ): Result<List<Product>> =
        when (val result = apiService.getProducts(categoryId, availability?.name)) {
            is NetworkResult.Success ->
                Result.success(result.data.map { it.toDomain() })
            is NetworkResult.HttpError ->
                Result.failure(IllegalStateException("HTTP ${result.code}: ${result.message}"))
            is NetworkResult.NetworkError ->
                Result.failure(result.cause)
        }

    override suspend fun registerProduct(product: NewProduct): Result<String> =
        withContext(Dispatchers.IO) {
            val images = product.images.mapIndexed { index, image ->
                val compressed = imageCompressor.compress(image)
                    .getOrElse { return@withContext Result.failure(it) }
                MultipartImage(
                    bytes = compressed.bytes,
                    contentType = compressed.contentType,
                    filename = "image_$index.jpg",
                )
            }

            when (
                val result = apiService.createProductWithImages(product.toCreateRequestDto(), images)
            ) {
                is NetworkResult.Success ->
                    Result.success(result.data.toProductId())
                is NetworkResult.HttpError ->
                    Result.failure(RegisterProductException(result.code.toRegisterError()))
                is NetworkResult.NetworkError ->
                    Result.failure(RegisterProductException(RegisterProductError.Network))
            }
        }

    private fun Int.toRegisterError(): RegisterProductError = when (this) {
        402, 403 -> RegisterProductError.PlanLimitReached
        400, 422 -> RegisterProductError.InvalidInput
        503 -> RegisterProductError.Network
        else -> RegisterProductError.Unknown("HTTP $this")
    }
}
