package com.qhathia.mobile.staff.core.data.mapper

import com.qhathia.mobile.staff.core.domain.model.SalesPeriod
import com.qhathia.mobile.staff.core.network.dto.RestockDto
import com.qhathia.mobile.staff.core.network.dto.RestockItemDto
import com.qhathia.mobile.staff.core.network.dto.SalesDashboardDto
import com.qhathia.mobile.staff.core.network.dto.SalesPeriodDto
import com.qhathia.mobile.staff.core.network.dto.UnmetSearchExampleDto
import com.qhathia.mobile.staff.core.network.dto.UnmetSearchGroupDto
import com.qhathia.mobile.staff.core.network.dto.UnmetSearchesDto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant

class StoreInsightsMappersTest {

    private val now = Instant.parse("2026-09-13T16:50:00Z")

    private fun period(label: String, revenue: Double) = SalesPeriodDto(label = label, revenue = revenue)

    private fun salesDto(
        buckets: List<SalesPeriodDto> = listOf(
            period("Sem 10", 1240.0),
            period("Sem 11", 1480.0),
            period("Sem 12", 1360.0),
            period("Sem 13", 1690.0),
        ),
        current: SalesPeriodDto? = buckets.lastOrNull(),
        deltaPercent: Double? = 24.3,
    ) = SalesDashboardDto(
        granularity = "WEEKLY",
        current = current,
        deltaPercent = deltaPercent,
        buckets = buckets,
    )

    private fun assertMoney(expected: String, actual: BigDecimal) =
        assertEquals(0, BigDecimal(expected).compareTo(actual), "expected $expected, was $actual")

    @Test
    fun `the headline is the revenue of the period in progress, not the sum of the chart`() {
        val summary = salesDto().toSalesSummary(SalesPeriod.WEEKLY, now)

        assertMoney("1690", summary.total)
        assertEquals(24.3, summary.deltaPercent)
        assertEquals(SalesPeriod.WEEKLY, summary.period)
        assertEquals(now, summary.generatedAt)
        assertEquals(listOf("Sem 10", "Sem 11", "Sem 12", "Sem 13"), summary.buckets.map { it.label })
    }

    @Test
    fun `without current the headline falls back to the last bucket`() {
        val summary = salesDto(current = null).toSalesSummary(SalesPeriod.WEEKLY, now)

        assertMoney("1690", summary.total)
    }

    @Test
    fun `a missing comparison base stays null instead of becoming a number`() {
        val summary = salesDto(deltaPercent = null).toSalesSummary(SalesPeriod.WEEKLY, now)

        assertNull(summary.deltaPercent)
    }

    @Test
    fun `highlights the largest bucket and compares it with the one before`() {
        val summary = salesDto().toSalesSummary(SalesPeriod.WEEKLY, now)

        assertEquals(3, summary.highlightedBucketIndex)
        // 1690 against 1360.
        assertEquals(24.2647, summary.highlightedDeltaPercent!!, 0.0001)
    }

    @Test
    fun `a best bucket charted first has no comparison`() {
        val summary = salesDto(
            buckets = listOf(period("Sem 10", 900.0), period("Sem 11", 100.0), period("Sem 12", 200.0)),
        ).toSalesSummary(SalesPeriod.WEEKLY, now)

        assertEquals(0, summary.highlightedBucketIndex)
        assertNull(summary.highlightedDeltaPercent)
    }

    @Test
    fun `a best bucket after a bucket that sold nothing has no comparison`() {
        val summary = salesDto(
            buckets = listOf(period("Sem 12", 0.0), period("Sem 13", 500.0)),
        ).toSalesSummary(SalesPeriod.WEEKLY, now)

        assertEquals(1, summary.highlightedBucketIndex)
        assertNull(summary.highlightedDeltaPercent)
    }

    @Test
    fun `ties highlight the earliest bucket`() {
        val summary = salesDto(
            buckets = listOf(period("Sem 12", 700.0), period("Sem 13", 700.0)),
        ).toSalesSummary(SalesPeriod.WEEKLY, now)

        assertEquals(0, summary.highlightedBucketIndex)
    }

    @Test
    fun `a window that sold nothing highlights nothing`() {
        val summary = salesDto(
            buckets = listOf(period("9 h", 0.0), period("10 h", 0.0)),
            deltaPercent = null,
        ).toSalesSummary(SalesPeriod.HOURLY, now)

        assertEquals(-1, summary.highlightedBucketIndex)
        assertNull(summary.highlightedDeltaPercent)
        assertMoney("0", summary.total)
    }

    @Test
    fun `an unmet search is named after its most frequent raw text`() {
        val dto = UnmetSearchesDto(
            groups = listOf(
                UnmetSearchGroupDto(
                    term = "taladro",
                    personCount = 9,
                    searchCount = 14,
                    examples = listOf(
                        UnmetSearchExampleDto(rawText = "taladro para concreto", count = 4),
                        UnmetSearchExampleDto(rawText = "taladro percutor bosch", count = 5),
                    ),
                ),
            ),
        )

        val suggestion = dto.toMissingProductSuggestions().single()

        assertEquals("taladro", suggestion.id)
        assertEquals("Taladro percutor bosch", suggestion.name)
        assertEquals(9, suggestion.personCount)
    }

    @Test
    fun `an unmet search without usable examples falls back to its term`() {
        val dto = UnmetSearchesDto(
            groups = listOf(
                UnmetSearchGroupDto(
                    term = "amoladora",
                    personCount = 3,
                    examples = listOf(UnmetSearchExampleDto(rawText = "  ", count = 2)),
                ),
            ),
        )

        assertEquals("Amoladora", dto.toMissingProductSuggestions().single().name)
    }

    @Test
    fun `blank terms are dropped and repeated terms kept once`() {
        val dto = UnmetSearchesDto(
            groups = listOf(
                UnmetSearchGroupDto(term = "taladro", personCount = 9),
                UnmetSearchGroupDto(term = " ", personCount = 5),
                UnmetSearchGroupDto(term = "taladro", personCount = 2),
            ),
        )

        val suggestions = dto.toMissingProductSuggestions()

        assertEquals(listOf("taladro"), suggestions.map { it.id })
        assertEquals(9, suggestions.single().personCount)
    }

    @Test
    fun `restock items keep their product id and stock`() {
        val dto = RestockDto(
            threshold = 5,
            items = listOf(
                RestockItemDto(productId = "p-1", name = "Foco LED", imageUrl = "https://cdn/p1.jpg", stock = 1),
                RestockItemDto(productId = "p-2", name = "Cinta métrica", imageUrl = " ", stock = 3),
                RestockItemDto(productId = "p-1", name = "Foco LED", stock = 1),
                RestockItemDto(productId = "", name = "Sin id", stock = 2),
            ),
        )

        val suggestions = dto.toRestockSuggestions()

        assertEquals(listOf("p-1", "p-2"), suggestions.map { it.id })
        assertEquals(listOf(1, 3), suggestions.map { it.remainingStock })
        assertEquals("https://cdn/p1.jpg", suggestions[0].imageUrl)
        assertNull(suggestions[1].imageUrl)
    }
}
