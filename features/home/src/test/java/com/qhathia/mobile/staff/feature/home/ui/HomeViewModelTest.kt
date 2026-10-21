package com.qhathia.mobile.staff.feature.home.ui

import app.cash.turbine.Event
import app.cash.turbine.test
import com.qhathia.mobile.staff.core.domain.model.SalesPeriod
import com.qhathia.mobile.staff.core.domain.usecase.GetSalesSuggestionsUseCase
import com.qhathia.mobile.staff.core.domain.usecase.GetSalesSummaryUseCase
import com.qhathia.mobile.staff.core.domain.usecase.ObservePendingOrderCountUseCase
import com.qhathia.mobile.staff.core.domain.usecase.ObserveSessionUseCase
import com.qhathia.mobile.staff.core.domain.usecase.RefreshPendingOrderCountUseCase
import com.qhathia.mobile.staff.core.testing.FakeOrderRepository
import com.qhathia.mobile.staff.core.testing.FakeSessionRepository
import com.qhathia.mobile.staff.core.testing.FakeStoreInsightsRepository
import com.qhathia.mobile.staff.core.testing.MainDispatcherExtension
import com.qhathia.mobile.staff.core.testing.TestFixtures
import com.qhathia.mobile.staff.feature.home.model.HomeUiEffect
import com.qhathia.mobile.staff.feature.home.model.HomeUiEvent
import com.qhathia.mobile.staff.feature.home.model.HomeUiState
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
import java.math.BigDecimal

class HomeViewModelTest {

    @JvmField
    @RegisterExtension
    val mainDispatcher = MainDispatcherExtension()

    private lateinit var insights: FakeStoreInsightsRepository
    private lateinit var orders: FakeOrderRepository
    private lateinit var session: FakeSessionRepository

    @BeforeEach
    fun setUp() {
        insights = FakeStoreInsightsRepository()
        orders = FakeOrderRepository()
        session = FakeSessionRepository(TestFixtures.session())
    }

    private fun createViewModel() = HomeViewModel(
        getSalesSummaryUseCase = GetSalesSummaryUseCase(insights),
        getSalesSuggestionsUseCase = GetSalesSuggestionsUseCase(insights),
        refreshPendingOrderCountUseCase = RefreshPendingOrderCountUseCase(orders),
        observeSessionUseCase = ObserveSessionUseCase(session),
        observePendingOrderCountUseCase = ObservePendingOrderCountUseCase(orders),
    )

    @Test
    fun `opens on the weekly period and loads it`() = runTest {
        val viewModel = createViewModel()
        runCurrent()

        val state = viewModel.uiState.value
        assertEquals(SalesPeriod.WEEKLY, state.selectedPeriod)
        assertEquals(listOf(SalesPeriod.WEEKLY), insights.requestedPeriods)
        assertNotNull(state.summary)
        assertFalse(state.isLoading)
        assertFalse(state.isError)
    }

    @Test
    fun `selecting a period reloads the card for it`() = runTest {
        val viewModel = createViewModel()
        runCurrent()

        viewModel.onEvent(HomeUiEvent.SelectPeriod(SalesPeriod.MONTHLY))
        runCurrent()

        assertEquals(SalesPeriod.MONTHLY, viewModel.uiState.value.selectedPeriod)
        assertEquals(SalesPeriod.MONTHLY, viewModel.uiState.value.summary?.period)
        assertEquals(listOf(SalesPeriod.WEEKLY, SalesPeriod.MONTHLY), insights.requestedPeriods)
    }

    @Test
    fun `the selected period survives a refresh`() = runTest {
        val viewModel = createViewModel()
        runCurrent()
        viewModel.onEvent(HomeUiEvent.SelectPeriod(SalesPeriod.DAILY))
        runCurrent()

        viewModel.onEvent(HomeUiEvent.Refresh)
        runCurrent()

        assertEquals(SalesPeriod.DAILY, viewModel.uiState.value.selectedPeriod)
        assertEquals(
            listOf(SalesPeriod.WEEKLY, SalesPeriod.DAILY, SalesPeriod.DAILY),
            insights.requestedPeriods,
        )
    }

