package com.qhathia.mobile.staff.core.domain.usecase

import com.qhathia.mobile.staff.core.domain.model.OrderPage
import com.qhathia.mobile.staff.core.domain.model.OrderStatus
import com.qhathia.mobile.staff.core.domain.repository.OrderRepository
import javax.inject.Inject

/**
 * Lists the store's orders for the given [statuses], optionally narrowed by a server-side search
 * [query] over customer name and order-code prefix.
 */
class GetOrdersUseCase @Inject constructor(
    private val repository: OrderRepository,
) {
    suspend operator fun invoke(
        statuses: List<OrderStatus>,
        query: String? = null,
        page: Int = 0,
        size: Int = DEFAULT_PAGE_SIZE,
    ): Result<OrderPage> = repository.getOrders(statuses, query, page, size)

    companion object {
        const val DEFAULT_PAGE_SIZE = 20
    }
}
