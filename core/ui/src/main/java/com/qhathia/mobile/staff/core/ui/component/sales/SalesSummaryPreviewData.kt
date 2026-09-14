package com.qhathia.mobile.staff.core.ui.component.sales

import kotlinx.collections.immutable.persistentListOf
import java.math.BigDecimal

/** Fixed models behind the sales component previews, using the design's figures. */
internal object SalesSummaryPreviewData {

    private fun bucket(label: String, amount: Long) = SalesBucketModel(label, BigDecimal(amount))

    /** The design's week: the period in progress is also the best one, 24% above the one before. */
    val weekly = SalesSummaryCardModel(
        period = SalesPeriodUi.WEEKLY,
        total = BigDecimal(1690),
        deltaPercent = 24.3,
        buckets = persistentListOf(
            bucket("Sem 10", 1240),
            bucket("Sem 11", 1480),
            bucket("Sem 12", 1360),
            bucket("Sem 13", 1690),
        ),
        highlightedBucketIndex = 3,
        highlightedDeltaPercent = 24.3,
    )

    /** A week in progress behind the previous one, while an earlier week stays the best. */
    val declining = weekly.copy(
        total = BigDecimal(1280),
        deltaPercent = -5.9,
        buckets = persistentListOf(
            bucket("Sem 10", 1240),
            bucket("Sem 11", 1480),
            bucket("Sem 12", 1360),
            bucket("Sem 13", 1280),
        ),
        highlightedBucketIndex = 1,
        highlightedDeltaPercent = 19.4,
    )

    /** A window that sold nothing — the card's empty treatment. */
    val empty = weekly.copy(
        period = SalesPeriodUi.HOURLY,
        total = BigDecimal.ZERO,
        deltaPercent = null,
        buckets = persistentListOf(),
        highlightedBucketIndex = -1,
        highlightedDeltaPercent = null,
    )

    /** One bucket a fraction of the tallest — the case the minimum bar height exists for. */
    val lopsidedBuckets = persistentListOf(
        bucket("Sem 10", 12),
        bucket("Sem 11", 40),
        bucket("Sem 12", 60),
        bucket("Sem 13", 1600),
    )
}
