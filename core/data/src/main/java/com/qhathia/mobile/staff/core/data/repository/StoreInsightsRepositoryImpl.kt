package com.qhathia.mobile.staff.core.data.repository

import com.qhathia.mobile.staff.core.data.mapper.toMissingProductSuggestions
import com.qhathia.mobile.staff.core.data.mapper.toRestockSuggestions
import com.qhathia.mobile.staff.core.data.mapper.toSalesSummary
import com.qhathia.mobile.staff.core.domain.model.SalesPeriod
import com.qhathia.mobile.staff.core.domain.model.SalesSummary
import com.qhathia.mobile.staff.core.domain.model.StoreSuggestions
import com.qhathia.mobile.staff.core.domain.repository.StoreInsightsRepository
import com.qhathia.mobile.staff.core.network.NetworkResult
import com.qhathia.mobile.staff.core.network.service.DashboardApiService
import com.qhathia.mobile.staff.core.network.service.InsightsApiService
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.time.Instant
import javax.inject.Inject

/**
 * Serves the Inicio summary from the dashboard (biz) and insights (vo) endpoints, mapping transport
 * results to domain [Result]s at the data boundary.
 *
 * Every period asks for the same [CHART_BUCKETS], because the card is designed for that many bars —
 * the backend's per-granularity defaults would chart up to twelve.
 *
 * The two suggestion rows come from two services. They are requested concurrently and fail
 * together, so a failure on either side leaves both rows as they were rather than refreshing one.
 */
class StoreInsightsRepositoryImpl @Inject constructor(
    private val dashboardApi: DashboardApiService,
    private val insightsApi: InsightsApiService,
) : StoreInsightsRepository {

    override suspend fun getSalesSummary(period: SalesPeriod): Result<SalesSummary> =
        dashboardApi.getSales(granularity = period.name, buckets = CHART_BUCKETS)
            // "Última actualización" is the time of the last successful fetch, a client-side fact.
            .toResult { it.toSalesSummary(period, generatedAt = Instant.now()) }

    override suspend fun getSuggestions(): Result<StoreSuggestions> = coroutineScope {
        val missing = async { insightsApi.getUnmetSearches(days = null, limit = null) }
        val restock = async { dashboardApi.getRestock(limit = null) }

        val missingProducts = missing.await().toResult { it.toMissingProductSuggestions() }
        val restockProducts = restock.await().toResult { it.toRestockSuggestions() }

        missingProducts.fold(
            onSuccess = { found -> restockProducts.map { StoreSuggestions(found, it) } },
            onFailure = { Result.failure(it) },
        )
    }

    private inline fun <T, R> NetworkResult<T>.toResult(transform: (T) -> R): Result<R> =
        when (this) {
            is NetworkResult.Success -> Result.success(transform(data))
            is NetworkResult.HttpError ->
                Result.failure(IllegalStateException("HTTP $code: $message"))
            is NetworkResult.NetworkError -> Result.failure(cause)
        }

    private companion object {
        const val CHART_BUCKETS = 4
    }
}
