package com.qhathia.mobile.staff.core.domain.usecase

import com.qhathia.mobile.staff.core.domain.model.SalesPeriod
import com.qhathia.mobile.staff.core.domain.model.StoreActivity
import com.qhathia.mobile.staff.core.domain.repository.StoreInsightsRepository
import javax.inject.Inject

/** How shoppers used the kiosk tablet over a period. */
class GetStoreActivityUseCase @Inject constructor(
    private val repository: StoreInsightsRepository,
) {
    suspend operator fun invoke(period: SalesPeriod): Result<StoreActivity> =
        repository.getStoreActivity(period)
}
