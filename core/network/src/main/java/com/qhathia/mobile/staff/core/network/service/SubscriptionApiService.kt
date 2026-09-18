package com.qhathia.mobile.staff.core.network.service

import com.qhathia.mobile.staff.core.network.NetworkResult
import com.qhathia.mobile.staff.core.network.dto.SubscriptionDto
import com.qhathia.mobile.staff.core.network.toNetworkResult
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get

/**
 * Wraps the subscriptions API. OWNER/ADMIN only; a store without a subscription answers `404`,
 * which callers treat as "no subscription" rather than a failure.
 */
class SubscriptionApiService(private val client: HttpClient) {

    suspend fun getCurrent(): NetworkResult<SubscriptionDto> =
        runCatching {
            client.get("api/v1/subscriptions/current").body<SubscriptionDto>()
        }.toNetworkResult()
}
