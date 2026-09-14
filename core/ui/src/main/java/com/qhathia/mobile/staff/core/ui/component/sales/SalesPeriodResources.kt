package com.qhathia.mobile.staff.core.ui.component.sales

import androidx.annotation.StringRes
import com.qhathia.mobile.staff.core.ui.R

/** Selector label of a period: "Mensual", "Semanal", "Diario", "Por hora". */
@get:StringRes
val SalesPeriodUi.labelRes: Int
    get() = when (this) {
        SalesPeriodUi.MONTHLY -> R.string.sales_period_monthly
        SalesPeriodUi.WEEKLY -> R.string.sales_period_weekly
        SalesPeriodUi.DAILY -> R.string.sales_period_daily
        SalesPeriodUi.HOURLY -> R.string.sales_period_hourly
    }

/** "Esta semana" — the period in progress, above the headline amount. */
@get:StringRes
val SalesPeriodUi.currentLabelRes: Int
    get() = when (this) {
        SalesPeriodUi.MONTHLY -> R.string.sales_current_period_monthly
        SalesPeriodUi.WEEKLY -> R.string.sales_current_period_weekly
        SalesPeriodUi.DAILY -> R.string.sales_current_period_daily
        SalesPeriodUi.HOURLY -> R.string.sales_current_period_hourly
    }

/** "+12% vs. semana anterior" — each period names its own previous unit. */
@get:StringRes
internal val SalesPeriodUi.deltaRes: Int
    get() = when (this) {
        SalesPeriodUi.MONTHLY -> R.string.sales_delta_monthly
        SalesPeriodUi.WEEKLY -> R.string.sales_delta_weekly
        SalesPeriodUi.DAILY -> R.string.sales_delta_daily
        SalesPeriodUi.HOURLY -> R.string.sales_delta_hourly
    }

/** "la semana anterior" — what the insight sentence compares the best bucket with. */
@get:StringRes
internal val SalesPeriodUi.previousRes: Int
    get() = when (this) {
        SalesPeriodUi.MONTHLY -> R.string.sales_previous_monthly
        SalesPeriodUi.WEEKLY -> R.string.sales_previous_weekly
        SalesPeriodUi.DAILY -> R.string.sales_previous_daily
        SalesPeriodUi.HOURLY -> R.string.sales_previous_hourly
    }
