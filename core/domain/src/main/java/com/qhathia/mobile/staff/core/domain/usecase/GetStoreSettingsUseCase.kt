package com.qhathia.mobile.staff.core.domain.usecase

import com.qhathia.mobile.staff.core.domain.model.StoreSettings
import com.qhathia.mobile.staff.core.domain.repository.StoreSettingsRepository
import javax.inject.Inject

/** Loads the active store's display settings (its language). */
class GetStoreSettingsUseCase @Inject constructor(
    private val repository: StoreSettingsRepository,
) {
    suspend operator fun invoke(): Result<StoreSettings> = repository.get()
}
