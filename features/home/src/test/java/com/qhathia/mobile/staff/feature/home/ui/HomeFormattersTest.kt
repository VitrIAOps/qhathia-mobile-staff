package com.qhathia.mobile.staff.feature.home.ui

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class HomeFormattersTest {

    @Test
    fun `percentages print as whole numbers rounded half up`() {
        assertEquals("+24%", 24.3.toSignedPercentLabel())
        assertEquals("+25%", 24.5.toSignedPercentLabel())
    }

    @Test
    fun `negative percentages keep their sign`() {
        assertEquals("-6%", (-6.2).toSignedPercentLabel())
        assertEquals(PercentTrend.DOWN, (-6.2).percentTrend)
    }

    @Test
    fun `below one percent keeps one decimal so a real change does not read as flat`() {
        assertEquals("+0.4%", 0.4.toSignedPercentLabel())
        assertEquals("-0.4%", (-0.4).toSignedPercentLabel())
    }

    @Test
    fun `a change that rounds to nothing is flat, never -0%`() {
        assertEquals("0%", (-0.04).toSignedPercentLabel())
        assertEquals(PercentTrend.FLAT, (-0.04).percentTrend)
        assertEquals(PercentTrend.FLAT, 0.0.percentTrend)
    }

    @Test
    fun `values that round up to one percent drop the decimal`() {
        assertEquals("+1%", 0.96.toSignedPercentLabel())
    }

    @Test
    fun `the magnitude label drops the sign`() {
        assertEquals("6%", (-6.2).toPercentMagnitudeLabel())
        assertEquals("24%", 24.3.toPercentMagnitudeLabel())
    }
}
