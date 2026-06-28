package com.qhathia.mobile.staff.core.common.validation

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class EmailValidatorTest {

    @Test
    fun `accepts a well formed email`() {
        assertTrue(isValidEmail("owner@tienda.com"))
        assertTrue(isValidEmail("  owner@tienda.com  "))
    }

    @Test
    fun `rejects malformed emails`() {
        assertFalse(isValidEmail(""))
        assertFalse(isValidEmail("owner"))
        assertFalse(isValidEmail("owner@"))
        assertFalse(isValidEmail("owner@tienda"))
        assertFalse(isValidEmail("@tienda.com"))
    }
}
