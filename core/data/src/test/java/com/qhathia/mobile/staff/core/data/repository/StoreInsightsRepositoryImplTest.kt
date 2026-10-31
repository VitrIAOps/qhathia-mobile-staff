package com.qhathia.mobile.staff.core.data.repository

import com.qhathia.mobile.staff.core.domain.model.SalesPeriod
import com.qhathia.mobile.staff.core.network.NetworkResult
import com.qhathia.mobile.staff.core.network.dto.RestockDto
import com.qhathia.mobile.staff.core.network.dto.RestockItemDto
import com.qhathia.mobile.staff.core.network.dto.SalesDashboardDto
import com.qhathia.mobile.staff.core.network.dto.SalesPeriodDto
import com.qhathia.mobile.staff.core.network.dto.SalesRankingDto
import com.qhathia.mobile.staff.core.network.dto.StoreActivityDto
import com.qhathia.mobile.staff.core.network.dto.TopSellerItemDto
import com.qhathia.mobile.staff.core.network.dto.TopSellersDto
import com.qhathia.mobile.staff.core.network.dto.UnmetSearchGroupDto
import com.qhathia.mobile.staff.core.network.dto.UnmetSearchesDto
import com.qhathia.mobile.staff.core.network.service.DashboardApiService
import com.qhathia.mobile.staff.core.network.service.InsightsApiService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.IOException
import java.math.BigDecimal
import java.time.Instant

class StoreInsightsRepositoryImplTest {

    private val dashboard = mockk<DashboardApiService>()
    private val insights = mockk<InsightsApiService>()
    private val repository = StoreInsightsRepositoryImpl(dashboard, insights)

    private val restock = RestockDto(items = listOf(RestockItemDto(productId = "p-1", name = "Foco", stock = 1)))
    private val unmet = UnmetSearchesDto(
        groups = listOf(UnmetSearchGroupDto(term = "taladro", personCount = 9)),
        otherRequests = listOf(UnmetSearchGroupDto(term = "pintura", personCount = 3)),
    )

    @Test
    fun `every period asks for its own granularity and four buckets`() = runTest {
        coEvery { dashboard.getSales(any(), any(), any()) } returns NetworkResult.Success(SalesDashboardDto())

        SalesPeriod.entries.forEach { repository.getSalesSummary(it) }

        SalesPeriod.entries.forEach { period ->
            coVerify(exactly = 1) { dashboard.getSales(period.name, 4, null) }
        }
    }

    @Test
    fun `a category is passed to the sales request`() = runTest {
        coEvery { dashboard.getSales(any(), any(), any()) } returns NetworkResult.Success(SalesDashboardDto())

        repository.getSalesSummary(SalesPeriod.WEEKLY, categoryId = "category-1")

        coVerify(exactly = 1) { dashboard.getSales("WEEKLY", 4, "category-1") }
    }

    @Test
    fun `a successful sales response maps to the summary and stamps the load time`() = runTest {
        val before = Instant.now()
        val inProgress = SalesPeriodDto(label = "Sem 13", revenue = 1690.0)
        coEvery { dashboard.getSales("WEEKLY", 4, null) } returns NetworkResult.Success(
            SalesDashboardDto(current = inProgress, deltaPercent = 24.3, buckets = listOf(inProgress)),
        )

        val summary = repository.getSalesSummary(SalesPeriod.WEEKLY).getOrThrow()

        assertEquals(0, BigDecimal("1690").compareTo(summary.total))
        assertEquals(24.3, summary.deltaPercent)
        assertFalse(summary.generatedAt.isBefore(before))
    }

    @Test
    fun `an HTTP error fails the summary`() = runTest {
        coEvery { dashboard.getSales(any(), any(), any()) } returns NetworkResult.HttpError(403, "Forbidden")

        assertTrue(repository.getSalesSummary(SalesPeriod.WEEKLY).isFailure)
    }

