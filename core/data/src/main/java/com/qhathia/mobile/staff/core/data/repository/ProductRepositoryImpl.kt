package com.qhathia.mobile.staff.core.data.repository

import com.qhathia.mobile.staff.core.data.mapper.toDomain
import com.qhathia.mobile.staff.core.domain.model.Availability
import com.qhathia.mobile.staff.core.domain.model.Product
import com.qhathia.mobile.staff.core.domain.repository.ProductRepository
import com.qhathia.mobile.staff.core.network.NetworkResult
import com.qhathia.mobile.staff.core.network.service.ProductApiService
import javax.inject.Inject

/**
 * Maps transport [NetworkResult]s to domain [Result]s at the data boundary, so the domain and
 * presentation layers never see Ktor or DTO types. Mirrors [AuthRepositoryImpl].
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
}
