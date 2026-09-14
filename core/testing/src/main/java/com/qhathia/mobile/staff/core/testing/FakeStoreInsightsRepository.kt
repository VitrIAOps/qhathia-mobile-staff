package com.qhathia.mobile.staff.core.testing

import com.qhathia.mobile.staff.core.domain.model.SalesPeriod
import com.qhathia.mobile.staff.core.domain.model.SalesRanking
import com.qhathia.mobile.staff.core.domain.model.SalesSummary
import com.qhathia.mobile.staff.core.domain.model.StoreActivity
import com.qhathia.mobile.staff.core.domain.model.StoreSuggestions
import com.qhathia.mobile.staff.core.domain.repository.StoreInsightsRepository

/**
 * Controllable [StoreInsightsRepository] for tests.
 *
 * By default every period answers with [TestFixtures.salesSummary] for that period, and the ranking
 * and activity with their fixtures. Register a per-period answer with [setSummary] — including a
 * failure — to drive the error path, and read the recorded requests to assert what a screen asked
 * for, filters included.
 */
class FakeStoreInsightsRepository : StoreInsightsRepository {

    /** One recorded request: the period and the category filter it carried. */
    data class Request(val period: SalesPeriod, val categoryId: String?)

    private val summaries = mutableMapOf<SalesPeriod, Result<SalesSummary>>()

    /** Answer for any period with no [setSummary] override. */
    var defaultSummaryResult: ((SalesPeriod) -> Result<SalesSummary>) = { period ->
        Result.success(TestFixtures.salesSummary(period = period))
    }

    var suggestionsResult: Result<StoreSuggestions> = Result.success(TestFixtures.storeSuggestions())

    var rankingResult: Result<SalesRanking> = Result.success(TestFixtures.salesRanking())

    var activityResult: Result<StoreActivity> = Result.success(TestFixtures.storeActivity())

    val requestedPeriods: MutableList<SalesPeriod> = mutableListOf()
    val summaryRequests: MutableList<Request> = mutableListOf()
    val rankingRequests: MutableList<Request> = mutableListOf()
    val activityRequests: MutableList<SalesPeriod> = mutableListOf()

    var suggestionsCallCount: Int = 0
        private set

    /** Pins the answer for [period]; pass a failed [Result] to exercise the error path. */
    fun setSummary(period: SalesPeriod, result: Result<SalesSummary>) {
        summaries[period] = result
    }

    /** Makes every call fail, whatever was registered before. */
    fun failEverything(cause: Throwable = IllegalStateException("insights unavailable")) {
        summaries.clear()
        defaultSummaryResult = { Result.failure(cause) }
        suggestionsResult = Result.failure(cause)
        rankingResult = Result.failure(cause)
        activityResult = Result.failure(cause)
    }

    override suspend fun getSalesSummary(period: SalesPeriod, categoryId: String?): Result<SalesSummary> {
        requestedPeriods += period
        summaryRequests += Request(period, categoryId)
        return summaries[period] ?: defaultSummaryResult(period)
    }

    override suspend fun getSuggestions(): Result<StoreSuggestions> {
        suggestionsCallCount++
        return suggestionsResult
    }

    override suspend fun getSalesRanking(period: SalesPeriod, categoryId: String?): Result<SalesRanking> {
        rankingRequests += Request(period, categoryId)
        return rankingResult
    }

    override suspend fun getStoreActivity(period: SalesPeriod): Result<StoreActivity> {
        activityRequests += period
        return activityResult
    }
}
