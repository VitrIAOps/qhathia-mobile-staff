package com.qhathia.mobile.staff.core.domain.model

import java.time.Instant

/**
 * A purchase order as listed in the Solicitudes screen — the compact shape returned by
 * `GET /api/v1/orders`, without its items.
 *
 * [customerName] and [orderCode] are nullable on purpose: the backend only added `orderCode` in V15,
 * so older orders carry none, and the kiosk may create an order without a customer name. The screen
 * renders explicit fallbacks rather than blank labels.
 *
 * [expiresAt] is the server's expiry deadline; the countdown is derived from it, never from a
 * client-side TTL. It is null for orders that are already in a terminal state.
 */
data class Order(
    val id: String,
    val status: OrderStatus,
    val customerName: String?,
    val customerPhone: String?,
    val orderCode: String?,
    val itemCount: Int,
    val total: Double,
    val expiresAt: Instant?,
    val rejectionReason: String?,
    val createdAt: Instant?,
)

/**
 * The full order returned by `GET /api/v1/orders/{id}` and by the confirm/reject transitions: the
 * same [order] summary plus its [items]. The list endpoint omits `items[]` to keep the payload
 * small, so the detail dialog always fetches instead of reusing a list row.
 */
data class OrderDetail(
    val order: Order,
    val items: List<OrderItem>,
)
