package com.qhathia.mobile.staff.feature.salesstatistics.ui

import com.qhathia.mobile.staff.core.testing.TestFixtures
import com.qhathia.mobile.staff.feature.salesstatistics.R
import com.qhathia.mobile.staff.feature.salesstatistics.model.SalesStatisticsUiState
import com.qhathia.mobile.staff.feature.salesstatistics.model.StatisticsTab
import com.qhathia.mobile.staff.feature.salesstatistics.model.TabContent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class StatisticsShareTest {

    /** Stands in for `Resources.getString`: the resource id, then its arguments. */
    private val text: (Int, Array<out Any>) -> String = { id, args -> "$id|${args.joinToString("|")}" }

    private fun share(tab: StatisticsTab, state: SalesStatisticsUiState) =
        shareText(tab, state, range = "2 – 8 de noviembre", text = text)

    @Test
    fun `the summary text has the range, the total and the KPIs`() {
        val summary = TestFixtures.salesSummary(total = BigDecimal(2385))
        val state = SalesStatisticsUiState(summary = TabContent(data = summary))

        val shared = share(StatisticsTab.SUMMARY, state)

        assertEquals(
            "${R.string.statistics_share_summary}|2 – 8 de noviembre|S/ 2,385|" +
                "${summary.kpis.salesCount}|S/ ${summary.kpis.averageTicket.toInt()}|${summary.kpis.unitsSold}",
            shared,
        )
    }

    @Test
    fun `the ranking text lists the top sellers with their revenue`() {
        val ranking = TestFixtures.salesRanking(
            topSellers = listOf(
                TestFixtures.topSellingProduct(id = "a", name = "Taladro", revenue = 1870),
                TestFixtures.topSellingProduct(id = "b", name = "Foco", revenue = 515),
            ),
        )
        val state = SalesStatisticsUiState(ranking = TabContent(data = ranking))

        val lines = share(StatisticsTab.RANKING, state)!!.lines()

        assertEquals("${R.string.statistics_share_ranking_header}|2 – 8 de noviembre", lines[0])
        assertEquals("${R.string.statistics_share_ranking_row}|1|Taladro|S/ 1,870", lines[1])
        assertEquals("${R.string.statistics_share_ranking_row}|2|Foco|S/ 515", lines[2])
    }

    @Test
    fun `the activity text has sessions, views and orders, or no orders on an older backend`() {
        val withOrders = SalesStatisticsUiState(
            activity = TabContent(data = TestFixtures.storeActivity(uniqueSessions = 63, productViews = 148).copy(confirmedOrders = 9)),
        )
        val withoutOrders = SalesStatisticsUiState(
            activity = TabContent(data = TestFixtures.storeActivity(uniqueSessions = 63, productViews = 148).copy(confirmedOrders = null)),
        )

        assertEquals("${R.string.statistics_share_activity}|2 – 8 de noviembre|63|148|9", share(StatisticsTab.ACTIVITY, withOrders))
        assertEquals(
            "${R.string.statistics_share_activity_no_orders}|2 – 8 de noviembre|63|148",
            share(StatisticsTab.ACTIVITY, withoutOrders),
        )
    }

    @Test
    fun `a tab with nothing loaded shares nothing`() {
        assertNull(share(StatisticsTab.RANKING, SalesStatisticsUiState()))
    }

    @Test
    fun `share is enabled only for a loaded tab that is not reloading`() {
        val summary = TestFixtures.salesSummary()

        assertFalse(SalesStatisticsUiState().isShareEnabled)
        assertFalse(SalesStatisticsUiState(summary = TabContent(data = summary, isLoading = true)).isShareEnabled)
        assertTrue(SalesStatisticsUiState(summary = TabContent(data = summary)).isShareEnabled)
        assertFalse(
            SalesStatisticsUiState(selectedTab = StatisticsTab.RANKING, summary = TabContent(data = summary)).isShareEnabled,
        )
    }
}
