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
private val LIMA: ZoneId = ZoneId.of("America/Lima")
private val SPANISH_PERU: Locale = Locale.forLanguageTag("es-PE")
private val HOUR_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

/**
 * The calendar range a period covers, as the design writes it: `24 – 30 de marzo`,
 * `30 de marzo – 5 de abril`, `marzo 2026`, `30 de marzo`, `30 de marzo, 14:00 – 15:00`.
 * Null without a start bound.
 *
 * The contract does not say whether [end] is exclusive, so the last day is read at `end − 1 ns`:
 * right for both an exclusive midnight and an inclusive `23:59:59.999`.
 */
internal fun periodRangeLabel(period: SalesPeriod, start: Instant?, end: Instant?): String? {
    val from = start?.atZone(LIMA) ?: return null
    return when (period) {
        SalesPeriod.MONTHLY -> "${from.monthName()} ${from.year}"
        SalesPeriod.DAILY -> from.dayAndMonth()
        SalesPeriod.HOURLY ->
            "${from.dayAndMonth()}, ${from.format(HOUR_FORMAT)} – ${from.plusHours(1).format(HOUR_FORMAT)}"
        SalesPeriod.WEEKLY -> weekLabel(from, end)
    }
}

/** `+ 12%` / `- 20%` / `0%` — the spaced form the activity pills use. */
internal fun Double.toSpacedSignedPercentLabel(): String = when (percentTrend) {
    PercentTrend.UP -> "+ ${toPercentMagnitudeLabel()}"
    PercentTrend.DOWN -> "- ${toPercentMagnitudeLabel()}"
    PercentTrend.FLAT -> "0%"
}

private fun weekLabel(from: ZonedDateTime, end: Instant?): String {
    val to = end
        ?.minusNanos(1)
        ?.atZone(LIMA)
        ?.takeIf { it.toLocalDate() > from.toLocalDate() }
        ?: return from.dayAndMonth()
    return when {
        from.year != to.year ->
            "${from.dayAndMonth()} de ${from.year} – ${to.dayAndMonth()} de ${to.year}"
        from.month != to.month -> "${from.dayAndMonth()} – ${to.dayAndMonth()}"
        else -> "${from.dayOfMonth} – ${to.dayAndMonth()}"
    }
}

private fun ZonedDateTime.monthName(): String = month.getDisplayName(TextStyle.FULL, SPANISH_PERU)

private fun ZonedDateTime.dayAndMonth(): String = "$dayOfMonth de ${monthName()}"
