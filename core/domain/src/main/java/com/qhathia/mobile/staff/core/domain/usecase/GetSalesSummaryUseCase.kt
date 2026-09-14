package com.qhathia.mobile.staff.core.domain.usecase

import com.qhathia.mobile.staff.core.domain.model.SalesPeriod
import com.qhathia.mobile.staff.core.domain.model.SalesSummary
import com.qhathia.mobile.staff.core.domain.repository.StoreInsightsRepository
import javax.inject.Inject

/** The sales summary for a period, optionally limited to one product category. */
class GetSalesSummaryUseCase @Inject constructor(
    private val repository: StoreInsightsRepository,
) {
    suspend operator fun invoke(
        period: SalesPeriod,
        categoryId: String? = null,
    ): Result<SalesSummary> = repository.getSalesSummary(period, categoryId)
}