    @Test
    fun `a transport error fails the summary with its cause`() = runTest {
        val cause = IOException("offline")
        coEvery { dashboard.getSales(any(), any(), any()) } returns NetworkResult.NetworkError(cause)

        assertSame(cause, repository.getSalesSummary(SalesPeriod.WEEKLY).exceptionOrNull())
    }

    @Test
    fun `suggestions combine unmet searches and restock`() = runTest {
        coEvery { insights.getUnmetSearches(any(), any()) } returns NetworkResult.Success(unmet)
        coEvery { dashboard.getRestock(any()) } returns NetworkResult.Success(restock)

        val suggestions = repository.getSuggestions().getOrThrow()

        assertEquals(listOf("taladro"), suggestions.missingProducts.map { it.id })
        assertEquals(listOf("p-1"), suggestions.restockProducts.map { it.id })
        assertEquals(listOf("pintura"), suggestions.otherRequests.map { it.id })
    }

    @Test
    fun `suggestions fail when unmet searches fail`() = runTest {
        coEvery { insights.getUnmetSearches(any(), any()) } returns NetworkResult.HttpError(503, "Unavailable")
        coEvery { dashboard.getRestock(any()) } returns NetworkResult.Success(restock)

        assertTrue(repository.getSuggestions().isFailure)
    }

    @Test
    fun `suggestions fail when restock fails`() = runTest {
        val cause = IOException("offline")
        coEvery { insights.getUnmetSearches(any(), any()) } returns NetworkResult.Success(unmet)
        coEvery { dashboard.getRestock(any()) } returns NetworkResult.NetworkError(cause)

        assertSame(cause, repository.getSuggestions().exceptionOrNull())
    }

    @Test
    fun `the ranking is requested with its period and category and mapped`() = runTest {
        coEvery { dashboard.getRanking("DAILY", "category-1") } returns NetworkResult.Success(
            SalesRankingDto(
                topSellers = TopSellersDto(
                    totalRevenue = 1870.0,
                    items = listOf(TopSellerItemDto(productId = "p-1", name = "Taladro", revenue = 1870.0, unitsSold = 34)),
                ),
            ),
        )

        val ranking = repository.getSalesRanking(SalesPeriod.DAILY, "category-1").getOrThrow()

        assertEquals(listOf("p-1"), ranking.topSellers.map { it.id })
        assertEquals(0, BigDecimal("1870").compareTo(ranking.topSellersRevenue))
    }

    @Test
    fun `ranking errors fail the result`() = runTest {
        val cause = IOException("offline")
        coEvery { dashboard.getRanking(any(), any()) } returns NetworkResult.NetworkError(cause)

        assertSame(cause, repository.getSalesRanking(SalesPeriod.WEEKLY, null).exceptionOrNull())

        coEvery { dashboard.getRanking(any(), any()) } returns NetworkResult.HttpError(500, "Server Error")

        assertTrue(repository.getSalesRanking(SalesPeriod.WEEKLY, null).isFailure)
    }

    @Test
    fun `activity is requested with its period only and mapped`() = runTest {
        coEvery { dashboard.getActivity("HOURLY") } returns NetworkResult.Success(
            StoreActivityDto(uniqueSessions = 63, productViews = 148),
        )

        val activity = repository.getStoreActivity(SalesPeriod.HOURLY).getOrThrow()

        assertEquals(63, activity.uniqueSessions)
        assertEquals(148, activity.productViews)
    }

    @Test
    fun `activity errors fail the result`() = runTest {
        val cause = IOException("offline")
        coEvery { dashboard.getActivity(any()) } returns NetworkResult.NetworkError(cause)

        assertSame(cause, repository.getStoreActivity(SalesPeriod.WEEKLY).exceptionOrNull())

        coEvery { dashboard.getActivity(any()) } returns NetworkResult.HttpError(403, "Forbidden")

        assertTrue(repository.getStoreActivity(SalesPeriod.WEEKLY).isFailure)
    }
}
