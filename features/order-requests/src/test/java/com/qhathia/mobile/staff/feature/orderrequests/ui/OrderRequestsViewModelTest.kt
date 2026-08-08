package com.qhathia.mobile.staff.feature.orderrequests.ui

import app.cash.turbine.test
import com.qhathia.mobile.staff.core.domain.model.ConfirmOrderError
import com.qhathia.mobile.staff.core.domain.model.ConfirmOrderException
import com.qhathia.mobile.staff.core.domain.model.OrderPage
import com.qhathia.mobile.staff.core.domain.model.OrderStatus
import com.qhathia.mobile.staff.core.domain.model.RejectOrderError
import com.qhathia.mobile.staff.core.domain.model.RejectOrderException
import com.qhathia.mobile.staff.core.domain.usecase.ConfirmOrderUseCase
import com.qhathia.mobile.staff.core.domain.usecase.GetOrderDetailUseCase
import com.qhathia.mobile.staff.core.domain.usecase.GetOrdersUseCase
import com.qhathia.mobile.staff.core.domain.usecase.RejectOrderUseCase
import com.qhathia.mobile.staff.core.testing.FakeOrderRepository
import com.qhathia.mobile.staff.core.testing.MainDispatcherExtension
import com.qhathia.mobile.staff.core.testing.TestFixtures
import com.qhathia.mobile.staff.feature.orderrequests.model.OrderRequestsFilter
import com.qhathia.mobile.staff.feature.orderrequests.model.OrderRequestsUiEffect
import com.qhathia.mobile.staff.feature.orderrequests.model.OrderRequestsUiEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import java.io.IOException

/** Comfortably past the ViewModel's 350 ms search debounce. */
private const val PAST_DEBOUNCE_MS = 500L

class OrderRequestsViewModelTest {

    @JvmField
    @RegisterExtension
    val mainDispatcher = MainDispatcherExtension()

    private lateinit var repository: FakeOrderRepository

    @BeforeEach
    fun setUp() {
        repository = FakeOrderRepository()
    }

    private fun createViewModel() = OrderRequestsViewModel(
        getOrdersUseCase = GetOrdersUseCase(repository),
        getOrderDetailUseCase = GetOrderDetailUseCase(repository),
        confirmOrderUseCase = ConfirmOrderUseCase(repository),
        rejectOrderUseCase = RejectOrderUseCase(repository),
    )

    /** Answers the `size=1` badge query with [count], leaving the list query on its own result. */
    private fun badgeCountOf(count: Int) {
        repository.ordersResultFor = { call ->
            if (call.size == 1) {
                Result.success(OrderPage(orders = emptyList(), totalElements = count))
            } else {
                null
            }
        }
    }

    @Test
    fun `on init requests PENDING and shows it as the selected filter`() = runTest {
        val viewModel = createViewModel()
        runCurrent()

        assertEquals(listOf(OrderStatus.PENDING), repository.lastListCall?.statuses)
        assertNull(repository.lastListCall?.query)
        assertEquals(OrderRequestsFilter.PENDING, viewModel.uiState.value.selectedFilter)
        assertFalse(viewModel.uiState.value.isLoading)
        assertNotNull(viewModel.uiState.value.lastUpdatedAt)
    }

    @Test
    fun `Rechazados re-queries with both REJECTED and EXPIRED`() = runTest {
        val viewModel = createViewModel()
        runCurrent()

        viewModel.onEvent(OrderRequestsUiEvent.SelectFilter(OrderRequestsFilter.REJECTED))
        runCurrent()

        assertEquals(
            listOf(OrderStatus.REJECTED, OrderStatus.EXPIRED),
            repository.lastListCall?.statuses,
        )
        assertEquals(OrderRequestsFilter.REJECTED, viewModel.uiState.value.selectedFilter)
    }

    @Test
    fun `search sends q with the active filter and only the latest query wins`() = runTest {
        val viewModel = createViewModel()
        runCurrent()
        val listCallsAfterInit = repository.calls.count { it.size > 1 }

        viewModel.onEvent(OrderRequestsUiEvent.Search("ma"))
        viewModel.onEvent(OrderRequestsUiEvent.Search("mari"))
        advanceTimeBy(PAST_DEBOUNCE_MS)
        runCurrent()

        assertEquals("mari", repository.lastListCall?.query)
        assertEquals(listOf(OrderStatus.PENDING), repository.lastListCall?.statuses)
        // The intermediate "ma" keystroke was debounced away — one extra list query, not two.
        assertEquals(listCallsAfterInit + 1, repository.calls.count { it.size > 1 })
    }

    @Test
    fun `clearing the search reloads without a query`() = runTest {
        val viewModel = createViewModel()
        viewModel.onEvent(OrderRequestsUiEvent.Search("mari"))
        advanceTimeBy(PAST_DEBOUNCE_MS)
        runCurrent()

        viewModel.onEvent(OrderRequestsUiEvent.Search(""))
        runCurrent()

        assertNull(repository.lastListCall?.query)
    }

