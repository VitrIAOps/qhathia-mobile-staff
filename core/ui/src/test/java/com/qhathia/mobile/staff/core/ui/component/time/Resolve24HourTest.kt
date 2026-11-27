package com.qhathia.mobile.staff.core.ui.component.time

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class Resolve24HourTest {

    @Test
    fun `an explicit 12 wins over a 24-hour system language`() {
        assertFalse(resolve24Hour(setting = "12", systemLocaleIs24 = true))
    }

    @Test
    fun `an explicit 24 wins over a 12-hour system language`() {
        assertTrue(resolve24Hour(setting = "24", systemLocaleIs24 = false))
    }

    @Test
    fun `unset on an English (US) phone gives 12 hours`() {
        assertFalse(resolve24Hour(setting = null, systemLocaleIs24 = false))
    }

    @Test
    fun `unset on a Spanish phone gives 24 hours`() {
        assertTrue(resolve24Hour(setting = null, systemLocaleIs24 = true))
    }
}
