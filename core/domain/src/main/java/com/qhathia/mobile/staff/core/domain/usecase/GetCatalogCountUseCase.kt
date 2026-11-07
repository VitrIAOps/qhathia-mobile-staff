package com.qhathia.mobile.staff.core.domain.usecase

import com.qhathia.mobile.staff.core.domain.model.CatalogCount
import com.qhathia.mobile.staff.core.domain.repository.DeviceRepository
import javax.inject.Inject

/** The store's catalog size: every active product, and the ones the tablets show. */
class GetCatalogCountUseCase @Inject constructor(
    private val repository: DeviceRepository,
) {
    suspend operator fun invoke(): Result<CatalogCount> = repository.getCatalogCount()
}
