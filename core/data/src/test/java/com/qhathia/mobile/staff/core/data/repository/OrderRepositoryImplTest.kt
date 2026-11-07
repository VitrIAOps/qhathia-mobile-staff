package com.qhathia.mobile.staff.core.data.repository

import com.qhathia.mobile.staff.core.domain.model.ConfirmOrderError
import com.qhathia.mobile.staff.core.domain.model.ConfirmOrderException
import com.qhathia.mobile.staff.core.domain.model.OrderStatus
import com.qhathia.mobile.staff.core.domain.model.RejectOrderError
import com.qhathia.mobile.staff.core.domain.model.RejectOrderException
import com.qhathia.mobile.staff.core.network.NetworkResult
import com.qhathia.mobile.staff.core.network.OrderMutationResult
import com.qhathia.mobile.staff.core.network.dto.OrderDetailDto
import com.qhathia.mobile.staff.core.network.dto.OrderItemDto
import com.qhathia.mobile.staff.core.network.dto.OrderPageDto
import com.qhathia.mobile.staff.core.network.dto.OrderProblemDto
import com.qhathia.mobile.staff.core.network.dto.OrderSummaryDto
import com.qhathia.mobile.staff.core.network.service.OrderApiService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.IOException

class OrderRepositoryImplTest {

    private val api = mockk<OrderApiService>()
    private val repository = OrderRepositoryImpl(api)

    private fun detailDto() = OrderDetailDto(
        id = "order-1",
        status = "CONFIRMED",
        customerName = "Maria Murillo",
        orderCode = "859653",
        total = 50.0,
        items = listOf(
            OrderItemDto(
                id = "item-1",
                productId = "product-1",
                productName = "Foco LED Bosch 9W",
                quantity = 2,
                unitPriceSnapshot = 25.0,
                lineTotal = 50.0,
            ),
        ),
    )

    /**
     * A successful confirm or reject re-reads the pending count, so every test needs that query
     * answered. Tests that assert on the count stub it again with the value they care about.
     */
    @BeforeEach
    fun stubPendingCountQuery() {
        coEvery { api.getOrders(listOf("PENDING"), null, 0, 1) } returns
            NetworkResult.Success(OrderPageDto())
    }

    /** Extracts the domain error a failed transition carries. */
    private inline fun <reified E : Throwable> Result<*>.errorOf(): E =
        assertInstanceOf(E::class.java, exceptionOrNull())

    @Test
    fun `given success when getOrders then maps the page to domain`() = runTest {
        coEvery { api.getOrders(any(), any(), any(), any()) } returns NetworkResult.Success(
            OrderPageDto(
                content = listOf(OrderSummaryDto(id = "order-1", status = "PENDING")),
                totalElements = 4,
            ),
        )

        val result = repository.getOrders(listOf(OrderStatus.PENDING), null, page = 0, size = 20)

        assertTrue(result.isSuccess)
        val page = result.getOrThrow()
        assertEquals(4, page.totalElements)
        assertEquals("order-1", page.orders.single().id)
    }

    @Test
    fun `getOrders forwards status names and a trimmed query`() = runTest {
        coEvery { api.getOrders(any(), any(), any(), any()) } returns
            NetworkResult.Success(OrderPageDto())

        repository.getOrders(
            statuses = listOf(OrderStatus.REJECTED, OrderStatus.EXPIRED),
            query = "  mari  ",
            page = 0,
            size = 50,
        )

        coVerify { api.getOrders(listOf("REJECTED", "EXPIRED"), "mari", 0, 50) }
    }

    @Test
    fun `getOrders drops a blank query so no empty q is sent`() = runTest {
        coEvery { api.getOrders(any(), any(), any(), any()) } returns
            NetworkResult.Success(OrderPageDto())

        repository.getOrders(listOf(OrderStatus.PENDING), query = "   ", page = 0, size = 50)

        coVerify { api.getOrders(listOf("PENDING"), null, 0, 50) }
    }

    @Test
    fun `given network failure when getOrders then propagates the cause`() = runTest {
        val cause = IOException("offline")
        coEvery { api.getOrders(any(), any(), any(), any()) } returns
            NetworkResult.NetworkError(cause)

        val result = repository.getOrders(listOf(OrderStatus.PENDING), null, 0, 50)

        assertEquals(cause, result.exceptionOrNull())
    }

