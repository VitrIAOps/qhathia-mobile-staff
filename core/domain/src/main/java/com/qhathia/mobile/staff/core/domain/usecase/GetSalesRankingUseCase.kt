package com.qhathia.mobile.staff.core.domain.usecase

import com.qhathia.mobile.staff.core.domain.model.SalesPeriod
import com.qhathia.mobile.staff.core.domain.model.SalesRanking
import com.qhathia.mobile.staff.core.domain.repository.StoreInsightsRepository
import javax.inject.Inject

/** What sold most and what did not move over a period, optionally for one product category. */
class GetSalesRankingUseCase @Inject constructor(
    private val repository: StoreInsightsRepository,
) {
    suspend operator fun invoke(period: SalesPeriod, categoryId: String?): Result<SalesRanking> =
        repository.getSalesRanking(period, categoryId)
}
