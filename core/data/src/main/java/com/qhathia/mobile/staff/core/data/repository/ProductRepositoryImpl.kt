package com.qhathia.mobile.staff.core.data.repository

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
import com.qhathia.mobile.staff.core.network.dto.CreateMediaRequestDto
import com.qhathia.mobile.staff.core.network.service.ProductApiService
import javax.inject.Inject

/**
 * Maps transport [NetworkResult]s to domain [Result]s at the data boundary, so the domain and
 * presentation layers never see Ktor or DTO types. Write failures carry a [RegisterProductException]
 * whose [RegisterProductError] is derived from the HTTP status (403 → plan limit, 400/422 → invalid).
 */
class ProductRepositoryImpl @Inject constructor(
    private val apiService: ProductApiService,
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

    override suspend fun createProduct(product: NewProduct): Result<String> =
        when (val result = apiService.createProduct(product.toCreateRequestDto())) {
            is NetworkResult.Success ->
                Result.success(result.data.toProductId())
            is NetworkResult.HttpError ->
                Result.failure(RegisterProductException(result.code.toCreateError()))
            is NetworkResult.NetworkError ->
                Result.failure(RegisterProductException(RegisterProductError.Network))
        }

    override suspend fun attachMedia(
        productId: String,
        url: String,
        position: Int,
        primary: Boolean,
    ): Result<Unit> =
        when (
            val result = apiService.attachMedia(
                productId,
                CreateMediaRequestDto(url = url, position = position, primary = primary),
            )
        ) {
            is NetworkResult.Success -> Result.success(Unit)
            is NetworkResult.HttpError ->
                Result.failure(RegisterProductException(RegisterProductError.Unknown("HTTP ${result.code}")))
            is NetworkResult.NetworkError ->
                Result.failure(RegisterProductException(RegisterProductError.Network))
        }

    override suspend fun deactivateProduct(productId: String): Result<Unit> =
        when (val result = apiService.deactivateProduct(productId)) {
            is NetworkResult.Success -> Result.success(Unit)
            is NetworkResult.HttpError ->
                Result.failure(RegisterProductException(RegisterProductError.Unknown("HTTP ${result.code}")))
            is NetworkResult.NetworkError ->
                Result.failure(RegisterProductException(RegisterProductError.Network))
        }

    private fun Int.toCreateError(): RegisterProductError = when (this) {
        403 -> RegisterProductError.PlanLimitReached
        400, 422 -> RegisterProductError.InvalidInput
        else -> RegisterProductError.Unknown("HTTP $this")
    }
}