    @Test
    fun `given success when confirmOrder then maps the returned order`() = runTest {
        coEvery { api.confirmOrder("order-1") } returns OrderMutationResult.Success(detailDto())

        val result = repository.confirmOrder("order-1")

        assertTrue(result.isSuccess)
        assertEquals(OrderStatus.CONFIRMED, result.getOrThrow().order.status)
        assertEquals(1, result.getOrThrow().items.size)
    }

    @Test
    fun `422 STOCK_INSUFFICIENT maps to StockInsufficient with its detail fields`() = runTest {
        coEvery { api.confirmOrder("order-1") } returns OrderMutationResult.HttpError(
            code = 422,
            problem = OrderProblemDto(
                errorCode = "STOCK_INSUFFICIENT",
                productName = "Foco LED Bosch 9W",
                available = 3,
                requested = 10,
            ),
        )

        val result = repository.confirmOrder("order-1")

        val error = result.errorOf<ConfirmOrderException>().error
        val stock = assertInstanceOf(ConfirmOrderError.StockInsufficient::class.java, error)
        assertEquals("Foco LED Bosch 9W", stock.productName)
        assertEquals(3, stock.available)
        assertEquals(10, stock.requested)
    }

    @Test
    fun `422 INVALID_STATE_TRANSITION maps to AlreadyHandled on confirm`() = runTest {
        coEvery { api.confirmOrder("order-1") } returns OrderMutationResult.HttpError(
            code = 422,
            problem = OrderProblemDto(errorCode = "INVALID_STATE_TRANSITION"),
        )

        val result = repository.confirmOrder("order-1")

        assertEquals(ConfirmOrderError.AlreadyHandled, result.errorOf<ConfirmOrderException>().error)
    }

    @Test
    fun `404 maps to NotFound on confirm`() = runTest {
        coEvery { api.confirmOrder("order-1") } returns
            OrderMutationResult.HttpError(code = 404, problem = null)

        val result = repository.confirmOrder("order-1")

        assertEquals(ConfirmOrderError.NotFound, result.errorOf<ConfirmOrderException>().error)
    }

    @Test
    fun `transport failure maps to Network on confirm`() = runTest {
        coEvery { api.confirmOrder("order-1") } returns
            OrderMutationResult.NetworkError(IOException("offline"))

        val result = repository.confirmOrder("order-1")

        assertEquals(ConfirmOrderError.Network, result.errorOf<ConfirmOrderException>().error)
    }

    @Test
    fun `an unrecognized error code maps to Unknown carrying the status`() = runTest {
        coEvery { api.confirmOrder("order-1") } returns OrderMutationResult.HttpError(
            code = 500,
            problem = OrderProblemDto(errorCode = "SOMETHING_ELSE"),
        )

        val result = repository.confirmOrder("order-1")

        val error = result.errorOf<ConfirmOrderException>().error
        assertEquals(ConfirmOrderError.Unknown("HTTP 500"), error)
    }

    @Test
    fun `422 INVALID_STATE_TRANSITION maps to AlreadyHandled on reject`() = runTest {
        coEvery { api.rejectOrder("order-1", null) } returns OrderMutationResult.HttpError(
            code = 422,
            problem = OrderProblemDto(errorCode = "INVALID_STATE_TRANSITION"),
        )

        val result = repository.rejectOrder("order-1", reason = null)

        assertEquals(RejectOrderError.AlreadyHandled, result.errorOf<RejectOrderException>().error)
    }

    @Test
    fun `transport failure maps to Network on reject`() = runTest {
        coEvery { api.rejectOrder("order-1", null) } returns
            OrderMutationResult.NetworkError(IOException("offline"))

        val result = repository.rejectOrder("order-1", reason = null)

        assertEquals(RejectOrderError.Network, result.errorOf<RejectOrderException>().error)
    }

    @Test
    fun `reject forwards the reason to the api`() = runTest {
        coEvery { api.rejectOrder("order-1", "Sin stock") } returns
            OrderMutationResult.NetworkError(IOException("offline"))

        repository.rejectOrder("order-1", reason = "Sin stock")

        coVerify { api.rejectOrder("order-1", "Sin stock") }
    }

