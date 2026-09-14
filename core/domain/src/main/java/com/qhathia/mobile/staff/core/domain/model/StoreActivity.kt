package com.qhathia.mobile.staff.core.domain.model

import java.time.Instant

/**
 * How shoppers used the kiosk tablet over a period. [uniqueSessions] counts distinct sessions,
 * [productViews] product detail views. Each delta compares with the previous period and is null
 * when there is no base to compare against.
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
)
