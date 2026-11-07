package com.qhathia.mobile.staff.core.network.service

import com.qhathia.mobile.staff.core.network.NetworkResult
import com.qhathia.mobile.staff.core.network.dto.RestockDto
import com.qhathia.mobile.staff.core.network.dto.SalesDashboardDto
import com.qhathia.mobile.staff.core.network.dto.SalesRankingDto
import com.qhathia.mobile.staff.core.network.dto.StoreActivityDto
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

    /**
     * Sales series for [granularity] (`MONTHLY`, `WEEKLY`, `DAILY`, `HOURLY`), [buckets] 1..48. A
     * [categoryId] limits revenue to that category's order lines.
     */
    suspend fun getSales(
        granularity: String,
        buckets: Int?,
        categoryId: String? = null,
    ): NetworkResult<SalesDashboardDto> =
        runCatching {
            client.get("api/v1/dashboard/sales") {
                parameter("granularity", granularity)
                buckets?.let { parameter("buckets", it) }
                categoryId?.let { parameter("categoryId", it) }
            }.body<SalesDashboardDto>()
        }.toNetworkResult()

    /**
     * Top sellers and stalled stock over [granularity], optionally for one category. [limit] caps
     * the top sellers (the backend defaults to 3 and accepts up to 50).
     */
    suspend fun getRanking(
        granularity: String,
        categoryId: String?,
        limit: Int = DEFAULT_RANKING_LIMIT,
    ): NetworkResult<SalesRankingDto> =
        runCatching {
            client.get("api/v1/dashboard/ranking") {
                parameter("granularity", granularity)
                categoryId?.let { parameter("categoryId", it) }
                parameter("limit", limit)
            }.body<SalesRankingDto>()
        }.toNetworkResult()

    /** Tablet sessions and product views over [granularity]. There is no category filter. */
    suspend fun getActivity(granularity: String): NetworkResult<StoreActivityDto> =
        runCatching {
            client.get("api/v1/dashboard/activity") {
                parameter("granularity", granularity)
            }.body<StoreActivityDto>()
        }.toNetworkResult()

    /** Products below the restock threshold, [limit] 1..50. */
    suspend fun getRestock(limit: Int?): NetworkResult<RestockDto> =
        runCatching {
            client.get("api/v1/dashboard/restock") {
                limit?.let { parameter("limit", it) }
            }.body<RestockDto>()
        }.toNetworkResult()

    companion object {
        /** How many top sellers Ranking shows. */
        const val DEFAULT_RANKING_LIMIT = 5
    }
}