    @Test
    fun `the first screen start does not load again`() = runTest {
        val viewModel = createViewModel()
        runCurrent()

        viewModel.onEvent(HomeUiEvent.ScreenStarted)
        runCurrent()

        assertEquals(listOf(SalesPeriod.WEEKLY), insights.requestedPeriods)
        assertEquals(1, insights.suggestionsCallCount)
        assertEquals(1, orders.refreshPendingCountCallCount)
    }

    @Test
    fun `returning to the screen reloads everything for the selected period`() = runTest {
        val viewModel = createViewModel()
        runCurrent()
        viewModel.onEvent(HomeUiEvent.ScreenStarted)
        viewModel.onEvent(HomeUiEvent.SelectPeriod(SalesPeriod.MONTHLY))
        runCurrent()

        viewModel.onEvent(HomeUiEvent.ScreenStarted)
        runCurrent()

        assertEquals(SalesPeriod.MONTHLY, viewModel.uiState.value.selectedPeriod)
        assertEquals(
            listOf(SalesPeriod.WEEKLY, SalesPeriod.MONTHLY, SalesPeriod.MONTHLY),
            insights.requestedPeriods,
        )
        assertEquals(3, insights.suggestionsCallCount)
        assertEquals(3, orders.refreshPendingCountCallCount)
    }

    @Test
    fun `a reload on return never clears the summary on screen`() = runTest {
        val viewModel = createViewModel()
        runCurrent()
        viewModel.onEvent(HomeUiEvent.ScreenStarted)
        val loaded = viewModel.uiState.value

        viewModel.uiState.test {
            skipItems(1)
            viewModel.onEvent(HomeUiEvent.ScreenStarted)
            runCurrent()
            val states = cancelAndConsumeRemainingEvents()
                .filterIsInstance<Event.Item<HomeUiState>>()
                .map { it.value }
            assertTrue(states.isNotEmpty())
            assertTrue(states.all { it.summary != null && !it.isInitialLoading })
        }
        assertEquals(loaded.summary, viewModel.uiState.value.summary)
    }

    @Test
    fun `a failed reload on return keeps the previous data`() = runTest {
        val viewModel = createViewModel()
        runCurrent()
        viewModel.onEvent(HomeUiEvent.ScreenStarted)
        val loaded = viewModel.uiState.value

        insights.failEverything(IOException("offline"))
        viewModel.onEvent(HomeUiEvent.ScreenStarted)
        runCurrent()

        val stale = viewModel.uiState.value
        assertTrue(stale.isError)
        assertEquals(loaded.summary, stale.summary)
        assertEquals(loaded.lastUpdatedAt, stale.lastUpdatedAt)
        assertEquals(loaded.restockProducts, stale.restockProducts)
    }

    @Test
    fun `a successful load stamps the freshness time from the summary`() = runTest {
        val viewModel = createViewModel()
        runCurrent()

        assertEquals(TestFixtures.NOW, viewModel.uiState.value.lastUpdatedAt)
    }

    @Test
    fun `nothing is stamped before the first successful load`() = runTest {
        insights.failEverything()

        val viewModel = createViewModel()
        runCurrent()

        assertNull(viewModel.uiState.value.lastUpdatedAt)
        assertNull(viewModel.uiState.value.summary)
        assertTrue(viewModel.uiState.value.isError)
    }

    @Test
    fun `a failed refresh keeps the previous summary and its freshness time on screen`() = runTest {
        val viewModel = createViewModel()
        runCurrent()
        val loaded = viewModel.uiState.value
        assertNotNull(loaded.summary)

        insights.failEverything(IOException("offline"))
        viewModel.onEvent(HomeUiEvent.Refresh)
        runCurrent()

        val stale = viewModel.uiState.value
        assertTrue(stale.isError)
        assertFalse(stale.isLoading)
        assertEquals(loaded.summary, stale.summary)
        assertEquals(loaded.lastUpdatedAt, stale.lastUpdatedAt)
        assertEquals(loaded.missingProducts, stale.missingProducts)
        assertEquals(loaded.restockProducts, stale.restockProducts)
    }

