package com.qhathia.mobile.staff.core.network.dto

import kotlinx.serialization.Serializable

/**
 * Row of `GET /api/v1/orders` — the compact shape, without `items[]`.
 *
 * `orderCode` is null for orders created before backend V15, `expiresAt` only exists while the
 * order can still expire, and the customer fields are absent on orders the kiosk created without
 * them; the data layer keeps all of them nullable and the screen renders explicit fallbacks.
 */
@Serializable
data class OrderSummaryDto(
    val id: String,
    val status: String? = null,
    val customerName: String? = null,
    val customerPhone: String? = null,
    val orderCode: String? = null,
    val itemCount: Int = 0,
    val total: Double = 0.0,
    val expiresAt: String? = null,
    val rejectionReason: String? = null,
    val createdAt: String? = null,
)

/** Spring page envelope of `GET /api/v1/orders`. */
@Serializable
data class OrderPageDto(
    val content: List<OrderSummaryDto> = emptyList(),
    val totalElements: Int = 0,
    val totalPages: Int = 0,
    val size: Int = 0,
    val number: Int = 0,
)

/**
 * Full order returned by `GET /api/v1/orders/{id}` and by the confirm/reject transitions. Unlike
 * [OrderSummaryDto] it carries `items[]` but no `itemCount` — the data layer derives the count from
 * the list.
 */
@Serializable
data class OrderDetailDto(
    val id: String,
    val status: String? = null,
    val customerName: String? = null,
    val customerPhone: String? = null,
    val orderCode: String? = null,
    val total: Double = 0.0,
    val items: List<OrderItemDto> = emptyList(),
    val expiresAt: String? = null,
    val rejectionReason: String? = null,
    val createdAt: String? = null,
)

/**
 * One line of [OrderDetailDto]. `unitPriceSnapshot` is the price frozen at order creation.
 * `productStock` is the product's live stock and is null for consult-mode products;
 * `productAvailability` is its live availability. Both were added in Fase 16 for the low-stock
 * warning. `productImageUrl` is the product's current primary image — not a snapshot — added in
 * Fase 24, and is null when the product has no images.
 */
@Serializable
data class OrderItemDto(
    val id: String,
    val productId: String,
    val productName: String = "",
    val quantity: Int = 0,
    val unitPriceSnapshot: Double = 0.0,
    val lineTotal: Double = 0.0,
    val productStock: Int? = null,
    val productAvailability: String? = null,
    val productImageUrl: String? = null,
)

/**
 * Body of `POST /api/v1/orders/{id}/reject`. The backend treats the body — and therefore the
 * reason — as optional, so this is only sent when the merchant picked one.
 */
@Serializable
data class RejectOrderRequestDto(val reason: String)
