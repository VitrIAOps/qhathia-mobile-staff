package com.qhathia.mobile.staff.feature.salesstatistics.ui

import androidx.annotation.StringRes
import com.qhathia.mobile.staff.core.domain.model.SalesPeriod
import com.qhathia.mobile.staff.core.domain.model.SalesSummary
import com.qhathia.mobile.staff.core.ui.component.sales.SalesBucketModel
import com.qhathia.mobile.staff.core.ui.component.sales.SalesPeriodUi
import com.qhathia.mobile.staff.core.ui.component.sales.SalesSummaryCardModel
import com.qhathia.mobile.staff.feature.salesstatistics.R
import com.qhathia.mobile.staff.feature.salesstatistics.model.StatisticsTab
import kotlinx.collections.immutable.toImmutableList

/** Tab label: "Resumen", "Ranking", "Actividad". */
@get:StringRes
internal val StatisticsTab.labelRes: Int
    get() = when (this) {
        StatisticsTab.SUMMARY -> R.string.statistics_tab_summary
        StatisticsTab.RANKING -> R.string.statistics_tab_ranking
        StatisticsTab.ACTIVITY -> R.string.statistics_tab_activity
    }

/** "+ 12% vs semana pasada" — the activity pill names the previous unit of its own period. */
@get:StringRes
internal val SalesPeriod.activityDeltaRes: Int
    get() = when (this) {
        SalesPeriod.MONTHLY -> R.string.statistics_activity_delta_monthly
        SalesPeriod.WEEKLY -> R.string.statistics_activity_delta_weekly
        SalesPeriod.DAILY -> R.string.statistics_activity_delta_daily
        SalesPeriod.HOURLY -> R.string.statistics_activity_delta_hourly
    }

/** The design system's mirror of the period; exhaustive, so a new period fails to compile here. */
internal fun SalesPeriod.toUi(): SalesPeriodUi = when (this) {
    SalesPeriod.MONTHLY -> SalesPeriodUi.MONTHLY
    SalesPeriod.WEEKLY -> SalesPeriodUi.WEEKLY
    SalesPeriod.DAILY -> SalesPeriodUi.DAILY
    SalesPeriod.HOURLY -> SalesPeriodUi.HOURLY
}

/** What the shared summary card draws for this summary. */
internal fun SalesSummary.toCardModel(): SalesSummaryCardModel = SalesSummaryCardModel(
    period = period.toUi(),
    total = total,
    deltaPercent = deltaPercent,
    buckets = buckets.map { SalesBucketModel(label = it.label, amount = it.amount) }.toImmutableList(),
    highlightedBucketIndex = highlightedBucketIndex,
    highlightedDeltaPercent = highlightedDeltaPercent,
)
