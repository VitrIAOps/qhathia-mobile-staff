package com.qhathia.mobile.staff.core.domain.usecase

import com.qhathia.mobile.staff.core.domain.model.OrderDetail
import com.qhathia.mobile.staff.core.domain.repository.OrderRepository
import javax.inject.Inject

/** Rejects an order, without a reason. Failures carry a `RejectOrderException`. */
class RejectOrderUseCase @Inject constructor(
    private val repository: OrderRepository,
) {
    suspend operator fun invoke(orderId: String): Result<OrderDetail> =
        repository.rejectOrder(orderId)
}
