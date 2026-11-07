package com.qhathia.mobile.staff.feature.salesstatistics.ui

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.qhathia.mobile.staff.core.domain.model.SalesPeriod
import com.qhathia.mobile.staff.core.domain.usecase.GetCategoriesUseCase
import com.qhathia.mobile.staff.core.domain.usecase.GetSalesRankingUseCase
import com.qhathia.mobile.staff.core.domain.usecase.GetSalesSummaryUseCase
import com.qhathia.mobile.staff.core.domain.usecase.GetStoreActivityUseCase
import com.qhathia.mobile.staff.core.testing.FakeCategoryRepository
import com.qhathia.mobile.staff.core.testing.FakeStoreInsightsRepository
import com.qhathia.mobile.staff.core.testing.FakeStoreInsightsRepository.Request
import com.qhathia.mobile.staff.core.testing.MainDispatcherExtension
import com.qhathia.mobile.staff.core.testing.TestFixtures
import com.qhathia.mobile.staff.feature.salesstatistics.model.SalesStatisticsUiEffect
import com.qhathia.mobile.staff.feature.salesstatistics.model.SalesStatisticsUiEvent
import com.qhathia.mobile.staff.feature.salesstatistics.model.StatisticsTab
import com.qhathia.mobile.staff.feature.salesstatistics.navigation.SALES_STATISTICS_ARG_PERIOD
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import java.io.IOException

class SalesStatisticsViewModelTest {

    @JvmField
    @RegisterExtension
    val mainDispatcher = MainDispatcherExtension()

    private lateinit var insights: FakeStoreInsightsRepository
    private lateinit var categories: FakeCategoryRepository

    @BeforeEach
    fun setUp() {
        insights = FakeStoreInsightsRepository()
        categories = FakeCategoryRepository()
    }

    private fun createViewModel(period: String? = SalesPeriod.WEEKLY.name) = SalesStatisticsViewModel(
        savedStateHandle = SavedStateHandle(
            period?.let { mapOf(SALES_STATISTICS_ARG_PERIOD to it) } ?: emptyMap(),
        ),
        getSalesSummaryUseCase = GetSalesSummaryUseCase(insights),
        getSalesRankingUseCase = GetSalesRankingUseCase(insights),
        getStoreActivityUseCase = GetStoreActivityUseCase(insights),
        getCategoriesUseCase = GetCategoriesUseCase(categories),
    )

    @Test
    fun `opens on the period from the route and loads the summary first`() = runTest {
        val viewModel = createViewModel(period = SalesPeriod.MONTHLY.name)
        runCurrent()

        val state = viewModel.uiState.value
        assertEquals(SalesPeriod.MONTHLY, state.period)
        assertEquals(StatisticsTab.SUMMARY, state.selectedTab)
        assertNotNull(state.summary.data)
        assertEquals(listOf(Request(SalesPeriod.MONTHLY, null)), insights.summaryRequests)
        assertTrue(insights.rankingRequests.isEmpty())
        assertTrue(insights.activityRequests.isEmpty())
    }

    @Test
    fun `an unknown or missing period falls back to weekly`() = runTest {
        assertEquals(SalesPeriod.WEEKLY, createViewModel(period = "YEARLY").uiState.value.period)
        assertEquals(SalesPeriod.WEEKLY, createViewModel(period = null).uiState.value.period)
    }

    @Test
    fun `a tab loads on first selection and not again for the same filters`() = runTest {
        val viewModel = createViewModel()
        runCurrent()

        viewModel.onEvent(SalesStatisticsUiEvent.SelectTab(StatisticsTab.RANKING))
        runCurrent()
        viewModel.onEvent(SalesStatisticsUiEvent.SelectTab(StatisticsTab.SUMMARY))
        runCurrent()
        viewModel.onEvent(SalesStatisticsUiEvent.SelectTab(StatisticsTab.RANKING))
        runCurrent()

        assertEquals(1, insights.summaryRequests.size)
        assertEquals(1, insights.rankingRequests.size)
        assertNotNull(viewModel.uiState.value.ranking.data)
    }

