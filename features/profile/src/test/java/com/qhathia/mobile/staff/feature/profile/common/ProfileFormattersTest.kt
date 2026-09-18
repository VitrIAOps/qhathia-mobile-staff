package com.qhathia.mobile.staff.feature.profile.common

import com.qhathia.mobile.staff.core.domain.model.PlanFeatures
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.Locale

class ProfileFormattersTest {

    @Test
    fun `initials take the first letter of up to two words`() {
        assertEquals("CM", initialsOf("carla  mendoza ruiz"))
        assertEquals("C", initialsOf("Carla"))
        assertEquals("?", initialsOf("   "))
    }

    @Test
    fun `peruvian mobile is grouped after the country prefix`() {
        assertEquals("+51 999 888 777", formatPeruPhone("999888777"))
        assertEquals("+51 12345", formatPeruPhone("12345"))
    }

    @Test
    fun `plan checklist lists the product limit first and only the true flags`() {
        val lines = PlanFeatures(
            productLimit = 200,
            hasAdvancedAnalytics = true,
            hasVideoProducts = false,
            hasBulkPriceUpdate = false,
            hasPrioritySupport = true,
        ).lines()

        assertEquals(
            listOf(PlanFeatureLine.ProductLimit(200), PlanFeatureLine.AdvancedAnalytics, PlanFeatureLine.PrioritySupport),
            lines,
        )
        assertEquals(
            listOf(PlanFeatureLine.UnlimitedProducts),
            PlanFeatures(null, false, false, false, false).lines(),
        )
    }

    @Test
    fun `period progress is the elapsed fraction, clamped, and absent without bounds`() {
        val start = Instant.parse("2026-01-01T00:00:00Z")
        val end = Instant.parse("2026-01-11T00:00:00Z")

        assertEquals(0.5f, periodProgress(start, end, Instant.parse("2026-01-06T00:00:00Z"))!!, 0.001f)
        assertEquals(1f, periodProgress(start, end, Instant.parse("2027-01-01T00:00:00Z")))
        assertNull(periodProgress(start, null, end))
        assertNull(periodProgress(end, start, end))
    }

    @Test
    fun `dates follow the app locale in Lima time`() {
        // 05:00Z is midnight in Lima, so the date must not slip to the previous day.
        val renewal = Instant.parse("2026-09-15T05:00:00Z")

        assertEquals("September 15, 2026", renewal.longDateLabel(Locale.US))
        assertTrue(renewal.longDateLabel(Locale.forLanguageTag("es-PE")).startsWith("15 de"))
        assertTrue(renewal.shortDateLabel(Locale.US).startsWith("15 Sep"))
    }
}
