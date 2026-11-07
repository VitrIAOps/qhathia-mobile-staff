package com.qhathia.mobile.staff.core.network.dto

import kotlinx.serialization.Serializable

/**
 * The RFC 7807 ProblemDetail body that auth, biz and vo answer on every error, including the
 * 401/403 of the security filter (API_REFERENCE §42). [errorCode] is what tells apart two errors
 * with the same status (e.g. `CURRENT_PASSWORD_INCORRECT` and `SESSION_REVOKED`, both 401).
 *
 * The extra fields only appear on some errors:
 * - [currentVersion] — 409 `PRODUCT_VERSION_CONFLICT` (§40).
 * - [pendingOrders] — 409 `PRODUCT_IN_PENDING_ORDERS` (§39).
 * - [productId], [productName], [reason] — 409 `PRODUCT_NOT_SELLABLE` (§39).
 *
 * Every field is optional and the envelope's `type`/`title`/`status`/`instance`/`timestamp` are
 * ignored by the shared Json (`ignoreUnknownKeys`).
 */
@Serializable
data class ProblemDto(
    val errorCode: String? = null,
    val detail: String? = null,
    val currentVersion: Long? = null,
    val pendingOrders: List<PendingOrderRefDto> = emptyList(),
    val productId: String? = null,
    val productName: String? = null,
    val reason: String? = null,
)

/** One pending order that blocks deleting a product. Both fields are optional so one bad entry
 * cannot drop the whole ProblemDetail. */
@Serializable
data class PendingOrderRefDto(
    val id: String? = null,
    val orderCode: String? = null,
)
