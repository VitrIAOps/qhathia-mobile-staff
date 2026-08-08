package com.qhathia.mobile.staff.core.domain.usecase

import com.qhathia.mobile.staff.core.domain.model.OrderDetail
import com.qhathia.mobile.staff.core.domain.repository.OrderRepository
import javax.inject.Inject

/** Fetches one order with its items and their live stock, for the detail dialog. */
class GetOrderDetailUseCase @Inject constructor(
    private val repository: OrderRepository,
) {
    suspend operator fun invoke(orderId: String): Result<OrderDetail> =
        repository.getOrderDetail(orderId)
}
