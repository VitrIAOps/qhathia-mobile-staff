package com.qhathia.mobile.staff.core.domain.model

/**
 * Lifecycle state of a purchase order. Mirrors the backend `OrderStatus` enum.
 *
 * `PENDING` is the only actionable state: [CONFIRMED], [REJECTED], [CANCELLED] and [EXPIRED] are
 * terminal, and confirming or rejecting an order in one of them returns `422` from the backend.
 * [EXPIRED] is set server-side by the TTL job, never by the app.
 */
enum class OrderStatus {
    PENDING,
    CONFIRMED,
    REJECTED,
    CANCELLED,
    EXPIRED,
}