    // --- app-wide pending count -------------------------------------------------------------

    private fun pendingPageOf(count: Int) =
        NetworkResult.Success(OrderPageDto(content = emptyList(), totalElements = count))

    @Test
    fun `refreshPendingOrderCount publishes totalElements`() = runTest {
        coEvery { api.getOrders(listOf("PENDING"), null, 0, 1) } returns pendingPageOf(4)

        val result = repository.refreshPendingOrderCount()

        assertEquals(4, result.getOrThrow())
        assertEquals(4, repository.pendingOrderCount.first())
    }

    @Test
    fun `an unfiltered pending getOrders feeds the count without an extra request`() = runTest {
        coEvery { api.getOrders(listOf("PENDING"), null, 0, 50) } returns pendingPageOf(6)

        repository.getOrders(listOf(OrderStatus.PENDING), query = null, page = 0, size = 50)

        assertEquals(6, repository.pendingOrderCount.first())
        // The size=1 count query was never issued: the list load already carried the number.
        coVerify(exactly = 0) { api.getOrders(any(), any(), any(), 1) }
    }

    @Test
    fun `a search-narrowed pending query does not publish its count`() = runTest {
        coEvery { api.getOrders(listOf("PENDING"), null, 0, 50) } returns pendingPageOf(6)
        repository.getOrders(listOf(OrderStatus.PENDING), query = null, page = 0, size = 50)
        coEvery { api.getOrders(listOf("PENDING"), "mari", 0, 50) } returns pendingPageOf(1)

        repository.getOrders(listOf(OrderStatus.PENDING), query = "mari", page = 0, size = 50)

        // Still 6: one matching order is not "one order pending".
        assertEquals(6, repository.pendingOrderCount.first())
    }

    @Test
    fun `a non-pending query does not publish its count`() = runTest {
        coEvery { api.getOrders(listOf("PENDING"), null, 0, 1) } returns pendingPageOf(3)
        repository.refreshPendingOrderCount()
        coEvery { api.getOrders(listOf("CONFIRMED"), null, 0, 50) } returns pendingPageOf(11)

        repository.getOrders(listOf(OrderStatus.CONFIRMED), query = null, page = 0, size = 50)

        assertEquals(3, repository.pendingOrderCount.first())
    }

    @Test
    fun `confirming an order refreshes the count`() = runTest {
        coEvery { api.confirmOrder("order-1") } returns OrderMutationResult.Success(detailDto())
        coEvery { api.getOrders(listOf("PENDING"), null, 0, 1) } returns pendingPageOf(1)

        repository.confirmOrder("order-1")

        assertEquals(1, repository.pendingOrderCount.first())
        coVerify(exactly = 1) { api.getOrders(listOf("PENDING"), null, 0, 1) }
    }

    @Test
    fun `rejecting an order refreshes the count`() = runTest {
        coEvery { api.rejectOrder("order-1", null) } returns OrderMutationResult.Success(detailDto())
        coEvery { api.getOrders(listOf("PENDING"), null, 0, 1) } returns pendingPageOf(2)

        repository.rejectOrder("order-1", reason = null)

        assertEquals(2, repository.pendingOrderCount.first())
    }

    @Test
    fun `concurrent refreshes send one request and share its count`() = runTest {
        val answer = CompletableDeferred<NetworkResult<OrderPageDto>>()
        coEvery { api.getOrders(listOf("PENDING"), null, 0, 1) } coAnswers { answer.await() }

        val first = async { repository.refreshPendingOrderCount() }
        val second = async { repository.refreshPendingOrderCount() }
        yield()
        answer.complete(pendingPageOf(3))

        assertEquals(3, first.await().getOrThrow())
        assertEquals(3, second.await().getOrThrow())
        coVerify(exactly = 1) { api.getOrders(listOf("PENDING"), null, 0, 1) }
    }

