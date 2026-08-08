package com.qhathia.mobile.staff.core.domain.usecase

import com.qhathia.mobile.staff.core.domain.model.OrderDetail
import com.qhathia.mobile.staff.core.domain.repository.OrderRepository
import javax.inject.Inject

/**
 * Confirms an order. Failures carry a `ConfirmOrderException`, so callers can tell an out-of-stock
 * conflict apart from an order someone else already handled.
 */
class ConfirmOrderUseCase @Inject constructor(
    private val repository: OrderRepository,
) {
    suspend operator fun invoke(orderId: String): Result<OrderDetail> =
        repository.confirmOrder(orderId)
}
