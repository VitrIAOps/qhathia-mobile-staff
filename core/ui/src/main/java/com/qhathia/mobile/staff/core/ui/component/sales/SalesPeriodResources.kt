package com.qhathia.mobile.staff.core.ui.component.sales

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
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

/** The selector label when there is little room: "Mes", "Sem.", "Día", "Hora". */
@get:StringRes
val SalesPeriodUi.shortLabelRes: Int
    get() = when (this) {
        SalesPeriodUi.MONTHLY -> R.string.sales_period_monthly_short
        SalesPeriodUi.WEEKLY -> R.string.sales_period_weekly_short
        SalesPeriodUi.DAILY -> R.string.sales_period_daily_short
        SalesPeriodUi.HOURLY -> R.string.sales_period_hourly_short
    }

/** Font scale from which the period pills switch to [shortLabelRes]: shrinking alone still cuts "Mensual". */
const val SHORT_PERIOD_LABEL_FONT_SCALE = 1.5f

/** The selector label for the current font scale: the full one, or the short one when the text is large. */
@Composable
fun SalesPeriodUi.selectorLabel(): String = stringResource(
    if (LocalDensity.current.fontScale >= SHORT_PERIOD_LABEL_FONT_SCALE) shortLabelRes else labelRes,
)

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
