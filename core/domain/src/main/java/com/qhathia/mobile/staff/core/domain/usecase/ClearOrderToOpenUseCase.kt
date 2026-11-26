package com.qhathia.mobile.staff.core.domain.usecase

import com.qhathia.mobile.staff.core.domain.repository.OrderRepository
import javax.inject.Inject

/** Solicitudes opened [orderId]'s detail: the request is done. */
class ClearOrderToOpenUseCase @Inject constructor(
    private val repository: OrderRepository,
) {
    operator fun invoke(orderId: String) = repository.clearOrderToOpen(orderId)
}
