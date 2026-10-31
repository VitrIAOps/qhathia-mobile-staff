package com.qhathia.mobile.staff.feature.productdetail.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant

class DiscountTimeLeftTest {

    private val now = Instant.parse("2026-10-31T12:00:00Z")

    private fun leftFor(duration: Duration) = remainingDiscountTime(now.plus(duration), now)

    @Test
    fun `days and leftover hours are counted, minutes dropped`() {
        assertEquals(
            DiscountTimeLeft.DaysAndHours(6, 4),
            leftFor(Duration.ofDays(6).plusHours(4).plusMinutes(20)),
        )
    }

    @Test
    fun `whole days keep zero hours`() {
        assertEquals(DiscountTimeLeft.DaysAndHours(1, 0), leftFor(Duration.ofDays(1).plusMinutes(30)))
    }

    @Test
    fun `less than a day counts hours rounded down`() {
        assertEquals(DiscountTimeLeft.Hours(5), leftFor(Duration.ofHours(5).plusMinutes(59)))
    }

    @Test
    fun `less than an hour is under an hour`() {
        assertEquals(DiscountTimeLeft.UnderAnHour, leftFor(Duration.ofMinutes(40)))
    }

    @Test
    fun `an ended discount has no time left`() {
        assertNull(leftFor(Duration.ZERO))
        assertNull(leftFor(Duration.ofMinutes(-1)))
    }
}
