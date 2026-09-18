package com.qhathia.mobile.staff.core.domain.usecase

import com.qhathia.mobile.staff.core.domain.model.StoreSettings
import com.qhathia.mobile.staff.core.domain.repository.StoreSettingsRepository
import javax.inject.Inject

/** Declares whether the store's prices already include IGV. Never recalculates a price. */
class SetPricesIncludeIgvUseCase @Inject constructor(
    private val repository: StoreSettingsRepository,
) {
    suspend operator fun invoke(pricesIncludeIgv: Boolean): Result<StoreSettings> =
        repository.updatePricesIncludeIgv(pricesIncludeIgv)
}
