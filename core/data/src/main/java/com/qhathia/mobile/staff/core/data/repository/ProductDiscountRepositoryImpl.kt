package com.qhathia.mobile.staff.core.data.repository

import com.qhathia.mobile.staff.core.data.mapper.toCreateRequestDto
import com.qhathia.mobile.staff.core.data.mapper.toDomainOrNull
import com.qhathia.mobile.staff.core.domain.model.Discount
import com.qhathia.mobile.staff.core.domain.model.DiscountError
import com.qhathia.mobile.staff.core.domain.model.DiscountException
import com.qhathia.mobile.staff.core.domain.model.ProductDiscount
import com.qhathia.mobile.staff.core.domain.repository.ProductDiscountRepository
import com.qhathia.mobile.staff.core.network.NetworkResult
import com.qhathia.mobile.staff.core.network.service.ProductApiService
import java.time.Instant
import javax.inject.Inject

/**
 * Maps the discount calls of [ProductApiService] to domain [Result]s. Failures carry a
 * [DiscountException] whose [DiscountError] is derived from the HTTP status (404 → not found,
 * 400/422 → invalid input, 503 or a transport failure → network).
 */
class ProductDiscountRepositoryImpl @Inject constructor(
    private val apiService: ProductApiService,
) : ProductDiscountRepository {

    override suspend fun getDiscounts(productId: String): Result<List<ProductDiscount>> =
        apiService.getDiscounts(productId).toResult { dtos -> dtos.mapNotNull { it.toDomainOrNull() } }

    override suspend fun createDiscount(
        productId: String,
        discount: Discount,
        startsAt: Instant,
        endsAt: Instant,
    ): Result<ProductDiscount> =
        when (
            val result = apiService.createDiscount(productId, discount.toCreateRequestDto(startsAt, endsAt))
        ) {
            is NetworkResult.Success ->
                // The request is the source of truth if the echoed row cannot be parsed.
                Result.success(
                    result.data.toDomainOrNull()
                        ?: ProductDiscount(result.data.id, discount, startsAt, endsAt, active = true),
                )
            is NetworkResult.HttpError -> Result.failure(DiscountException(result.code.toDiscountError()))
            is NetworkResult.NetworkError -> Result.failure(DiscountException(DiscountError.Network))
        }

    override suspend fun deleteDiscount(productId: String, discountId: String): Result<Unit> =
        apiService.deleteDiscount(productId, discountId).toResult { }

    private inline fun <T, R> NetworkResult<T>.toResult(transform: (T) -> R): Result<R> = when (this) {
        is NetworkResult.Success -> Result.success(transform(data))
        is NetworkResult.HttpError -> Result.failure(DiscountException(code.toDiscountError()))
        is NetworkResult.NetworkError -> Result.failure(DiscountException(DiscountError.Network))
    }

    private fun Int.toDiscountError(): DiscountError = when (this) {
        404 -> DiscountError.NotFound
        400, 422 -> DiscountError.InvalidInput
        503 -> DiscountError.Network
        else -> DiscountError.Unknown("HTTP $this")
    }
}
