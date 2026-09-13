package com.qhathia.mobile.staff.core.domain.usecase

import com.qhathia.mobile.staff.core.domain.repository.OrderRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Streams the app-wide pending-order count. Every consumer — the Inicio bell, the Solicitudes tab
 * badge and the `Pendientes (N)` filter label — reads this same flow, so they cannot disagree.
 */
class ObservePendingOrderCountUseCase @Inject constructor(
    private val repository: OrderRepository,
) {
    operator fun invoke(): Flow<Int> = repository.pendingOrderCount
}
