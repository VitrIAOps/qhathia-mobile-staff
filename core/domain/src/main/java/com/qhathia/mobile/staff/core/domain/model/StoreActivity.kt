package com.qhathia.mobile.staff.core.domain.model

import java.time.Instant

/**
 * How shoppers used the kiosk tablet over a period. [uniqueSessions] counts distinct sessions —
 * everyone who used the tablet, bought or not — [productViews] product detail views, and
 * [confirmedOrders] the orders confirmed, which is the "who bought" figure. Each delta compares with
 * the previous period and is null when there is no base to compare against. [confirmedOrders] is
 * null when the backend did not report it, which is different from `0`.
 *
 * A tablet that emits no events yields zeros — that is a real reading, not a failure.
 */
data class StoreActivity(
    val periodStart: Instant?,
    val periodEnd: Instant?,
    val uniqueSessions: Int,
    val uniqueSessionsDeltaPercent: Double?,
    val productViews: Int,
    val productViewsDeltaPercent: Double?,
    val confirmedOrders: Int? = null,
    val confirmedOrdersDeltaPercent: Double? = null,
)
