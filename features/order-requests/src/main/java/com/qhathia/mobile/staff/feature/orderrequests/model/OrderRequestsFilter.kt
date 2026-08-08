package com.qhathia.mobile.staff.feature.orderrequests.model

import com.qhathia.mobile.staff.core.domain.model.OrderStatus

/**
 * The three status tabs of the Solicitudes screen, each carrying the backend statuses it maps to.
 * Rechazados deliberately covers both manual rejections and TTL expiries, per the contract.
 */
enum class OrderRequestsFilter(val statuses: List<OrderStatus>) {
    PENDING(listOf(OrderStatus.PENDING)),
    CONFIRMED(listOf(OrderStatus.CONFIRMED)),
    REJECTED(listOf(OrderStatus.REJECTED, OrderStatus.EXPIRED)),
}
