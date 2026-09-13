package com.qhathia.mobile.staff.core.domain.usecase

import com.qhathia.mobile.staff.core.domain.repository.OrderRepository
import javax.inject.Inject

/** Re-reads the pending-order count from the backend and publishes it to every consumer. */
class RefreshPendingOrderCountUseCase @Inject constructor(
    private val repository: OrderRepository,
) {
    suspend operator fun invoke(): Result<Int> = repository.refreshPendingOrderCount()
}
