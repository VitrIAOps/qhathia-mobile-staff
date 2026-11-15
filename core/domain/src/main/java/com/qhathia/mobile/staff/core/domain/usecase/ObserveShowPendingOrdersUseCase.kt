package com.qhathia.mobile.staff.core.domain.usecase

import com.qhathia.mobile.staff.core.domain.repository.OrderRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Fires when Solicitudes must show Pendientes from the top. */
class ObserveShowPendingOrdersUseCase @Inject constructor(
    private val repository: OrderRepository,
) {
    operator fun invoke(): Flow<Unit> = repository.showPendingRequested
}
