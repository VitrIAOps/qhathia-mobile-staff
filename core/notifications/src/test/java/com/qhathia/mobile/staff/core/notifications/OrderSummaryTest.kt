package com.qhathia.mobile.staff.core.notifications

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class OrderSummaryTest {

    private fun order(id: String) = TrayEntry(tag = id, id = 1, isSummary = false)

    @Test
    fun `summaries, ours or the system's, are not children`() {
        val entries = listOf(
            order("a"),
            order("b"),
            TrayEntry(tag = "orders_summary", id = 2, isSummary = true),
            TrayEntry(tag = null, id = 99, isSummary = true),
        )

        assertEquals(setOf("a", "b"), OrderSummary.childKeys(entries))
    }

    @Test
    fun `a notification without an order id counts by its id`() {
        val entries = listOf(order("a"), TrayEntry(tag = null, id = 42, isSummary = false))

        assertEquals(setOf("a", "#42"), OrderSummary.childKeys(entries))
    }

    @Test
    fun `including adds an order the tray does not show yet`() {
        assertEquals(setOf("a", "b"), OrderSummary.childKeys(listOf(order("a")), including = "b"))
    }

    @Test
    fun `including an order already shown counts it once`() {
        assertEquals(setOf("a"), OrderSummary.childKeys(listOf(order("a")), including = "a"))
    }

    @Test
    fun `excluding removes an order the tray still shows`() {
        assertEquals(setOf("b"), OrderSummary.childKeys(listOf(order("a"), order("b")), excluding = "a"))
    }

    @Test
    fun `the last order leaving gives no children`() {
        val entries = listOf(order("a"), TrayEntry(tag = "orders_summary", id = 2, isSummary = true))

        assertEquals(emptySet<String>(), OrderSummary.childKeys(entries, excluding = "a"))
    }

    @Test
    fun `an empty tray with excluding gives no children`() {
        assertEquals(emptySet<String>(), OrderSummary.childKeys(emptyList(), excluding = "a"))
    }
}
