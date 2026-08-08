package com.qhathia.mobile.staff.core.domain.usecase

import com.qhathia.mobile.staff.core.domain.model.OrderDetail
import com.qhathia.mobile.staff.core.domain.repository.OrderRepository
import javax.inject.Inject

/**
 * Rejects an order. [reason] is the motive the merchant picked in the reject form — free text, as
 * the contract defines it — and is null only when a caller rejects without asking. Failures carry a
 * `RejectOrderException`.
 */
class RejectOrderUseCase @Inject constructor(
    private val repository: OrderRepository,
) {
    suspend operator fun invoke(orderId: String, reason: String?): Result<OrderDetail> =
        repository.rejectOrder(orderId, reason)
}
