package com.qhathia.mobile.staff.core.domain.usecase

import com.qhathia.mobile.staff.core.domain.model.OrderPage
import com.qhathia.mobile.staff.core.domain.model.OrderStatus
import com.qhathia.mobile.staff.core.testing.FakeOrderNotificationTray
import com.qhathia.mobile.staff.core.testing.FakeOrderRepository
import com.qhathia.mobile.staff.core.testing.TestFixtures
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ReconcileOrderNotificationsUseCaseTest {

    private fun pendingPage(vararg ids: String, total: Int = ids.size) = Result.success(
        OrderPage(orders = ids.map { TestFixtures.order(id = it) }, totalElements = total),
    )

    @Test
    fun `given an empty tray when invoked then only the pending count is refreshed`() = runTest {
        val repository = FakeOrderRepository()
        val tray = FakeOrderNotificationTray()

        ReconcileOrderNotificationsUseCase(repository, tray)()

        assertEquals(1, repository.refreshPendingCountCallCount)
        assertTrue(repository.calls.isEmpty())
        assertTrue(tray.cancelledIds.isEmpty())
    }

    @Test
    fun `given notifications when invoked then it reads the unfiltered pending list`() = runTest {
        val repository = FakeOrderRepository().apply { ordersResult = pendingPage("a") }
        val tray = FakeOrderNotificationTray(setOf("a"))

        ReconcileOrderNotificationsUseCase(repository, tray)()

        assertEquals(
            listOf(
                FakeOrderRepository.OrdersCall(
                    statuses = listOf(OrderStatus.PENDING),
                    query = null,
                    page = 0,
                    size = ReconcileOrderNotificationsUseCase.RECONCILE_PAGE_SIZE,
                ),
            ),
            repository.calls,
        )
    }

    @Test
    fun `given orders no longer pending when invoked then only their notifications are cancelled`() = runTest {
        val repository = FakeOrderRepository().apply { ordersResult = pendingPage("a", "c") }
        val tray = FakeOrderNotificationTray(setOf("a", "b", "c", "d"))

        ReconcileOrderNotificationsUseCase(repository, tray)()

        assertEquals(setOf("b", "d"), tray.cancelledIds.toSet())
        assertEquals(setOf("a", "c"), tray.activeOrderIds())
    }

    @Test
    fun `given no pending orders when invoked then every notification is cancelled`() = runTest {
        val repository = FakeOrderRepository().apply { ordersResult = pendingPage() }
        val tray = FakeOrderNotificationTray(setOf("a", "b"))

        ReconcileOrderNotificationsUseCase(repository, tray)()

        assertTrue(tray.activeOrderIds().isEmpty())
    }

    @Test
    fun `given notifications when invoked then the pending count follows the server`() = runTest {
        val repository = FakeOrderRepository().apply { ordersResult = pendingPage("a", "c") }
        val tray = FakeOrderNotificationTray(setOf("a"))

        ReconcileOrderNotificationsUseCase(repository, tray)()

        assertEquals(2, repository.pendingOrderCount.first())
    }

    @Test
    fun `given more pending orders than one page when invoked then nothing is cancelled`() = runTest {
        val repository = FakeOrderRepository().apply { ordersResult = pendingPage("a", total = 80) }
        val tray = FakeOrderNotificationTray(setOf("a", "b"))

        ReconcileOrderNotificationsUseCase(repository, tray)()

        assertTrue(tray.cancelledIds.isEmpty())
    }

    @Test
    fun `given the read fails when invoked then nothing is cancelled and it does not throw`() = runTest {
        val repository = FakeOrderRepository().apply {
            ordersResult = Result.failure(IllegalStateException("HTTP 500"))
        }
        val tray = FakeOrderNotificationTray(setOf("a"))

        ReconcileOrderNotificationsUseCase(repository, tray)()

        assertTrue(tray.cancelledIds.isEmpty())
    }

    @Test
    fun `when invoked then unmatched notifications are always cancelled`() = runTest {
        val failing = FakeOrderRepository().apply {
            ordersResult = Result.failure(IllegalStateException("offline"))
        }
        val emptyTray = FakeOrderNotificationTray()
        val busyTray = FakeOrderNotificationTray(setOf("a"))

        ReconcileOrderNotificationsUseCase(FakeOrderRepository(), emptyTray)()
        ReconcileOrderNotificationsUseCase(failing, busyTray)()

        assertEquals(1, emptyTray.cancelUnmatchedCallCount)
        assertEquals(1, busyTray.cancelUnmatchedCallCount)
    }
}
