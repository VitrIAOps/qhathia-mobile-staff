package com.qhathia.mobile.staff.core.common.validation

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ProductValidatorsTest {

    @Test
    fun `name is valid when non-blank and within the length limit`() {
        assertTrue(isValidProductName("Lámpara"))
        assertFalse(isValidProductName(""))
        assertFalse(isValidProductName("   "))
        assertFalse(isValidProductName("x".repeat(MAX_PRODUCT_NAME_LENGTH + 1)))
    }

    @Test
    fun `price is valid when greater than zero with at most two decimals`() {
        assertEquals(89.9, parsePrice("89.90"))
        assertEquals(10.0, parsePrice("10"))
        assertEquals(5.5, parsePrice("5,5"))
        assertNull(parsePrice("0"))
        assertNull(parsePrice("-1"))
        assertNull(parsePrice("1.234"))
        assertNull(parsePrice("abc"))
        assertNull(parsePrice(""))
    }

    @Test
    fun `quantity is valid when a non-negative integer`() {
        assertEquals(0, parseQuantity("0"))
        assertEquals(12, parseQuantity("12"))
        assertNull(parseQuantity("-1"))
        assertNull(parseQuantity("3.5"))
        assertNull(parseQuantity(""))
        assertNull(parseQuantity("abc"))
        assertFalse(isValidQuantity("-3"))
        assertTrue(isValidQuantity("0"))
    }
}
