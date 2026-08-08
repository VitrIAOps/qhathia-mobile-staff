package com.qhathia.mobile.staff.core.domain.repository

import com.qhathia.mobile.staff.core.domain.model.OrderDetail
import com.qhathia.mobile.staff.core.domain.model.OrderPage
import com.qhathia.mobile.staff.core.domain.model.OrderStatus

/** Port for reading purchase orders and moving them out of `PENDING`. */
interface OrderRepository {

    /**
     * Lists the store's orders, newest first. [statuses] is sent as the backend's comma-separated
     * `status` filter (empty = every status); [query] searches customer name and order-code prefix
     * server-side and is omitted when null.
     */
    suspend fun getOrders(
        statuses: List<OrderStatus>,
        query: String?,
        page: Int,
        size: Int,
    ): Result<OrderPage>

    /** Fetches the full order, including its items and their live stock. */
    suspend fun getOrderDetail(id: String): Result<OrderDetail>

    /**
     * Confirms the order (`PENDING → CONFIRMED`), decrementing each item's stock server-side.
     * Failures carry a `ConfirmOrderException`.
     */
    suspend fun confirmOrder(id: String): Result<OrderDetail>

    /**
     * Rejects the order (`PENDING → REJECTED`) without a reason. Failures carry a
     * `RejectOrderException`.
     */
    suspend fun rejectOrder(id: String): Result<OrderDetail>
}
