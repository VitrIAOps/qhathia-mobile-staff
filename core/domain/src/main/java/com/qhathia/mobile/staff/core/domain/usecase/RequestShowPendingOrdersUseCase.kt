package com.qhathia.mobile.staff.core.domain.usecase

import com.qhathia.mobile.staff.core.domain.repository.OrderRepository
import javax.inject.Inject

/** Asks Solicitudes to show Pendientes from the top, e.g. after a notification tap. */
class RequestShowPendingOrdersUseCase @Inject constructor(
    private val repository: OrderRepository,
) {
    operator fun invoke() = repository.requestShowPending()
}
