package com.qhathia.mobile.staff.core.ui.component

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * The badge's one rule that is not a rendering concern: past 99 the exact number stops being
 * actionable, so it is capped. Runs on the JVM; the rendering itself is covered by
 * `QhathiaBottomBarItemTest` on a device.
 */
class QhathiaBadgeLabelTest {

    @Test
    fun `a small count is written out`() {
        assertEquals("2", badgeLabel(2))
    }

    @Test
    fun `ninety-nine is the last exact count`() {
        assertEquals("99", badgeLabel(99))
    }

    @Test
    fun `anything above ninety-nine is capped`() {
        assertEquals("99+", badgeLabel(100))
        assertEquals("99+", badgeLabel(1280))
    }
}
