package com.qhathia.mobile.staff.core.domain.usecase

import com.qhathia.mobile.staff.core.domain.model.SalesPeriod
import com.qhathia.mobile.staff.core.domain.model.SalesSummary
import com.qhathia.mobile.staff.core.domain.repository.StoreInsightsRepository
import javax.inject.Inject

/** Reads the store's sales summary for [SalesPeriod]. */
class GetSalesSummaryUseCase @Inject constructor(
    private val repository: StoreInsightsRepository,
) {
    suspend operator fun invoke(period: SalesPeriod): Result<SalesSummary> =
        repository.getSalesSummary(period)
}
