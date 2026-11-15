package com.qhathia.mobile.staff.core.data.local

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PreferredDeviceLabelTest {

    @Test
    fun `the name the user gave the phone wins`() {
        assertEquals("Galaxy A54 de Carla", preferredDeviceLabel(" Galaxy A54 de Carla ", "samsung", "SM-A546E"))
    }

    @Test
    fun `a blank or missing name falls back to maker and model`() {
        assertEquals("Samsung SM-A546E", preferredDeviceLabel("  ", "samsung", "SM-A546E"))
        assertEquals("Xiaomi Redmi Note 12", preferredDeviceLabel(null, "Xiaomi", "Redmi Note 12"))
    }

    @Test
    fun `a long name is cut at 100 characters`() {
        assertEquals(100, preferredDeviceLabel("x".repeat(150), "a", "b")?.length)
    }
}
