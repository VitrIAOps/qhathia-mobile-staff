package com.qhathia.mobile.staff.core.testing

import com.qhathia.mobile.staff.core.domain.model.SalesPeriod
import com.qhathia.mobile.staff.core.domain.model.SalesSummary
import com.qhathia.mobile.staff.core.domain.model.StoreSuggestions
import com.qhathia.mobile.staff.core.domain.repository.StoreInsightsRepository

/**
 * Controllable [StoreInsightsRepository] for tests.
 *
 * By default every period answers with [TestFixtures.salesSummary] for that period. Register a
 * per-period answer with [setSummary] — including a failure — to drive the error path, and read
 * [requestedPeriods] to assert which period the screen actually asked for.
 */
class FakeStoreInsightsRepository : StoreInsightsRepository {

    private val summaries = mutableMapOf<SalesPeriod, Result<SalesSummary>>()

    /** Answer for any period with no [setSummary] override. */
    var defaultSummaryResult: ((SalesPeriod) -> Result<SalesSummary>) = { period ->
        Result.success(TestFixtures.salesSummary(period = period))
    }

    var suggestionsResult: Result<StoreSuggestions> = Result.success(TestFixtures.storeSuggestions())

    val requestedPeriods: MutableList<SalesPeriod> = mutableListOf()
    var suggestionsCallCount: Int = 0
        private set

    /** Pins the answer for [period]; pass a failed [Result] to exercise the error path. */
    fun setSummary(period: SalesPeriod, result: Result<SalesSummary>) {
        summaries[period] = result
    }

    /** Makes every period fail, whatever was registered before. */
    fun failEverything(cause: Throwable = IllegalStateException("insights unavailable")) {
        summaries.clear()
        defaultSummaryResult = { Result.failure(cause) }
        suggestionsResult = Result.failure(cause)
    }

    override suspend fun getSalesSummary(period: SalesPeriod): Result<SalesSummary> {
        requestedPeriods += period
        return summaries[period] ?: defaultSummaryResult(period)
    }

    override suspend fun getSuggestions(): Result<StoreSuggestions> {
        suggestionsCallCount++
        return suggestionsResult
    }
}
