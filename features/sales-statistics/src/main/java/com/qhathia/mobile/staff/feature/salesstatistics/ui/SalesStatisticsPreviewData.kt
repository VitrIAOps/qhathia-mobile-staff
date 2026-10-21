package com.qhathia.mobile.staff.feature.salesstatistics.ui

import com.qhathia.mobile.staff.core.domain.model.Category
import com.qhathia.mobile.staff.core.domain.model.SalesBucket
import com.qhathia.mobile.staff.core.domain.model.SalesKpis
import com.qhathia.mobile.staff.core.domain.model.SalesPeriod
import com.qhathia.mobile.staff.core.domain.model.SalesRanking
import com.qhathia.mobile.staff.core.domain.model.SalesSummary
import com.qhathia.mobile.staff.core.domain.model.StalledProduct
import com.qhathia.mobile.staff.core.domain.model.StoreActivity
import com.qhathia.mobile.staff.core.domain.model.TopSellingProduct
import com.qhathia.mobile.staff.feature.salesstatistics.model.SalesStatisticsUiState
import com.qhathia.mobile.staff.feature.salesstatistics.model.StatisticsTab
import com.qhathia.mobile.staff.feature.salesstatistics.model.TabContent
import kotlinx.collections.immutable.persistentListOf
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Fixed data behind the "Resumen de ventas" previews, using the figures of Figma nodes
 * `2238-30891`, `2238-31151` and `2238-31411` (the week of 24 – 30 March). Kept out of the tests,
 * which build their own fixtures from `core:testing`.
 */
internal object SalesStatisticsPreviewData {

    private val lima = ZoneId.of("America/Lima")

    private fun day(month: Int, dayOfMonth: Int): Instant =
        LocalDate.of(2026, month, dayOfMonth).atStartOfDay(lima).toInstant()

    private fun bucket(label: String, amount: Long) = SalesBucket(label, BigDecimal(amount))

    val summary = SalesSummary(
        period = SalesPeriod.WEEKLY,
        total = BigDecimal(5770),
        deltaPercent = 12.0,
        buckets = listOf(
            bucket("Sem 1", 1240),
            bucket("Sem 2", 1480),
            bucket("Sem 3", 1360),
            bucket("Sem 4", 1690),
        ),
        highlightedBucketIndex = 3,
        highlightedDeltaPercent = 24.3,
        generatedAt = day(3, 30),
        periodStart = day(3, 24),
        periodEnd = day(3, 31),
        kpis = SalesKpis(salesCount = 74, averageTicket = BigDecimal(78), unitsSold = 174),
    )

    val ranking = SalesRanking(
        periodStart = day(3, 24),
        periodEnd = day(3, 31),
        topSellersRevenue = BigDecimal(4090),
        topSellers = listOf(
            TopSellingProduct("p1", "Taladro percutor Bosch GSB", null, BigDecimal(1870), 34),
            TopSellingProduct("p2", "Taladro percutor Bosch GSB 13 RE", null, BigDecimal(1240), 120),
            TopSellingProduct("p3", "Juego de brocas Bosch HSS", null, BigDecimal(980), 1),
        ),
        immobilizedTotal = BigDecimal(4020),
        needsAttention = listOf(
            StalledProduct("p4", "Taladro percutor Bosch GSB", null, 34, BigDecimal(100), BigDecimal(3400)),
            StalledProduct("p5", "Foco LED 12W luz cálida", null, 10, BigDecimal(50), BigDecimal(500)),
            StalledProduct("p6", "Sierra caladora Stanley", null, 1, BigDecimal(120), BigDecimal(120)),
        ),
        // More products sold nothing than the card lists, so the "3 de los 17" caption shows.
        needsAttentionTotalCount = 17,
    )

    val emptyRanking = ranking.copy(
        topSellersRevenue = BigDecimal.ZERO,
        topSellers = emptyList(),
        immobilizedTotal = BigDecimal.ZERO,
        needsAttention = emptyList(),
        needsAttentionTotalCount = 0,
    )

    val activity = StoreActivity(
        periodStart = day(3, 24),
        periodEnd = day(3, 31),
        uniqueSessions = 63,
        uniqueSessionsDeltaPercent = 12.5,
        productViews = 148,
        productViewsDeltaPercent = -8.8,
        confirmedOrders = 9,
        confirmedOrdersDeltaPercent = 12.5,
    )

    val categories = persistentListOf(
        Category(id = "c1", name = "Herramientas"),
        Category(id = "c2", name = "Iluminación"),
    )

    val summaryState = SalesStatisticsUiState(
        period = SalesPeriod.WEEKLY,
        categories = categories,
        summary = TabContent(data = summary),
    )

    val filtersExpandedState = summaryState.copy(isFiltersExpanded = true)

    val rankingState = summaryState.copy(
        selectedTab = StatisticsTab.RANKING,
        ranking = TabContent(data = ranking),
    )

    val activityState = summaryState.copy(
        selectedTab = StatisticsTab.ACTIVITY,
        activity = TabContent(data = activity),
    )

    val loadingState = SalesStatisticsUiState(summary = TabContent(isLoading = true))

    val errorState = SalesStatisticsUiState(summary = TabContent(isError = true))
}
