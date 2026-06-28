package com.qhathia.mobile.staff.core.data.repository

import com.qhathia.mobile.staff.core.data.mapper.toDomain
import com.qhathia.mobile.staff.core.domain.model.Category
import com.qhathia.mobile.staff.core.domain.repository.CategoryRepository
import com.qhathia.mobile.staff.core.network.NetworkResult
import com.qhathia.mobile.staff.core.network.service.CategoryApiService
import javax.inject.Inject

/** Maps transport [NetworkResult]s to domain [Result]s, mirroring [ProductRepositoryImpl]. */
class CategoryRepositoryImpl @Inject constructor(
    private val apiService: CategoryApiService,
) : CategoryRepository {

    override suspend fun getCategories(): Result<List<Category>> =
        when (val result = apiService.getCategories()) {
            is NetworkResult.Success ->
                Result.success(result.data.map { it.toDomain() })
            is NetworkResult.HttpError ->
                Result.failure(IllegalStateException("HTTP ${result.code}: ${result.message}"))
            is NetworkResult.NetworkError ->
                Result.failure(result.cause)
        }
}
