package com.qhathia.mobile.staff.core.ui.format

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.Locale

class MoneyFormatTest {

    @Test
    fun `a price has two decimals, a point and thousands commas`() {
        assertEquals("S/ 12,345,678.50", 12345678.5.toPenPrice())
        assertEquals("S/ 189.90", 189.9.toPenPrice())
        assertEquals("S/ 149.00", 149.0.toPenPrice())
        assertEquals("S/ 1,234.50", BigDecimal("1234.5").toPenPrice())
    }

    @Test
    fun `a price rounds half up`() {
        assertEquals("S/ 0.01", 0.005.toPenPrice())
        assertEquals("S/ 2.68", 2.675.toPenPrice())
    }

    @Test
    fun `a large price is never in scientific notation`() {
        assertFalse(123456789.12.toPenPrice().contains('E'))
    }

    @Test
    fun `an amount is in whole soles with thousands commas`() {
        assertEquals("S/ 2,849", BigDecimal("2848.5").toPenAmount())
        assertEquals("S/ 5,770", BigDecimal("5770").toPenAmount())
    }

    @Test
    fun `the default locale does not change the separators`() {
        val previous = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("es-ES"))
            assertEquals("S/ 12,345,678.50", 12345678.5.toPenPrice())
            assertEquals("S/ 2,849", BigDecimal("2848.5").toPenAmount())
        } finally {
            Locale.setDefault(previous)
        }
    }
}