    @Test
    fun `retrying after a failure re-requests the selected period`() = runTest {
        insights.failEverything()
        val viewModel = createViewModel()
        runCurrent()
        viewModel.onEvent(HomeUiEvent.SelectPeriod(SalesPeriod.HOURLY))
        runCurrent()

        insights.defaultSummaryResult = { period ->
            Result.success(TestFixtures.salesSummary(period = period))
        }
        insights.suggestionsResult = Result.success(TestFixtures.storeSuggestions())
        viewModel.onEvent(HomeUiEvent.Refresh)
        runCurrent()

        val state = viewModel.uiState.value
        assertFalse(state.isError)
        assertEquals(SalesPeriod.HOURLY, state.summary?.period)
    }

    @Test
    fun `the suggestion rows come from the port`() = runTest {
        val viewModel = createViewModel()
        runCurrent()

        val state = viewModel.uiState.value
        assertEquals(listOf(TestFixtures.missingProductSuggestion()), state.missingProducts.toList())
        assertEquals(listOf(TestFixtures.restockSuggestion()), state.restockProducts.toList())
    }

    @Test
    fun `the greeting takes the session's first name`() = runTest {
        session = FakeSessionRepository(
            TestFixtures.session(user = TestFixtures.user(fullName = "Maria Lopez")),
        )

        val viewModel = createViewModel()
        runCurrent()

        assertEquals("Maria", viewModel.uiState.value.merchantFirstName)
    }

    @Test
    fun `the greeting degrades to no name when there is no session`() = runTest {
        session = FakeSessionRepository(initial = null)

        val viewModel = createViewModel()
        runCurrent()

        assertNull(viewModel.uiState.value.merchantFirstName)
    }

    @Test
    fun `the badge count follows the shared pending-order flow`() = runTest {
        orders.pendingCountResult = Result.success(2)

        val viewModel = createViewModel()
        runCurrent()

        assertEquals(2, viewModel.uiState.value.pendingOrderCount)
        assertEquals(1, orders.refreshPendingCountCallCount)
    }

    @Test
    fun `each load refreshes the pending count alongside it`() = runTest {
        val viewModel = createViewModel()
        runCurrent()

        viewModel.onEvent(HomeUiEvent.Refresh)
        runCurrent()

        assertEquals(2, orders.refreshPendingCountCallCount)
    }

    @Test
    fun `the bell asks the shell to switch to Solicitudes`() = runTest {
        val viewModel = createViewModel()
        runCurrent()

        viewModel.uiEffect.test {
            viewModel.onEvent(HomeUiEvent.OpenNotifications)
            assertEquals(HomeUiEffect.NavigateToOrderRequests, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `Ver más opens the sales statistics on the selected period`() = runTest {
        val viewModel = createViewModel()
        runCurrent()
        viewModel.onEvent(HomeUiEvent.SelectPeriod(SalesPeriod.DAILY))
        runCurrent()

        viewModel.uiEffect.test {
            viewModel.onEvent(HomeUiEvent.OpenSalesStatistics)
            assertEquals(HomeUiEffect.NavigateToSalesStatistics(SalesPeriod.DAILY), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `a period that sold nothing still loads without an error`() = runTest {
        insights.setSummary(
            SalesPeriod.HOURLY,
            Result.success(
                TestFixtures.salesSummary(
                    period = SalesPeriod.HOURLY,
                    deltaPercent = null,
                    highlightedDeltaPercent = null,
                    buckets = emptyList(),
                    total = BigDecimal.ZERO,
                    highlightedBucketIndex = -1,
                ),
            ),
        )
        val viewModel = createViewModel()
        runCurrent()

        viewModel.onEvent(HomeUiEvent.SelectPeriod(SalesPeriod.HOURLY))
        runCurrent()

        val state = viewModel.uiState.value
        assertFalse(state.isError)
        assertEquals(BigDecimal.ZERO, state.summary?.total)
    }
}
