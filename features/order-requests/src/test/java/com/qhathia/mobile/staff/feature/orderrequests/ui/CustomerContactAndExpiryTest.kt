package com.qhathia.mobile.staff.feature.orderrequests.ui

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.Duration

class CustomerContactAndExpiryTest {

    @Test
    fun `a Peruvian cell-phone is 9 digits starting with 9, with or without +51`() {
        assertEquals("987654321", peruvianCellOrNull("987654321"))
        assertEquals("987654321", peruvianCellOrNull("987 654 321"))
        assertEquals("987654321", peruvianCellOrNull("+51 987-654-321"))
    }

    @Test
    fun `anything else gets no contact buttons`() {
        assertNull(peruvianCellOrNull(null))
        assertNull(peruvianCellOrNull(""))
        assertNull(peruvianCellOrNull("012345678"))
        assertNull(peruvianCellOrNull("98765432"))
        assertNull(peruvianCellOrNull("9876543210"))
    }

    @Test
    fun `the countdown is neutral, then amber at the warning, then red at 3 minutes`() {
        assertEquals(ExpiryLevel.NORMAL, expiryLevel(Duration.ofMinutes(11), warningMinutes = 10))
        assertEquals(ExpiryLevel.WARNING, expiryLevel(Duration.ofMinutes(10), warningMinutes = 10))
        assertEquals(ExpiryLevel.WARNING, expiryLevel(Duration.ofMinutes(8), warningMinutes = 10))
        assertEquals(ExpiryLevel.CRITICAL, expiryLevel(Duration.ofMinutes(3), warningMinutes = 10))
        assertEquals(ExpiryLevel.CRITICAL, expiryLevel(Duration.ZERO, warningMinutes = 10))
    }

    @Test
    fun `a backend warning shorter than the red step goes straight to red`() {
        assertEquals(ExpiryLevel.NORMAL, expiryLevel(Duration.ofMinutes(4), warningMinutes = 2))
        assertEquals(ExpiryLevel.CRITICAL, expiryLevel(Duration.ofMinutes(2), warningMinutes = 2))
    }
}
