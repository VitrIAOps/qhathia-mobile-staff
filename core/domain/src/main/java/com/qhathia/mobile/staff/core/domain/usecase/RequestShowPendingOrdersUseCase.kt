package com.qhathia.mobile.staff.core.domain.usecase

import com.qhathia.mobile.staff.core.domain.repository.OrderRepository
import javax.inject.Inject

/**
 * Asks Solicitudes to show Pendientes from the top, e.g. after a notification tap, and to open
 * [orderId]'s detail when the tap was on one order's notification.
 */
class RequestShowPendingOrdersUseCase @Inject constructor(
    private val repository: OrderRepository,
) {
    operator fun invoke(orderId: String? = null) {
        repository.requestShowPending()
        if (orderId != null) repository.requestOpenOrder(orderId)
    }
}