    @Test
    fun `pending count comes from the badge query's totalElements`() = runTest {
        badgeCountOf(7)

        val viewModel = createViewModel()
        runCurrent()

        assertEquals(7, viewModel.uiState.value.pendingCount)
        assertTrue(repository.calls.any { it.size == 1 && it.statuses == listOf(OrderStatus.PENDING) })
    }

    @Test
    fun `pending count is refreshed from PENDING even while another tab is shown`() = runTest {
        badgeCountOf(3)
        val viewModel = createViewModel()
        runCurrent()

        viewModel.onEvent(OrderRequestsUiEvent.SelectFilter(OrderRequestsFilter.CONFIRMED))
        runCurrent()

        assertEquals(3, viewModel.uiState.value.pendingCount)
        assertEquals(listOf(OrderStatus.CONFIRMED), repository.lastListCall?.statuses)
    }

    @Test
    fun `opening a detail fetches it and populates the dialog`() = runTest {
        repository.detailResult = Result.success(
            TestFixtures.orderDetail(items = listOf(TestFixtures.orderItem(), TestFixtures.orderItem(id = "item-2"))),
        )
        val viewModel = createViewModel()
        runCurrent()

        viewModel.onEvent(OrderRequestsUiEvent.OpenDetail("order-1"))
        runCurrent()

        val detail = viewModel.uiState.value.detail
        assertNotNull(detail)
        assertEquals("order-1", detail?.orderId)
        assertEquals(2, detail?.order?.items?.size)
        assertFalse(detail?.isLoading ?: true)
        assertEquals(listOf("order-1"), repository.requestedDetailIds)
    }

    @Test
    fun `a failed detail fetch leaves the list intact`() = runTest {
        repository.detailResult = Result.failure(IOException("offline"))
        val viewModel = createViewModel()
        runCurrent()
        val ordersBefore = viewModel.uiState.value.orders

        viewModel.onEvent(OrderRequestsUiEvent.OpenDetail("order-1"))
        runCurrent()

        assertTrue(viewModel.uiState.value.detail?.isError == true)
        assertEquals(ordersBefore, viewModel.uiState.value.orders)
    }

    @Test
    fun `dismissing the dialog changes nothing else`() = runTest {
        val viewModel = createViewModel()
        runCurrent()
        viewModel.onEvent(OrderRequestsUiEvent.OpenDetail("order-1"))
        runCurrent()
        val listCallsBefore = repository.calls.count { it.size > 1 }
        val lastUpdatedBefore = viewModel.uiState.value.lastUpdatedAt

        viewModel.onEvent(OrderRequestsUiEvent.DismissDetail)
        runCurrent()

        assertNull(viewModel.uiState.value.detail)
        assertEquals(listCallsBefore, repository.calls.count { it.size > 1 })
        assertEquals(lastUpdatedBefore, viewModel.uiState.value.lastUpdatedAt)
    }

