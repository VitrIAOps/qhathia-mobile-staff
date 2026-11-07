package com.qhathia.mobile.staff.feature.salesstatistics.ui

import com.qhathia.mobile.staff.core.domain.model.SalesPeriod
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class StatisticsFormattersTest {

    private val lima = ZoneId.of("America/Lima")

    private fun at(year: Int, month: Int, day: Int, hour: Int = 0): Instant =
        ZonedDateTime.of(year, month, day, hour, 0, 0, 0, lima).toInstant()

    @Test
    fun `a week within one month names the month once`() {
        assertEquals(
            "24 – 30 de marzo",
            periodRangeLabel(SalesPeriod.WEEKLY, at(2026, 3, 24), at(2026, 3, 31)),
        )
    }

    @Test
    fun `a week across two months names both`() {
        assertEquals(
            "30 de marzo – 5 de abril",
            periodRangeLabel(SalesPeriod.WEEKLY, at(2026, 3, 30), at(2026, 4, 6)),
        )
    }

    @Test
    fun `a week across two years names both years`() {
        assertEquals(
            "29 de diciembre de 2025 – 4 de enero de 2026",
            periodRangeLabel(SalesPeriod.WEEKLY, at(2025, 12, 29), at(2026, 1, 5)),
        )
    }

    @Test
    fun `an exclusive and an inclusive end bound give the same week`() {
        val inclusiveEnd = at(2026, 3, 31).minusMillis(1)

        assertEquals(
            periodRangeLabel(SalesPeriod.WEEKLY, at(2026, 3, 24), at(2026, 3, 31)),
            periodRangeLabel(SalesPeriod.WEEKLY, at(2026, 3, 24), inclusiveEnd),
        )
    }

    @Test
    fun `a month, a day and an hour`() {
        assertEquals("marzo 2026", periodRangeLabel(SalesPeriod.MONTHLY, at(2026, 3, 1), at(2026, 4, 1)))
        assertEquals("30 de marzo", periodRangeLabel(SalesPeriod.DAILY, at(2026, 3, 30), at(2026, 3, 31)))
        assertEquals(
            "30 de marzo, 14:00 – 15:00",
            periodRangeLabel(SalesPeriod.HOURLY, at(2026, 3, 30, 14), at(2026, 3, 30, 15)),
        )
    }

    @Test
    fun `an hour follows the formatter it is given, such as a 12-hour one`() {
        val twelveHour = DateTimeFormatter.ofPattern("h:mm a", Locale.US)

        assertEquals(
            "30 de marzo, 2:00 PM – 3:00 PM",
            periodRangeLabel(SalesPeriod.HOURLY, at(2026, 3, 30, 14), at(2026, 3, 30, 15), timeFormatter = twelveHour),
        )
    }

    @Test
    fun `bounds are read in Lima time, not UTC`() {
        // 02:00 UTC on the 25th is still the evening of the 24th in Lima.
        assertEquals(
            "24 de marzo",
            periodRangeLabel(SalesPeriod.DAILY, Instant.parse("2026-03-25T02:00:00Z"), null),
        )
    }

    @Test
    fun `labels follow the app language when it is English`() {
        val english = Locale.US

        assertEquals("March 24 – 30", periodRangeLabel(SalesPeriod.WEEKLY, at(2026, 3, 24), at(2026, 3, 31), english))
        assertEquals("March 30 – April 5", periodRangeLabel(SalesPeriod.WEEKLY, at(2026, 3, 30), at(2026, 4, 6), english))
        assertEquals(
            "December 29, 2025 – January 4, 2026",
            periodRangeLabel(SalesPeriod.WEEKLY, at(2025, 12, 29), at(2026, 1, 5), english),
        )
        assertEquals("March 2026", periodRangeLabel(SalesPeriod.MONTHLY, at(2026, 3, 1), at(2026, 4, 1), english))
        assertEquals(
            "March 30, 14:00 – 15:00",
            periodRangeLabel(SalesPeriod.HOURLY, at(2026, 3, 30, 14), at(2026, 3, 30, 15), english),
        )
    }

    @Test
    fun `no start bound, no label`() {
        assertNull(periodRangeLabel(SalesPeriod.WEEKLY, null, at(2026, 3, 31)))
    }

    @Test
    fun `activity percentages are spaced and signed`() {
        assertEquals("+ 13%", 12.5.toSpacedSignedPercentLabel())
        assertEquals("- 20%", (-20.0).toSpacedSignedPercentLabel())
        assertEquals("0%", 0.0.toSpacedSignedPercentLabel())
    }
}
