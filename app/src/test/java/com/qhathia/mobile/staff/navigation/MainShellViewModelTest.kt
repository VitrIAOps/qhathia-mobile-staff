package com.qhathia.mobile.staff.navigation

import app.cash.turbine.test
import com.qhathia.mobile.staff.core.domain.usecase.ObservePendingOrderCountUseCase
import com.qhathia.mobile.staff.core.domain.model.OrderPage
import com.qhathia.mobile.staff.core.domain.usecase.NotifyOrdersChangedUseCase
import com.qhathia.mobile.staff.core.domain.usecase.ReconcileOrderNotificationsUseCase
import com.qhathia.mobile.staff.core.testing.FakeOrderNotificationTray
import com.qhathia.mobile.staff.core.testing.FakeOrderRepository
import com.qhathia.mobile.staff.core.testing.MainDispatcherExtension
import com.qhathia.mobile.staff.core.testing.TestFixtures
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class MainShellViewModelTest {

    @JvmField
    @RegisterExtension
    val mainDispatcher = MainDispatcherExtension()

    private lateinit var repository: FakeOrderRepository
    private lateinit var tray: FakeOrderNotificationTray

    @BeforeEach
    fun setUp() {
        repository = FakeOrderRepository()
        tray = FakeOrderNotificationTray()
    }

    private fun createViewModel() = MainShellViewModel(
        observePendingOrderCount = ObservePendingOrderCountUseCase(repository),
        reconcileOrderNotifications = ReconcileOrderNotificationsUseCase(repository, tray),
        notifyOrdersChanged = NotifyOrdersChangedUseCase(repository),
    )

    @Test
    fun `creating the view model alone does not refresh the pending count`() = runTest {
        createViewModel()
        runCurrent()

        assertEquals(0, repository.refreshPendingCountCallCount)
        assertEquals(0, tray.cancelUnmatchedCallCount)
    }

    @Test
    fun `coming to the foreground with an empty tray refreshes the pending count`() = runTest {
        repository.pendingCountResult = Result.success(2)
        val viewModel = createViewModel()

        viewModel.onForegrounded()
        runCurrent()

        assertEquals(1, repository.refreshPendingCountCallCount)
        assertEquals(1, tray.cancelUnmatchedCallCount)
    }

    @Test
    fun `coming to the foreground cancels notifications of orders no longer pending`() = runTest {
        tray.activeIds += setOf("handled", "still-pending")
        repository.ordersResult = Result.success(
            OrderPage(orders = listOf(TestFixtures.order(id = "still-pending")), totalElements = 1),
        )
        val viewModel = createViewModel()

        viewModel.onForegrounded()
        runCurrent()

        assertEquals(listOf("handled"), tray.cancelledIds)
        assertEquals(1, repository.pendingOrderCount.first())
    }

    @Test
    fun `the badge count follows the shared flow`() = runTest {
        val viewModel = createViewModel()

        viewModel.pendingOrderCount.test {
            assertEquals(0, awaitItem())

            repository.emitPendingCount(2)
            assertEquals(2, awaitItem())

            repository.emitPendingCount(1)
            assertEquals(1, awaitItem())

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `a notification tap tells the order lists to re-query`() {
        val viewModel = createViewModel()

        viewModel.onOrdersDeepLink()

        assertEquals(1, repository.notifyOrdersChangedCount)
    }
}
