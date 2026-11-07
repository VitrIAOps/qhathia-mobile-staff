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
import com.qhathia.mobile.staff.core.domain.usecase.ObserveOrdersChangedUseCase
import com.qhathia.mobile.staff.core.domain.usecase.ObservePendingOrderCountUseCase
import com.qhathia.mobile.staff.core.domain.usecase.RejectOrderUseCase
import com.qhathia.mobile.staff.core.testing.FakeOrderRepository
import com.qhathia.mobile.staff.core.testing.MainDispatcherExtension
import com.qhathia.mobile.staff.core.testing.TestFixtures
import com.qhathia.mobile.staff.feature.orderrequests.model.OrderRequestsFilter
import com.qhathia.mobile.staff.feature.orderrequests.model.OrderRequestsUiEffect
import com.qhathia.mobile.staff.feature.orderrequests.model.OrderRequestsUiEvent
import com.qhathia.mobile.staff.feature.orderrequests.R
import com.qhathia.mobile.staff.feature.orderrequests.model.RejectionReason
import com.qhathia.mobile.staff.feature.orderrequests.model.SubmitError
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
        observePendingOrderCountUseCase = ObservePendingOrderCountUseCase(repository),
        observeOrdersChangedUseCase = ObserveOrdersChangedUseCase(repository),
    )

    /**
     * Publishes [count] on the shared pending-order flow — the screen no longer counts for itself,
     * so this is the only way the label can move.
     */
    private fun sharedPendingCountOf(count: Int) {
        repository.pendingCountResult = Result.success(count)
        repository.emitPendingCount(count)
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
    fun `pending count comes from the shared flow, not from a query of its own`() = runTest {
        val viewModel = createViewModel()
        runCurrent()

        sharedPendingCountOf(7)
        runCurrent()

        assertEquals(7, viewModel.uiState.value.pendingCount)
        // The screen used to issue its own size=1 pending request; it must not any more.
        assertTrue(repository.calls.none { it.size == 1 })
    }

    @Test
    fun `pending count survives switching to another tab`() = runTest {
        val viewModel = createViewModel()
        runCurrent()
        sharedPendingCountOf(3)
        runCurrent()

        viewModel.onEvent(OrderRequestsUiEvent.SelectFilter(OrderRequestsFilter.CONFIRMED))
        runCurrent()

        // The Aceptados query says nothing about how many orders are pending, so 3 stands.
        assertEquals(3, viewModel.uiState.value.pendingCount)
        assertEquals(listOf(OrderStatus.CONFIRMED), repository.lastListCall?.statuses)
    }

    @Test
    fun `the unfiltered pending list load feeds the shared count`() = runTest {
        repository.ordersResult = Result.success(
            OrderPage(orders = listOf(TestFixtures.order()), totalElements = 5),
        )

        val viewModel = createViewModel()
        runCurrent()

        assertEquals(5, viewModel.uiState.value.pendingCount)
    }

    @Test
    fun `confirming an order moves the shared count`() = runTest {
        sharedPendingCountOf(2)
        val viewModel = createViewModel()
        runCurrent()
        repository.pendingCountResult = Result.success(1)

        viewModel.onEvent(OrderRequestsUiEvent.ConfirmOrder("order-1"))
        runCurrent()

        assertEquals(1, viewModel.uiState.value.pendingCount)
    }

    @Test
    fun `rejecting an order moves the shared count`() = runTest {
        sharedPendingCountOf(2)
        val viewModel = createViewModel()
        runCurrent()
        viewModel.onEvent(OrderRequestsUiEvent.StartRejection("order-1", "859653"))
        runCurrent()
        repository.pendingCountResult = Result.success(1)

        viewModel.onEvent(OrderRequestsUiEvent.ConfirmRejection("Sin stock"))
        runCurrent()

        assertEquals(1, viewModel.uiState.value.pendingCount)
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
    fun `reject from the card opens the motive form instead of rejecting outright`() = runTest {
        val viewModel = createViewModel()
        runCurrent()

        viewModel.onEvent(OrderRequestsUiEvent.StartRejection("order-2", "458795"))
        runCurrent()

        val rejection = viewModel.uiState.value.detail?.rejection
        assertEquals("order-2", viewModel.uiState.value.detail?.orderId)
        assertEquals("458795", rejection?.orderCode)
        assertNull(rejection?.selectedReason)
        assertTrue(repository.rejectedIds.isEmpty())
    }

    @Test
    fun `confirming the form rejects with the reason, emits the snackbar and reloads`() = runTest {
        val viewModel = createViewModel()
        runCurrent()
        viewModel.onEvent(OrderRequestsUiEvent.StartRejection("order-2", "458795"))
        runCurrent()
        val listCallsBefore = repository.calls.count { it.size > 1 }

        viewModel.uiEffect.test {
            viewModel.onEvent(OrderRequestsUiEvent.ConfirmRejection("Cantidad insuficiente"))
            runCurrent()

            assertEquals(OrderRequestsUiEffect.ShowRejectedSnackbar, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        assertEquals(listOf("order-2"), repository.rejectedIds)
        assertEquals(listOf("Cantidad insuficiente"), repository.rejectedReasons)
        assertEquals(listCallsBefore + 1, repository.calls.count { it.size > 1 })
    }

    @Test
    fun `a form opened from a card closes the dialog on cancel`() = runTest {
        val viewModel = createViewModel()
        runCurrent()
        viewModel.onEvent(OrderRequestsUiEvent.StartRejection("order-2", "458795"))
        runCurrent()

        viewModel.onEvent(OrderRequestsUiEvent.CancelRejection)
        runCurrent()

        assertNull(viewModel.uiState.value.detail)
    }

    @Test
    fun `a form opened from the detail returns to it on cancel`() = runTest {
        val viewModel = createViewModel()
        runCurrent()
        viewModel.onEvent(OrderRequestsUiEvent.OpenDetail("order-1"))
        runCurrent()
        viewModel.onEvent(OrderRequestsUiEvent.StartRejection("order-1", "458795"))
        runCurrent()

        viewModel.onEvent(OrderRequestsUiEvent.CancelRejection)
        runCurrent()

        val detail = viewModel.uiState.value.detail
        assertNull(detail?.rejection)
        assertNotNull(detail?.order)
    }

    @Test
    fun `confirming is impossible until a motive is picked`() = runTest {
        val viewModel = createViewModel()
        runCurrent()
        viewModel.onEvent(OrderRequestsUiEvent.StartRejection("order-2", "458795"))
        runCurrent()

        assertFalse(viewModel.uiState.value.detail?.rejection?.canSubmit == true)

        viewModel.onEvent(OrderRequestsUiEvent.SelectRejectionReason(RejectionReason.OTHER))
        runCurrent()

        // "Otro motivo" still needs the text written out.
        assertFalse(viewModel.uiState.value.detail?.rejection?.canSubmit == true)

        viewModel.onEvent(OrderRequestsUiEvent.EditOtherRejectionReason("Se agotó el modelo"))
        runCurrent()

        assertTrue(viewModel.uiState.value.detail?.rejection?.canSubmit == true)
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
        viewModel.onEvent(OrderRequestsUiEvent.StartRejection("order-1", "458795"))
        runCurrent()
        val listCallsBefore = repository.calls.count { it.size > 1 }

        viewModel.uiEffect.test {
            viewModel.onEvent(OrderRequestsUiEvent.ConfirmRejection("Sin stock"))
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

            // The error is read inside the dialog; the snackbar would sit under its scrim.
            expectNoEvents()
        }

        val detail = viewModel.uiState.value.detail
        assertNotNull(detail)
        assertFalse(detail?.isSubmitting ?: true)
        assertEquals(SubmitError(R.string.order_error_network), detail?.submitError)
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

    // ---- Re-query on change and on return, settled orders, inline dialog errors ----

    /** Comfortably past the 300 ms debounce of the orders-changed signal. */
    private val pastSignalDebounceMs = 400L

    private fun listCalls() = repository.calls.count { it.size > 1 }

    private fun pendingPageOf(vararg ids: String) = Result.success(
        OrderPage(orders = ids.map { TestFixtures.order(id = it) }, totalElements = ids.size),
    )

    @Test
    fun `orders changed elsewhere re-query the list`() = runTest {
        createViewModel()
        runCurrent()
        val before = listCalls()

        repository.notifyOrdersChanged()
        advanceTimeBy(pastSignalDebounceMs)
        runCurrent()

        assertEquals(before + 1, listCalls())
    }

    @Test
    fun `a burst of changes re-queries once`() = runTest {
        createViewModel()
        runCurrent()
        val before = listCalls()

        repeat(3) { repository.notifyOrdersChanged() }
        advanceTimeBy(pastSignalDebounceMs)
        runCurrent()

        assertEquals(before + 1, listCalls())
    }

    @Test
    fun `the first screen start is the initial load and later ones re-query`() = runTest {
        val viewModel = createViewModel()
        runCurrent()
        val before = listCalls()

        viewModel.onEvent(OrderRequestsUiEvent.ScreenStarted)
        runCurrent()
        assertEquals(before, listCalls())

        viewModel.onEvent(OrderRequestsUiEvent.ScreenStarted)
        runCurrent()
        assertEquals(before + 1, listCalls())
    }

    @Test
    fun `a quiet re-query that fails keeps the list and says nothing`() = runTest {
        repository.ordersResult = pendingPageOf("order-1")
        val viewModel = createViewModel()
        runCurrent()
        repository.ordersResult = Result.failure(IOException("offline"))

        viewModel.uiEffect.test {
            repository.notifyOrdersChanged()
            advanceTimeBy(pastSignalDebounceMs)
            runCurrent()
            expectNoEvents()
        }
        assertEquals(listOf("order-1"), viewModel.uiState.value.orders.map { it.id })
    }

    @Test
    fun `a confirmed order leaves Pendientes even when the reload fails`() = runTest {
        repository.ordersResult = pendingPageOf("order-1", "order-2")
        val viewModel = createViewModel()
        runCurrent()
        viewModel.onEvent(OrderRequestsUiEvent.OpenDetail("order-1"))
        runCurrent()
        repository.ordersResult = Result.failure(IOException("offline"))

        viewModel.uiEffect.test {
            viewModel.onEvent(OrderRequestsUiEvent.ConfirmOrder("order-1"))
            runCurrent()
            assertEquals(OrderRequestsUiEffect.ShowConfirmedSnackbar, awaitItem())
            expectNoEvents()
        }
        assertEquals(listOf("order-2"), viewModel.uiState.value.orders.map { it.id })
    }

    @Test
    fun `a stale page after a confirm does not bring the order back`() = runTest {
        repository.ordersResult = pendingPageOf("order-1", "order-2")
        val viewModel = createViewModel()
        runCurrent()
        viewModel.onEvent(OrderRequestsUiEvent.OpenDetail("order-1"))
        runCurrent()

        // The server still answers with the order as pending (a read before the confirm landed).
        viewModel.onEvent(OrderRequestsUiEvent.ConfirmOrder("order-1"))
        runCurrent()
        viewModel.onEvent(OrderRequestsUiEvent.Refresh)
        runCurrent()

        assertEquals(listOf("order-2"), viewModel.uiState.value.orders.map { it.id })
    }

    @Test
    fun `an order already handled elsewhere leaves Pendientes at once`() = runTest {
        repository.ordersResult = pendingPageOf("order-1", "order-2")
        repository.confirmResult = Result.failure(ConfirmOrderException(ConfirmOrderError.AlreadyHandled))
        val viewModel = createViewModel()
        runCurrent()
        viewModel.onEvent(OrderRequestsUiEvent.OpenDetail("order-1"))
        runCurrent()
        repository.ordersResult = Result.failure(IOException("offline"))

        viewModel.onEvent(OrderRequestsUiEvent.ConfirmOrder("order-1"))
        runCurrent()

        assertEquals(listOf("order-2"), viewModel.uiState.value.orders.map { it.id })
    }

    @Test
    fun `a network error on confirm stays in the dialog with no snackbar`() = runTest {
        repository.confirmResult = Result.failure(ConfirmOrderException(ConfirmOrderError.Network))
        val viewModel = createViewModel()
        runCurrent()
        viewModel.onEvent(OrderRequestsUiEvent.OpenDetail("order-1"))
        runCurrent()

        viewModel.uiEffect.test {
            viewModel.onEvent(OrderRequestsUiEvent.ConfirmOrder("order-1"))
            runCurrent()
            expectNoEvents()
        }
        val detail = checkNotNull(viewModel.uiState.value.detail)
        assertEquals(SubmitError(R.string.order_error_network), detail.submitError)
        assertFalse(detail.isSubmitting)
    }

    @Test
    fun `a network error on reject stays in the form with no snackbar`() = runTest {
        repository.rejectResult = Result.failure(RejectOrderException(RejectOrderError.Network))
        val viewModel = createViewModel()
        runCurrent()
        viewModel.onEvent(OrderRequestsUiEvent.StartRejection("order-1", "859653"))
        viewModel.onEvent(OrderRequestsUiEvent.SelectRejectionReason(RejectionReason.entries.first()))

        viewModel.uiEffect.test {
            viewModel.onEvent(OrderRequestsUiEvent.ConfirmRejection("Sin stock"))
            runCurrent()
            expectNoEvents()
        }
        val detail = checkNotNull(viewModel.uiState.value.detail)
        assertNotNull(detail.rejection)
        assertEquals(SubmitError(R.string.order_error_network), detail.submitError)
    }

    @Test
    fun `a product no longer for sale keeps the dialog with its name`() = runTest {
        repository.confirmResult = Result.failure(
            ConfirmOrderException(ConfirmOrderError.ProductNotSellable("Taladro", "INACTIVE")),
        )
        val viewModel = createViewModel()
        runCurrent()
        viewModel.onEvent(OrderRequestsUiEvent.OpenDetail("order-1"))
        runCurrent()

        viewModel.onEvent(OrderRequestsUiEvent.ConfirmOrder("order-1"))
        runCurrent()

        assertEquals(
            SubmitError(R.string.order_error_not_sellable, "Taladro"),
            viewModel.uiState.value.detail?.submitError,
        )
    }

    @Test
    fun `trying again clears the inline error`() = runTest {
        repository.confirmResult = Result.failure(ConfirmOrderException(ConfirmOrderError.Network))
        val viewModel = createViewModel()
        runCurrent()
        viewModel.onEvent(OrderRequestsUiEvent.OpenDetail("order-1"))
        runCurrent()
        viewModel.onEvent(OrderRequestsUiEvent.ConfirmOrder("order-1"))
        runCurrent()

        repository.confirmResult = Result.success(TestFixtures.orderDetail())
        viewModel.onEvent(OrderRequestsUiEvent.ConfirmOrder("order-1"))
        runCurrent()

        assertNull(viewModel.uiState.value.detail)
        assertEquals(listOf("order-1", "order-1"), repository.confirmedIds)
    }

    @Test
    fun `retrying a failed detail requests it again`() = runTest {
        repository.detailResult = Result.failure(IOException("offline"))
        val viewModel = createViewModel()
        runCurrent()
        viewModel.onEvent(OrderRequestsUiEvent.OpenDetail("order-1"))
        runCurrent()
        assertTrue(viewModel.uiState.value.detail?.isError == true)

        repository.detailResult = Result.success(TestFixtures.orderDetail())
        viewModel.onEvent(OrderRequestsUiEvent.OpenDetail("order-1"))
        runCurrent()

        assertEquals(listOf("order-1", "order-1"), repository.requestedDetailIds)
        assertNotNull(viewModel.uiState.value.detail?.order)
    }
}
