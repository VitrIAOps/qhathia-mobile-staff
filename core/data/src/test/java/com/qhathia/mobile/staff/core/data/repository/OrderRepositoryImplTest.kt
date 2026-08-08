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
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
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
        coEvery { api.rejectOrder("order-1") } returns OrderMutationResult.HttpError(
            code = 422,
            problem = OrderProblemDto(errorCode = "INVALID_STATE_TRANSITION"),
        )

        val result = repository.rejectOrder("order-1")

        assertEquals(RejectOrderError.AlreadyHandled, result.errorOf<RejectOrderException>().error)
    }

    @Test
    fun `transport failure maps to Network on reject`() = runTest {
        coEvery { api.rejectOrder("order-1") } returns
            OrderMutationResult.NetworkError(IOException("offline"))

        val result = repository.rejectOrder("order-1")

        assertEquals(RejectOrderError.Network, result.errorOf<RejectOrderException>().error)
    }
}
