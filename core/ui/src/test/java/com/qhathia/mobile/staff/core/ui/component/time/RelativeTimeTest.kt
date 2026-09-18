package com.qhathia.mobile.staff.core.ui.component.time

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Instant

class RelativeTimeTest {

    private val now = Instant.parse("2026-03-10T15:00:00Z")

    @Test
    fun `under a minute and future instants read as just now`() {
        assertEquals(RelativeTime.JustNow, relativeTimeBetween(now.minusSeconds(30), now))
        assertEquals(RelativeTime.JustNow, relativeTimeBetween(now.plusSeconds(300), now))
    }

    @Test
    fun `minutes, hours and days buckets`() {
        assertEquals(RelativeTime.Minutes(4), relativeTimeBetween(now.minusSeconds(4 * 60 + 20), now))
        assertEquals(RelativeTime.Hours(2), relativeTimeBetween(now.minusSeconds(2 * 3600 + 59), now))
        assertEquals(RelativeTime.Days(3), relativeTimeBetween(now.minusSeconds(3 * 86_400), now))
    }

    @Test
    fun `months and years count calendar months`() {
        assertEquals(RelativeTime.Months(4), relativeTimeBetween(Instant.parse("2025-11-10T15:00:00Z"), now))
        assertEquals(RelativeTime.Years(1), relativeTimeBetween(Instant.parse("2025-02-01T15:00:00Z"), now))
    }
}
