package com.qhathia.mobile.staff.core.domain.usecase

import com.qhathia.mobile.staff.core.domain.model.OrderDetail
import com.qhathia.mobile.staff.core.domain.repository.OrderRepository
import javax.inject.Inject

/** Brings an expired order back to `PENDING`. Failures carry a `RecoverOrderException`. */
class RecoverOrderUseCase @Inject constructor(
    private val repository: OrderRepository,
) {
    suspend operator fun invoke(orderId: String): Result<OrderDetail> = repository.recoverOrder(orderId)
}
