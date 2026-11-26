package com.qhathia.mobile.staff.core.domain.usecase

import com.qhathia.mobile.staff.core.domain.repository.OrderRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** The order a notification asked Solicitudes to open, or null. */
class ObserveOrderToOpenUseCase @Inject constructor(
    private val repository: OrderRepository,
) {
    operator fun invoke(): Flow<String?> = repository.orderToOpen
}
