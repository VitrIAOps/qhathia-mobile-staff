package com.qhathia.mobile.staff.core.domain.usecase

import com.qhathia.mobile.staff.core.domain.repository.DeviceRepository
import javax.inject.Inject

/** How many active products the store's tablets can show. */
class GetCatalogCountUseCase @Inject constructor(
    private val repository: DeviceRepository,
) {
    suspend operator fun invoke(): Result<Int> = repository.getCatalogCount()
}
