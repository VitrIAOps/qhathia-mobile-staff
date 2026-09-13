package com.qhathia.mobile.staff.core.network.service

import com.qhathia.mobile.staff.core.network.NetworkResult
import com.qhathia.mobile.staff.core.network.dto.RestockDto
import com.qhathia.mobile.staff.core.network.dto.SalesDashboardDto
import com.qhathia.mobile.staff.core.network.toNetworkResult
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

/**
 * Wraps the store dashboard API (biz). Runs on the authenticated client; the endpoints accept
 * OWNER, SELLER and ADMIN tokens. Optional params are only sent when non-null, so the backend
 * defaults apply otherwise.
 */
class DashboardApiService(private val client: HttpClient) {

    /** Sales series for [granularity] (`MONTHLY`, `WEEKLY`, `DAILY`, `HOURLY`), [buckets] 1..48. */
    suspend fun getSales(granularity: String, buckets: Int?): NetworkResult<SalesDashboardDto> =
        runCatching {
            client.get("api/v1/dashboard/sales") {
                parameter("granularity", granularity)
                buckets?.let { parameter("buckets", it) }
            }.body<SalesDashboardDto>()
        }.toNetworkResult()

    /** Products below the restock threshold, [limit] 1..50. */
    suspend fun getRestock(limit: Int?): NetworkResult<RestockDto> =
        runCatching {
            client.get("api/v1/dashboard/restock") {
                limit?.let { parameter("limit", it) }
            }.body<RestockDto>()
        }.toNetworkResult()
}
