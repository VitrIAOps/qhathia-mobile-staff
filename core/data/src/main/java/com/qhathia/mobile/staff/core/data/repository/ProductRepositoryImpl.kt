package com.qhathia.mobile.staff.core.data.repository

import com.qhathia.mobile.staff.core.data.local.ImageCompressor
import com.qhathia.mobile.staff.core.data.mapper.toCreateRequestDto
import com.qhathia.mobile.staff.core.data.mapper.toDomain
import com.qhathia.mobile.staff.core.data.mapper.toProductId
import com.qhathia.mobile.staff.core.data.mapper.toUpdateRequestDto
import com.qhathia.mobile.staff.core.domain.model.Availability
import com.qhathia.mobile.staff.core.domain.model.NewProduct
import com.qhathia.mobile.staff.core.domain.model.Product
import com.qhathia.mobile.staff.core.domain.model.ProductDetail
import com.qhathia.mobile.staff.core.domain.model.ProductUpdate
import com.qhathia.mobile.staff.core.domain.model.RegisterProductError
import com.qhathia.mobile.staff.core.domain.model.RegisterProductException
import com.qhathia.mobile.staff.core.domain.model.UpdateProductError
import com.qhathia.mobile.staff.core.domain.model.UpdateProductException
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
 * (402/403 → plan limit, 400/422 → invalid input, 503 → transient/storage). Detail reads and edits
 * fail with an [UpdateProductException] instead (404 → not found, 400/422 → invalid input).
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

    override suspend fun getProduct(id: String): Result<ProductDetail> =
        when (val result = apiService.getProduct(id)) {
            is NetworkResult.Success ->
                Result.success(result.data.toDomain())
            is NetworkResult.HttpError ->
                Result.failure(UpdateProductException(result.code.toUpdateError()))
            is NetworkResult.NetworkError ->
                Result.failure(UpdateProductException(UpdateProductError.Network))
        }

    override suspend fun updateProduct(id: String, product: ProductUpdate): Result<Unit> =
        when (val result = apiService.updateProduct(id, product.toUpdateRequestDto())) {
            is NetworkResult.Success ->
                Result.success(Unit)
            is NetworkResult.HttpError ->
                Result.failure(UpdateProductException(result.code.toUpdateError()))
            is NetworkResult.NetworkError ->
                Result.failure(UpdateProductException(UpdateProductError.Network))
        }

    private fun Int.toRegisterError(): RegisterProductError = when (this) {
        402, 403 -> RegisterProductError.PlanLimitReached
        400, 422 -> RegisterProductError.InvalidInput
        503 -> RegisterProductError.Network
        else -> RegisterProductError.Unknown("HTTP $this")
    }

    private fun Int.toUpdateError(): UpdateProductError = when (this) {
        404 -> UpdateProductError.NotFound
        400, 422 -> UpdateProductError.InvalidInput
        503 -> UpdateProductError.Network
        else -> UpdateProductError.Unknown("HTTP $this")
    }
}