    @Test
    fun `changing the period reloads the active tab with the selected category`() = runTest {
        val viewModel = createViewModel()
        runCurrent()
        viewModel.onEvent(SalesStatisticsUiEvent.SelectTab(StatisticsTab.RANKING))
        viewModel.onEvent(SalesStatisticsUiEvent.SelectCategory("category-1"))
        runCurrent()

        viewModel.onEvent(SalesStatisticsUiEvent.SelectPeriod(SalesPeriod.DAILY))
        runCurrent()

        assertEquals(Request(SalesPeriod.DAILY, "category-1"), insights.rankingRequests.last())
    }

    @Test
    fun `the category filters the summary request`() = runTest {
        val viewModel = createViewModel()
        runCurrent()

        viewModel.onEvent(SalesStatisticsUiEvent.SelectCategory("category-1"))
        runCurrent()

        assertEquals(Request(SalesPeriod.WEEKLY, "category-1"), insights.summaryRequests.last())
    }

    @Test
    fun `changing the category on Actividad issues no request`() = runTest {
        val viewModel = createViewModel()
        runCurrent()
        viewModel.onEvent(SalesStatisticsUiEvent.SelectTab(StatisticsTab.ACTIVITY))
        runCurrent()
        assertEquals(listOf(SalesPeriod.WEEKLY), insights.activityRequests)

        viewModel.onEvent(SalesStatisticsUiEvent.SelectCategory("category-1"))
        runCurrent()

        assertEquals(listOf(SalesPeriod.WEEKLY), insights.activityRequests)
        assertFalse(viewModel.uiState.value.isCategoryFilterVisible)
        assertEquals("category-1", viewModel.uiState.value.selectedCategoryId)
    }

    @Test
    fun `a failed retry for the same filters keeps the data on screen`() = runTest {
        val viewModel = createViewModel()
        runCurrent()
        val loaded = viewModel.uiState.value.summary.data
        assertNotNull(loaded)

        insights.defaultSummaryResult = { Result.failure(IOException("offline")) }
        viewModel.onEvent(SalesStatisticsUiEvent.Retry)
        runCurrent()

        val summary = viewModel.uiState.value.summary
        assertEquals(loaded, summary.data)
        assertTrue(summary.isError)
        assertFalse(summary.isLoading)
    }

    @Test
    fun `a filter change never leaves the previous filters' figures on screen`() = runTest {
        val viewModel = createViewModel()
        runCurrent()
        assertNotNull(viewModel.uiState.value.summary.data)

        // If the new period fails, the weekly figures must not survive under "Mensual".
        insights.setSummary(SalesPeriod.MONTHLY, Result.failure(IOException("offline")))
        viewModel.onEvent(SalesStatisticsUiEvent.SelectPeriod(SalesPeriod.MONTHLY))
        runCurrent()

        val summary = viewModel.uiState.value.summary
        assertNull(summary.data)
        assertTrue(summary.isError)
        assertFalse(summary.isLoading)
    }

    @Test
    fun `a first load failure shows the error and retry loads again`() = runTest {
        insights.failEverything()
        val viewModel = createViewModel()
        runCurrent()
        assertTrue(viewModel.uiState.value.summary.isError)

        insights.defaultSummaryResult = { Result.success(TestFixtures.salesSummary(period = it)) }
        viewModel.onEvent(SalesStatisticsUiEvent.Retry)
        runCurrent()

        val summary = viewModel.uiState.value.summary
        assertNotNull(summary.data)
        assertFalse(summary.isError)
        assertEquals(2, insights.summaryRequests.size)
    }

