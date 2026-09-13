package com.qhathia.mobile.staff.feature.home.ui

import androidx.annotation.StringRes
import com.qhathia.mobile.staff.core.domain.model.SalesPeriod
import com.qhathia.mobile.staff.feature.home.R
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlin.math.abs

/** Peruvian soles as the design writes them: `S/ 5,770`, grouped, without decimals. */
internal fun BigDecimal.toSolesLabel(): String {
    val format = NumberFormat.getIntegerInstance(Locale.US)
    return "S/ ${format.format(setScale(0, RoundingMode.HALF_UP))}"
}

/** Which way a percentage moved once rounded for display. */
internal enum class PercentTrend { UP, DOWN, FLAT }

/**
 * Decided on the rounded magnitude, so a change too small to print reads as flat — never as `-0%`
 * in the error colour next to a figure that did not move.
 */
internal val Double.percentTrend: PercentTrend
    get() = when {
        percentMagnitude().signum() == 0 -> PercentTrend.FLAT
        this > 0 -> PercentTrend.UP
        else -> PercentTrend.DOWN
    }

/** `+24%` / `-6%` / `0%` — the sign is part of the number, not of the sentence around it. */
internal fun Double.toSignedPercentLabel(): String = when (percentTrend) {
    PercentTrend.UP -> "+${toPercentMagnitudeLabel()}"
    PercentTrend.DOWN -> "-${toPercentMagnitudeLabel()}"
    PercentTrend.FLAT -> "0%"
}

/** `24%` for both `24.3` and `-24.3` — for sentences that say "más" or "menos" themselves. */
internal fun Double.toPercentMagnitudeLabel(): String = "${percentMagnitude().toPlainString()}%"

/**
 * Whole percentages, as the design writes them, except below 1% where one decimal keeps a real
 * `0.4%` change from collapsing into `0%`.
 */
private fun Double.percentMagnitude(): BigDecimal {
    if (!isFinite()) return BigDecimal.ZERO
    val magnitude = BigDecimal.valueOf(abs(this))
    val scale = if (magnitude >= BigDecimal.ONE) 0 else 1
    return magnitude.setScale(scale, RoundingMode.HALF_UP).stripTrailingZeros()
}

/** The device-local wall-clock time of the load, formatted for the device locale. */
internal fun Instant.toLocalTimeLabel(): String =
    DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
        .withZone(ZoneId.systemDefault())
        .format(this)

/** Tab label of a period. */
@get:StringRes
internal val SalesPeriod.labelRes: Int
    get() = when (this) {
        SalesPeriod.MONTHLY -> R.string.home_period_monthly
        SalesPeriod.WEEKLY -> R.string.home_period_weekly
        SalesPeriod.DAILY -> R.string.home_period_daily
        SalesPeriod.HOURLY -> R.string.home_period_hourly
    }

/** "Esta semana" — the period in progress, above the headline amount. */
@get:StringRes
internal val SalesPeriod.currentLabelRes: Int
    get() = when (this) {
        SalesPeriod.MONTHLY -> R.string.home_current_period_monthly
        SalesPeriod.WEEKLY -> R.string.home_current_period_weekly
        SalesPeriod.DAILY -> R.string.home_current_period_daily
        SalesPeriod.HOURLY -> R.string.home_current_period_hourly
    }

/** "+12% vs. semana anterior" — each period names its own previous unit. */
@get:StringRes
internal val SalesPeriod.deltaRes: Int
    get() = when (this) {
        SalesPeriod.MONTHLY -> R.string.home_delta_monthly
        SalesPeriod.WEEKLY -> R.string.home_delta_weekly
        SalesPeriod.DAILY -> R.string.home_delta_daily
        SalesPeriod.HOURLY -> R.string.home_delta_hourly
    }

/** "la semana anterior" — what the insight sentence compares the best bucket with. */
@get:StringRes
internal val SalesPeriod.previousRes: Int
    get() = when (this) {
        SalesPeriod.MONTHLY -> R.string.home_previous_monthly
        SalesPeriod.WEEKLY -> R.string.home_previous_weekly
        SalesPeriod.DAILY -> R.string.home_previous_daily
        SalesPeriod.HOURLY -> R.string.home_previous_hourly
    }