    @Test
    fun `a refresh that must be fresh sends its own request after the one in flight`() = runTest {
        val answer = CompletableDeferred<NetworkResult<OrderPageDto>>()
        var calls = 0
        coEvery { api.getOrders(listOf("PENDING"), null, 0, 1) } coAnswers {
            calls++
            if (calls == 1) answer.await() else pendingPageOf(4)
        }

        val running = async { repository.refreshPendingOrderCount() }
        val fresh = async { repository.refreshPendingOrderCount(reuseInFlight = false) }
        yield()
        answer.complete(pendingPageOf(3))

        assertEquals(3, running.await().getOrThrow())
        assertEquals(4, fresh.await().getOrThrow())
        assertEquals(2, calls)
        assertEquals(4, repository.pendingOrderCount.first())
    }

    @Test
    fun `a failed refresh is not shared with the one waiting`() = runTest {
        val answer = CompletableDeferred<NetworkResult<OrderPageDto>>()
        var calls = 0
        coEvery { api.getOrders(listOf("PENDING"), null, 0, 1) } coAnswers {
            calls++
            if (calls == 1) answer.await() else pendingPageOf(2)
        }

        val failing = async { repository.refreshPendingOrderCount() }
        val waiting = async { repository.refreshPendingOrderCount() }
        yield()
        answer.complete(NetworkResult.NetworkError(IOException("offline")))

        assertTrue(failing.await().isFailure)
        assertEquals(2, waiting.await().getOrThrow())
        assertEquals(2, calls)
    }

    @Test
    fun `an auth failure resets the count to zero`() = runTest {
        coEvery { api.getOrders(listOf("PENDING"), null, 0, 1) } returns pendingPageOf(5)
        repository.refreshPendingOrderCount()
        assertEquals(5, repository.pendingOrderCount.first())

        coEvery { api.getOrders(listOf("PENDING"), null, 0, 1) } returns
            NetworkResult.HttpError(401, "Unauthorized")

        val result = repository.refreshPendingOrderCount()

        assertTrue(result.isFailure)
        assertEquals(0, repository.pendingOrderCount.first())
    }

    @Test
    fun `a transport failure leaves the count alone`() = runTest {
        coEvery { api.getOrders(listOf("PENDING"), null, 0, 1) } returns pendingPageOf(5)
        repository.refreshPendingOrderCount()

        coEvery { api.getOrders(listOf("PENDING"), null, 0, 1) } returns
            NetworkResult.NetworkError(IOException("offline"))

        assertTrue(repository.refreshPendingOrderCount().isFailure)
        // Offline is not "nothing is pending"; the last known count stands.
        assertEquals(5, repository.pendingOrderCount.first())
    }

    @Test
    fun `409 PRODUCT_NOT_SELLABLE maps to ProductNotSellable with the product and reason`() = runTest {
        coEvery { api.confirmOrder("order-1") } returns OrderMutationResult.HttpError(
            code = 409,
            problem = OrderProblemDto(errorCode = "PRODUCT_NOT_SELLABLE", productName = "Taladro", reason = "INACTIVE"),
        )

        val error = repository.confirmOrder("order-1").errorOf<ConfirmOrderException>().error

        assertEquals(ConfirmOrderError.ProductNotSellable("Taladro", "INACTIVE"), error)
    }

    @Test
    fun `a confirm whose count cannot be re-read lowers the count by one`() = runTest {
        coEvery { api.getOrders(listOf("PENDING"), null, 0, 1) } returns pendingPageOf(2)
        repository.refreshPendingOrderCount()
        coEvery { api.confirmOrder("order-1") } returns OrderMutationResult.Success(detailDto())
        coEvery { api.getOrders(listOf("PENDING"), null, 0, 1) } returns
            NetworkResult.NetworkError(IOException("offline"))

        repository.confirmOrder("order-1")

        assertEquals(1, repository.pendingOrderCount.first())
    }

    @Test
    fun `a reject whose count cannot be re-read never goes below zero`() = runTest {
        coEvery { api.rejectOrder("order-1", null) } returns OrderMutationResult.Success(detailDto())
        coEvery { api.getOrders(listOf("PENDING"), null, 0, 1) } returns
            NetworkResult.NetworkError(IOException("offline"))

        repository.rejectOrder("order-1", reason = null)

        assertEquals(0, repository.pendingOrderCount.first())
    }

    @Test
    fun `notifyOrdersChanged emits on ordersChanged`() = runTest {
        val received = async { repository.ordersChanged.first() }
        yield()

        repository.notifyOrdersChanged()

        assertEquals(Unit, received.await())
    }
}
