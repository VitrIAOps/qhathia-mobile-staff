package com.qhathia.mobile.staff.core.domain.model

import java.math.BigDecimal
import java.time.Instant

/**
 * The store's current subscription. There is no payment data: the backend has no gateway.
 *
 * @property endsAt the renewal date; `null` when the plan has no end, in which case the period
 *   progress cannot be drawn.
 */
data class Subscription(
    val planType: String,
    val planName: String,
    val status: SubscriptionStatus,
    val priceMonthlyPen: BigDecimal,
    val features: PlanFeatures,
    val startedAt: Instant?,
    val endsAt: Instant?,
)

/**
 * What the plan includes. [productLimit] `null` means unlimited products.
 */
data class PlanFeatures(
    val productLimit: Int?,
    val hasAdvancedAnalytics: Boolean,
    val hasVideoProducts: Boolean,
    val hasBulkPriceUpdate: Boolean,
    val hasPrioritySupport: Boolean,
)

/** Subscription lifecycle; [UNKNOWN] guards against unrecognized server values. */
enum class SubscriptionStatus {
    ACTIVE,
    CANCELLED,
    EXPIRED,
    UNKNOWN,
}