    @Test
    fun `categories load once and a failure leaves only Todos without an error`() = runTest {
        categories.categoriesResult = Result.failure(IOException("offline"))

        val viewModel = createViewModel()
        runCurrent()

        assertTrue(viewModel.uiState.value.categories.isEmpty())
        assertFalse(viewModel.uiState.value.summary.isError)
        assertEquals(1, categories.callCount)
    }

    @Test
    fun `toggling the filters expands and collapses the panel`() = runTest {
        val viewModel = createViewModel()

        viewModel.onEvent(SalesStatisticsUiEvent.ToggleFilters)
        assertTrue(viewModel.uiState.value.isFiltersExpanded)

        viewModel.onEvent(SalesStatisticsUiEvent.ToggleFilters)
        assertFalse(viewModel.uiState.value.isFiltersExpanded)
    }

    @Test
    fun `back asks the shell to leave the screen`() = runTest {
        val viewModel = createViewModel()
        runCurrent()

        viewModel.uiEffect.test {
            viewModel.onEvent(SalesStatisticsUiEvent.Back)
            assertEquals(SalesStatisticsUiEffect.NavigateBack, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `back twice leaves once`() = runTest {
        val viewModel = createViewModel()
        runCurrent()

        viewModel.uiEffect.test {
            viewModel.onEvent(SalesStatisticsUiEvent.Back)
            viewModel.onEvent(SalesStatisticsUiEvent.Back)
            assertEquals(SalesStatisticsUiEffect.NavigateBack, awaitItem())
            expectNoEvents()
        }
    }

    @Test
    fun `share on a loaded tab asks the screen to share that tab`() = runTest {
        val viewModel = createViewModel()
        runCurrent()

        viewModel.uiEffect.test {
            viewModel.onEvent(SalesStatisticsUiEvent.Share)
            assertEquals(SalesStatisticsUiEffect.Share(StatisticsTab.SUMMARY), awaitItem())
        }
    }

    @Test
    fun `share with nothing loaded emits nothing`() = runTest {
        insights.failEverything(IOException("offline"))
        val viewModel = createViewModel()
        runCurrent()
        assertFalse(viewModel.uiState.value.isShareEnabled)

        viewModel.uiEffect.test {
            viewModel.onEvent(SalesStatisticsUiEvent.Share)
            expectNoEvents()
        }
    }

    @Test
    fun `the first screen start loads nothing extra`() = runTest {
        val viewModel = createViewModel()
        runCurrent()

        viewModel.onEvent(SalesStatisticsUiEvent.ScreenStarted)
        runCurrent()

        assertEquals(1, insights.summaryRequests.size)
    }

    @Test
    fun `coming back reloads the tab on screen and keeps its figures meanwhile`() = runTest {
        val viewModel = createViewModel()
        runCurrent()
        viewModel.onEvent(SalesStatisticsUiEvent.SelectTab(StatisticsTab.RANKING))
        runCurrent()
        viewModel.onEvent(SalesStatisticsUiEvent.ScreenStarted)
        val shown = viewModel.uiState.value.ranking.data

        viewModel.onEvent(SalesStatisticsUiEvent.ScreenStarted)

        assertEquals(shown, viewModel.uiState.value.ranking.data)
        runCurrent()
        assertEquals(2, insights.rankingRequests.size)
        assertNotNull(viewModel.uiState.value.ranking.data)
    }

    @Test
    fun `after coming back the other tabs reload when shown`() = runTest {
        val viewModel = createViewModel()
        runCurrent()
        viewModel.onEvent(SalesStatisticsUiEvent.SelectTab(StatisticsTab.RANKING))
        runCurrent()
        viewModel.onEvent(SalesStatisticsUiEvent.ScreenStarted)
        viewModel.onEvent(SalesStatisticsUiEvent.ScreenStarted)
        runCurrent()

        viewModel.onEvent(SalesStatisticsUiEvent.SelectTab(StatisticsTab.SUMMARY))
        runCurrent()

        assertEquals(2, insights.summaryRequests.size)
    }
}
