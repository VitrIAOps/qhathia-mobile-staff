package com.qhathia.mobile.staff.core.ui.component.sales

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import java.math.BigDecimal

/**
 * The four windows a sales summary can be read over. It mirrors the domain period so the design
 * system stays free of domain types; each feature maps its period onto this one.
 */
enum class SalesPeriodUi {
    MONTHLY,
    WEEKLY,
    DAILY,
    HOURLY,
}

/** One bar of [QhathiaSalesBarChart]: the bucket's name as the source labels it, and its amount. */
@Immutable
data class SalesBucketModel(
    val label: String,
    val amount: BigDecimal,
)

/**
 * Everything [QhathiaSalesSummaryCard] draws.
 *
 * [total] is the revenue of the period in progress; [deltaPercent] compares it with the previous
 * period and is null when there is no base. [highlightedBucketIndex] points at the best bucket
 * (`-1` when nothing sold) and [highlightedDeltaPercent] compares it with the bucket before it.
 * Percentages keep their decimals — rounding happens when they are printed.
 */
@Immutable
data class SalesSummaryCardModel(
    val period: SalesPeriodUi,
    val total: BigDecimal,
    val deltaPercent: Double?,
    val buckets: ImmutableList<SalesBucketModel>,
    val highlightedBucketIndex: Int,
    val highlightedDeltaPercent: Double?,
) {
    /** The best bucket of the window, or null when there is nothing to highlight. */
    val highlightedBucket: SalesBucketModel?
        get() = buckets.getOrNull(highlightedBucketIndex)

    /**
     * The period in progress is routinely at zero early on while the periods before it sold, and
     * the chart is still worth showing then. Only a window with no sales anywhere is empty.
     */
    val hasSales: Boolean
        get() = total.signum() > 0 || buckets.any { it.amount.signum() > 0 }
}
