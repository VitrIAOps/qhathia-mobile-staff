package com.qhathia.mobile.staff.core.domain.usecase

import com.qhathia.mobile.staff.core.testing.FakeOrderRepository
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.IOException

class HandleOrderPushUseCaseTest {

    private val repository = FakeOrderRepository()
    private val handle = HandleOrderPushUseCase(repository)

    @Test
    fun `a new order notifies and refreshes the count`() = runTest {
        handle("NEW_ORDER")

        assertEquals(1, repository.notifyOrdersChangedCount)
        assertEquals(1, repository.refreshPendingCountCallCount)
    }

    @Test
    fun `an order taken notifies and refreshes the count`() = runTest {
        handle("ORDER_TAKEN")

        assertEquals(1, repository.notifyOrdersChangedCount)
        assertEquals(1, repository.refreshPendingCountCallCount)
    }

    @Test
    fun `an expiring order, an unknown event or none do nothing`() = runTest {
        handle("ORDER_EXPIRING")
        handle("SOMETHING_ELSE")
        handle(null)

        assertEquals(0, repository.notifyOrdersChangedCount)
        assertEquals(0, repository.refreshPendingCountCallCount)
    }

    @Test
    fun `a count that cannot be read does not fail the push`() = runTest {
        repository.pendingCountResult = Result.failure(IOException("offline"))

        handle("NEW_ORDER")

        assertEquals(1, repository.notifyOrdersChangedCount)
    }
}
