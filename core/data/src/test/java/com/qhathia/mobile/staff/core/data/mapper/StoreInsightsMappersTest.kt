package com.qhathia.mobile.staff.core.data.mapper

import com.qhathia.mobile.staff.core.domain.model.SalesPeriod
import com.qhathia.mobile.staff.core.network.dto.RestockDto
import com.qhathia.mobile.staff.core.network.dto.RestockItemDto
import com.qhathia.mobile.staff.core.network.dto.SalesDashboardDto
import com.qhathia.mobile.staff.core.network.dto.SalesPeriodDto
import com.qhathia.mobile.staff.core.network.dto.NeedsAttentionDto
import com.qhathia.mobile.staff.core.network.dto.SalesRankingDto
import com.qhathia.mobile.staff.core.network.dto.StalledItemDto
import com.qhathia.mobile.staff.core.network.dto.StoreActivityDto
import com.qhathia.mobile.staff.core.network.dto.TopSellerItemDto
import com.qhathia.mobile.staff.core.network.dto.TopSellersDto
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
    fun `an unmet search is named after its canonical term, not its raw texts`() {
        val dto = UnmetSearchesDto(
            groups = listOf(
                UnmetSearchGroupDto(
                    term = "amoladora",
                    personCount = 9,
                    searchCount = 14,
                    examples = listOf(
                        UnmetSearchExampleDto(rawText = "quiero una amoladora", count = 5),
                        UnmetSearchExampleDto(rawText = "amoladora bosch", count = 4),
                    ),
                ),
            ),
        )

        val suggestion = dto.toMissingProductSuggestions().single()

        assertEquals("amoladora", suggestion.id)
        assertEquals("Amoladora", suggestion.name)
        assertEquals(9, suggestion.personCount)
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

    @Test
    fun `the summary carries the bounds and KPIs of the period in progress`() {
        val inProgress = SalesPeriodDto(
            label = "Sem 13",
            periodStart = "2026-03-24T05:00:00Z",
            periodEnd = "2026-03-31T05:00:00Z",
            revenue = 1690.0,
            salesCount = 24,
            averageTicket = 70.42,
            unitsSold = 55,
        )

        val summary = salesDto(buckets = listOf(inProgress)).toSalesSummary(SalesPeriod.WEEKLY, now)

        assertEquals(Instant.parse("2026-03-24T05:00:00Z"), summary.periodStart)
        assertEquals(Instant.parse("2026-03-31T05:00:00Z"), summary.periodEnd)
        assertEquals(24, summary.kpis.salesCount)
        assertMoney("70.42", summary.kpis.averageTicket)
        assertEquals(55, summary.kpis.unitsSold)
    }

    @Test
    fun `an unparseable period bound becomes null instead of failing the response`() {
        val inProgress = SalesPeriodDto(label = "Sem 13", periodStart = "not-a-date", revenue = 10.0)

        val summary = salesDto(buckets = listOf(inProgress)).toSalesSummary(SalesPeriod.WEEKLY, now)

        assertNull(summary.periodStart)
        assertNull(summary.periodEnd)
    }

    @Test
    fun `the ranking keeps its totals and drops repeated or blank products`() {
        val dto = SalesRankingDto(
            periodStart = "2026-03-24T05:00:00Z",
            topSellers = TopSellersDto(
                totalRevenue = 4090.0,
                items = listOf(
                    TopSellerItemDto(productId = "p1", name = "Taladro", imageUrl = " ", revenue = 1870.0, unitsSold = 34),
                    TopSellerItemDto(productId = "p1", name = "Taladro", revenue = 1870.0, unitsSold = 34),
                    TopSellerItemDto(productId = "", name = "Sin id", revenue = 5.0),
                ),
            ),
            needsAttention = NeedsAttentionDto(totalImmobilized = 620.0),
        )

        val ranking = dto.toSalesRanking()

        assertEquals(Instant.parse("2026-03-24T05:00:00Z"), ranking.periodStart)
        assertMoney("4090", ranking.topSellersRevenue)
        assertEquals(listOf("p1"), ranking.topSellers.map { it.id })
        assertNull(ranking.topSellers.single().imageUrl)
        assertEquals(34, ranking.topSellers.single().unitsSold)
        assertMoney("620", ranking.immobilizedTotal)
    }

    @Test
    fun `stalled stock is ordered by immobilized money, highest first`() {
        val dto = SalesRankingDto(
            needsAttention = NeedsAttentionDto(
                totalImmobilized = 770.0,
                items = listOf(
                    StalledItemDto(productId = "a", stock = 10, price = 5.0, immobilizedAmount = 50.0),
                    StalledItemDto(productId = "b", stock = 31, price = 20.0, immobilizedAmount = 620.0),
                    StalledItemDto(productId = "c", stock = 1, price = 100.0, immobilizedAmount = 100.0),
                ),
            ),
        )

        val needsAttention = dto.toSalesRanking().needsAttention

        assertEquals(listOf("b", "c", "a"), needsAttention.map { it.id })
        assertEquals(31, needsAttention.first().stock)
        assertMoney("20", needsAttention.first().price)
    }

    @Test
    fun `activity keeps its figures and a missing comparison base`() {
        val dto = StoreActivityDto(
            periodEnd = "2026-03-31T05:00:00Z",
            uniqueSessions = 63,
            uniqueSessionsDeltaPercent = 12.5,
            productViews = 0,
            productViewsDeltaPercent = null,
        )

        val activity = dto.toStoreActivity()

        assertEquals(Instant.parse("2026-03-31T05:00:00Z"), activity.periodEnd)
        assertEquals(63, activity.uniqueSessions)
        assertEquals(12.5, activity.uniqueSessionsDeltaPercent)
        assertEquals(0, activity.productViews)
        assertNull(activity.productViewsDeltaPercent)
    }

    @Test
    fun `activity maps confirmed orders and their variation`() {
        val activity = StoreActivityDto(confirmedOrders = 9, confirmedOrdersDeltaPercent = 12.5).toStoreActivity()

        assertEquals(9, activity.confirmedOrders)
        assertEquals(12.5, activity.confirmedOrdersDeltaPercent)
    }

    @Test
    fun `missing confirmed orders stay null instead of becoming zero`() {
        val activity = StoreActivityDto(uniqueSessions = 4).toStoreActivity()

        assertNull(activity.confirmedOrders)
        assertNull(activity.confirmedOrdersDeltaPercent)
    }

    @Test
    fun `zero confirmed orders are kept as a real zero`() {
        assertEquals(0, StoreActivityDto(confirmedOrders = 0).toStoreActivity().confirmedOrders)
    }

    @Test
    fun `a non-finite confirmed-orders variation becomes null`() {
        val activity = StoreActivityDto(confirmedOrders = 3, confirmedOrdersDeltaPercent = Double.NaN)
            .toStoreActivity()

        assertNull(activity.confirmedOrdersDeltaPercent)
    }

    @Test
    fun `the ranking keeps the total count of products without sales`() {
        val dto = SalesRankingDto(needsAttention = NeedsAttentionDto(totalImmobilized = 620.0, totalCount = 17))

        assertEquals(17, dto.toSalesRanking().needsAttentionTotalCount)
    }

    @Test
    fun `a missing or negative total count becomes null`() {
        assertNull(SalesRankingDto().toSalesRanking().needsAttentionTotalCount)
        assertNull(
            SalesRankingDto(needsAttention = NeedsAttentionDto(totalCount = -1)).toSalesRanking()
                .needsAttentionTotalCount,
        )
    }
}
