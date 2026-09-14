package com.qhathia.mobile.staff.core.network.service

import com.qhathia.mobile.staff.core.network.NetworkResult
import com.qhathia.mobile.staff.core.network.dto.UnmetSearchesDto
import com.qhathia.mobile.staff.core.network.toNetworkResult
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

/**
 * Wraps the kiosk insights API (vo). vo-api is served from the same host as biz, so it shares the
 * authenticated client and base URL. Optional params are only sent when non-null.
 */
class InsightsApiService(private val client: HttpClient) {

    /** Unmet kiosk searches over the last [days] (1..90), at most [limit] groups (1..50). */
    suspend fun getUnmetSearches(days: Int?, limit: Int?): NetworkResult<UnmetSearchesDto> =
        runCatching {
            client.get("api/v1/insights/unmet-searches") {
                days?.let { parameter("days", it) }
                limit?.let { parameter("limit", it) }
            }.body<UnmetSearchesDto>()
        }.toNetworkResult()
}
