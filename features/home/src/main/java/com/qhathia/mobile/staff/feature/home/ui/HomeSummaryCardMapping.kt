package com.qhathia.mobile.staff.feature.home.ui

import com.qhathia.mobile.staff.core.domain.model.SalesPeriod
import com.qhathia.mobile.staff.core.domain.model.SalesSummary
import com.qhathia.mobile.staff.core.ui.component.sales.SalesBucketModel
import com.qhathia.mobile.staff.core.ui.component.sales.SalesPeriodUi
import com.qhathia.mobile.staff.core.ui.component.sales.SalesSummaryCardModel
import kotlinx.collections.immutable.toImmutableList

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
