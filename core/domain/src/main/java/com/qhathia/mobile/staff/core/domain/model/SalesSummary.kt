package com.qhathia.mobile.staff.core.domain.model

import java.math.BigDecimal
import java.time.Instant

/** The four windows the Inicio summary card can be read over. */
enum class SalesPeriod {
    MONTHLY,
    WEEKLY,
    DAILY,
    HOURLY,
}

/**
 * One column of the sales chart. [label] is the bucket's name as the backend cuts it — `Sem 13` for
 * a weekly summary — and is not derived in the UI, because only the source knows which calendar
 * (and which time zone) the period was cut on.
 */
data class SalesBucket(
    val label: String,
    val amount: BigDecimal,
)

/**
 * What the store sold, as the Inicio summary card renders it.
 *
 * [total] is the revenue of the period in progress — the last of [buckets] — not the sum of the
 * chart. [deltaPercent] compares it with the previous period and is null when that period sold
 * nothing: there is no base, and inventing one would print a variation that did not happen.
 *
 * [highlightedBucketIndex] points at the best bucket in [buckets] and is `-1` when nothing in the
 * window sold. [highlightedDeltaPercent] compares that bucket with the one charted before it, and is
 * null when it is the first bucket or the preceding one sold nothing. Both are data statements —
 * the insight strip quotes them in prose — so the source computes them.
 *
 * Percentages keep the decimals the source computed; rounding them is a presentation decision.
 * [generatedAt] is when the data was received, backing "Última actualización a las HH:mm".
 */
data class SalesSummary(
    val period: SalesPeriod,
    val total: BigDecimal,
    val deltaPercent: Double?,
    val buckets: List<SalesBucket>,
    val highlightedBucketIndex: Int,
    val highlightedDeltaPercent: Double?,
    val generatedAt: Instant,
) {
    /** The best bucket of the period, or null when there is nothing to highlight. */
    val highlightedBucket: SalesBucket?
        get() = buckets.getOrNull(highlightedBucketIndex)
}