    @Test
    fun `confirm closes the dialog, emits the snackbar and reloads`() = runTest {
        val viewModel = createViewModel()
        runCurrent()
        viewModel.onEvent(OrderRequestsUiEvent.OpenDetail("order-1"))
        runCurrent()
        val listCallsBefore = repository.calls.count { it.size > 1 }

        viewModel.uiEffect.test {
            viewModel.onEvent(OrderRequestsUiEvent.ConfirmOrder("order-1"))
            runCurrent()

            assertEquals(OrderRequestsUiEffect.ShowConfirmedSnackbar, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        assertEquals(listOf("order-1"), repository.confirmedIds)
        assertNull(viewModel.uiState.value.detail)
        assertEquals(listCallsBefore + 1, repository.calls.count { it.size > 1 })
    }

    @Test
    fun `reject from the card emits the snackbar and reloads without a dialog`() = runTest {
        val viewModel = createViewModel()
        runCurrent()
        val listCallsBefore = repository.calls.count { it.size > 1 }

        viewModel.uiEffect.test {
            viewModel.onEvent(OrderRequestsUiEvent.RejectOrder("order-2"))
            runCurrent()

            assertEquals(OrderRequestsUiEffect.ShowRejectedSnackbar, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        assertEquals(listOf("order-2"), repository.rejectedIds)
        assertEquals(listCallsBefore + 1, repository.calls.count { it.size > 1 })
    }

    @Test
    fun `a second tap cannot fire a second confirm`() = runTest {
        // A non-eager dispatcher, so the first confirm is still in flight when the second tap
        // arrives. Under the default unconfined dispatcher it would already have completed and the
        // guard would never be exercised.
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val viewModel = createViewModel()
        runCurrent()
        viewModel.onEvent(OrderRequestsUiEvent.OpenDetail("order-1"))
        runCurrent()

        viewModel.onEvent(OrderRequestsUiEvent.ConfirmOrder("order-1"))
        viewModel.onEvent(OrderRequestsUiEvent.ConfirmOrder("order-1"))
        runCurrent()

        assertEquals(listOf("order-1"), repository.confirmedIds)
    }

    @Test
    fun `StockInsufficient closes the dialog, names the product and reloads`() = runTest {
        repository.confirmResult = Result.failure(
            ConfirmOrderException(
                ConfirmOrderError.StockInsufficient(
                    productName = "Foco LED Bosch 9W",
                    available = 3,
                    requested = 10,
                ),
            ),
        )
        val viewModel = createViewModel()
        runCurrent()
        viewModel.onEvent(OrderRequestsUiEvent.OpenDetail("order-1"))
        runCurrent()
        val listCallsBefore = repository.calls.count { it.size > 1 }

        viewModel.uiEffect.test {
            viewModel.onEvent(OrderRequestsUiEvent.ConfirmOrder("order-1"))
            runCurrent()

            val effect = awaitItem()
            assertTrue(effect is OrderRequestsUiEffect.ShowError)
            assertEquals("Foco LED Bosch 9W", (effect as OrderRequestsUiEffect.ShowError).productName)
            cancelAndIgnoreRemainingEvents()
        }

        assertNull(viewModel.uiState.value.detail)
        assertEquals(listCallsBefore + 1, repository.calls.count { it.size > 1 })
    }

    @Test
    fun `AlreadyHandled on reject closes the dialog, reports it and reloads`() = runTest {
        repository.rejectResult =
            Result.failure(RejectOrderException(RejectOrderError.AlreadyHandled))
        val viewModel = createViewModel()
        runCurrent()
        viewModel.onEvent(OrderRequestsUiEvent.OpenDetail("order-1"))
        runCurrent()
        val listCallsBefore = repository.calls.count { it.size > 1 }

        viewModel.uiEffect.test {
            viewModel.onEvent(OrderRequestsUiEvent.RejectOrder("order-1"))
            runCurrent()

            assertTrue(awaitItem() is OrderRequestsUiEffect.ShowError)
            cancelAndIgnoreRemainingEvents()
        }

        assertNull(viewModel.uiState.value.detail)
        assertEquals(listCallsBefore + 1, repository.calls.count { it.size > 1 })
    }

    @Test
    fun `a network failure on confirm keeps the dialog open`() = runTest {
        repository.confirmResult =
            Result.failure(ConfirmOrderException(ConfirmOrderError.Network))
        val viewModel = createViewModel()
        runCurrent()
        viewModel.onEvent(OrderRequestsUiEvent.OpenDetail("order-1"))
        runCurrent()

        viewModel.uiEffect.test {
            viewModel.onEvent(OrderRequestsUiEvent.ConfirmOrder("order-1"))
            runCurrent()

            assertTrue(awaitItem() is OrderRequestsUiEffect.ShowError)
            cancelAndIgnoreRemainingEvents()
        }

        val detail = viewModel.uiState.value.detail
        assertNotNull(detail)
        assertFalse(detail?.isSubmitting ?: true)
    }

    @Test
    fun `a failed refresh keeps the previous orders and last-updated time`() = runTest {
        repository.ordersResult = Result.success(
            OrderPage(orders = listOf(TestFixtures.order(id = "order-1")), totalElements = 1),
        )
        val viewModel = createViewModel()
        runCurrent()
        val ordersBefore = viewModel.uiState.value.orders
        val lastUpdatedBefore = viewModel.uiState.value.lastUpdatedAt

        repository.ordersResult = Result.failure(IOException("offline"))
        viewModel.onEvent(OrderRequestsUiEvent.Refresh)
        runCurrent()

        val state = viewModel.uiState.value
        assertTrue(state.isError)
        assertFalse(state.isLoading)
        assertEquals(ordersBefore, state.orders)
        assertEquals(lastUpdatedBefore, state.lastUpdatedAt)
    }

    @Test
    fun `retry re-issues the request with the active filter and query`() = runTest {
        val viewModel = createViewModel()
        runCurrent()
        viewModel.onEvent(OrderRequestsUiEvent.SelectFilter(OrderRequestsFilter.CONFIRMED))
        viewModel.onEvent(OrderRequestsUiEvent.Search("mari"))
        advanceTimeBy(PAST_DEBOUNCE_MS)
        runCurrent()

        viewModel.onEvent(OrderRequestsUiEvent.Retry)
        runCurrent()

        assertEquals(listOf(OrderStatus.CONFIRMED), repository.lastListCall?.statuses)
        assertEquals("mari", repository.lastListCall?.query)
    }
}
