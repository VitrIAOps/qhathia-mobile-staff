package com.qhathia.mobile.staff.core.data.repository

import com.qhathia.mobile.staff.core.data.mapper.toDomain
import com.qhathia.mobile.staff.core.domain.model.Subscription
import com.qhathia.mobile.staff.core.domain.repository.SubscriptionRepository
import com.qhathia.mobile.staff.core.network.NetworkResult
import com.qhathia.mobile.staff.core.network.service.SubscriptionApiService
import javax.inject.Inject

/**
 * [SubscriptionRepository] over `GET /subscriptions/current`. A `404` is the backend's way of saying
 * the store has no subscription, so it is a successful `null`, not an error.
 */
class SubscriptionRepositoryImpl @Inject constructor(
    private val apiService: SubscriptionApiService,
) : SubscriptionRepository {

    override suspend fun getCurrent(): Result<Subscription?> {
        val result = apiService.getCurrent()
        if (result is NetworkResult.HttpError && result.code == HTTP_NOT_FOUND) {
            return Result.success(null)
        }
        return result.asDomainResult { it.toDomain() }
    }

    private companion object {
        const val HTTP_NOT_FOUND = 404
    }
}
