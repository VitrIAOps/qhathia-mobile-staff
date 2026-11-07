package com.qhathia.mobile.staff.feature.salesstatistics.ui

import com.qhathia.mobile.staff.core.domain.model.SalesPeriod
import com.qhathia.mobile.staff.core.ui.component.sales.PercentTrend
import com.qhathia.mobile.staff.core.ui.component.sales.percentTrend
import com.qhathia.mobile.staff.core.ui.component.sales.toPercentMagnitudeLabel
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/** The zone the backend cuts its periods on; the label must name the merchant's calendar days. */
internal val LIMA: ZoneId = ZoneId.of("America/Lima")
private val SPANISH_PERU: Locale = Locale.forLanguageTag("es-PE")
private val HOUR_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

/**
 * The calendar range a period covers, as the design writes it, in the app [locale]:
 * `24 – 30 de marzo` / `March 24 – 30`, `30 de marzo – 5 de abril` / `March 30 – April 5`,
 * `marzo 2026` / `March 2026`, `30 de marzo, 14:00 – 15:00` / `March 30, 14:00 – 15:00`.
 * Null without a start bound. The hours use [timeFormatter], which the screen builds from the
 * phone's 12/24-hour setting; the default is the 24-hour form.
 *
 * The contract does not say whether [end] is exclusive, so the last day is read at `end − 1 ns`:
 * right for both an exclusive midnight and an inclusive `23:59:59.999`.
 */
internal fun periodRangeLabel(
    period: SalesPeriod,
    start: Instant?,
    end: Instant?,
    locale: Locale = SPANISH_PERU,
    timeFormatter: DateTimeFormatter = HOUR_FORMAT,
): String? {
    val from = start?.atZone(LIMA) ?: return null
    return when (period) {
        SalesPeriod.MONTHLY -> "${from.monthName(locale)} ${from.year}"
        SalesPeriod.DAILY -> from.dayAndMonth(locale)
        SalesPeriod.HOURLY ->
            "${from.dayAndMonth(locale)}, ${from.format(timeFormatter)} – ${from.plusHours(1).format(timeFormatter)}"
        SalesPeriod.WEEKLY -> weekLabel(from, end, locale)
    }
}

/** `+ 12%` / `- 20%` / `0%` — the spaced form the activity pills use. */
internal fun Double.toSpacedSignedPercentLabel(): String = when (percentTrend) {
    PercentTrend.UP -> "+ ${toPercentMagnitudeLabel()}"
    PercentTrend.DOWN -> "- ${toPercentMagnitudeLabel()}"
    PercentTrend.FLAT -> "0%"
}

private fun weekLabel(from: ZonedDateTime, end: Instant?, locale: Locale): String {
    val to = end
        ?.minusNanos(1)
        ?.atZone(LIMA)
        ?.takeIf { it.toLocalDate() > from.toLocalDate() }
        ?: return from.dayAndMonth(locale)
    return if (locale.isSpanish) {
        when {
            from.year != to.year ->
                "${from.dayAndMonth(locale)} de ${from.year} – ${to.dayAndMonth(locale)} de ${to.year}"
            from.month != to.month -> "${from.dayAndMonth(locale)} – ${to.dayAndMonth(locale)}"
            else -> "${from.dayOfMonth} – ${to.dayAndMonth(locale)}"
        }
    } else {
        when {
            from.year != to.year ->
                "${from.dayAndMonth(locale)}, ${from.year} – ${to.dayAndMonth(locale)}, ${to.year}"
            from.month != to.month -> "${from.dayAndMonth(locale)} – ${to.dayAndMonth(locale)}"
            else -> "${from.dayAndMonth(locale)} – ${to.dayOfMonth}"
        }
    }
}

private val Locale.isSpanish: Boolean get() = language == "es"

private fun ZonedDateTime.monthName(locale: Locale): String = month.getDisplayName(TextStyle.FULL, locale)

/** "30 de marzo" in Spanish, "March 30" otherwise. */
private fun ZonedDateTime.dayAndMonth(locale: Locale): String =
    if (locale.isSpanish) "$dayOfMonth de ${monthName(locale)}" else "${monthName(locale)} $dayOfMonth"
