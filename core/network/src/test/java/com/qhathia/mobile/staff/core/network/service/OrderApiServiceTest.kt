package com.qhathia.mobile.staff.core.network.service

import com.qhathia.mobile.staff.core.network.NetworkResult
import com.qhathia.mobile.staff.core.network.OrderMutationResult
import com.qhathia.mobile.staff.core.network.dto.OrderPageDto
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.url
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.io.IOException

class OrderApiServiceTest {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private val capturedRequests = mutableListOf<HttpRequestData>()

    private fun serviceRespondingWith(
        body: String,
        status: HttpStatusCode = HttpStatusCode.OK,
    ): OrderApiService = serviceReturning(
        MockEngine { request ->
            capturedRequests += request
            respond(
                content = body,
                status = status,
                headers = headersOf("Content-Type", ContentType.Application.Json.toString()),
            )
        },
    )

    private fun serviceReturning(handler: MockEngine): OrderApiService {
        val client = HttpClient(handler) {
            expectSuccess = true
            install(ContentNegotiation) { json(json) }
            defaultRequest {
                url("http://localhost/")
                contentType(ContentType.Application.Json)
            }
        }
        return OrderApiService(client, json)
    }

    private fun lastQuery(name: String): String? =
        capturedRequests.last().url.parameters[name]

    /** The serialized request body, or null when the call sent none. */
    private fun lastBody(): String? =
        (capturedRequests.last().body as? TextContent)?.text

    @Test
    fun `getOrders joins statuses into a single comma-separated param`() = runTest {
        val service = serviceRespondingWith("""{"content":[],"totalElements":0}""")

        service.getOrders(listOf("REJECTED", "EXPIRED"), query = null, page = 0, size = 50)

        assertEquals("REJECTED,EXPIRED", lastQuery("status"))
    }

    @Test
    fun `getOrders omits every optional param that is null`() = runTest {
        val service = serviceRespondingWith("""{"content":[],"totalElements":0}""")

        service.getOrders(emptyList(), query = null, page = null, size = null)

        assertNull(lastQuery("status"))
        assertNull(lastQuery("q"))
        assertNull(lastQuery("page"))
        assertNull(lastQuery("size"))
    }

    @Test
    fun `getOrders sends the search query as q`() = runTest {
        val service = serviceRespondingWith("""{"content":[],"totalElements":0}""")

        service.getOrders(listOf("PENDING"), query = "mari", page = 0, size = 50)

        assertEquals("mari", lastQuery("q"))
        assertEquals("PENDING", lastQuery("status"))
    }

    @Test
    fun `given 200 when getOrders then returns the parsed page`() = runTest {
        val body = """
            {
              "content": [
                {
                  "id": "order-1",
                  "status": "PENDING",
                  "customerName": "Maria Murillo",
                  "itemCount": 2,
                  "total": 156.90,
                  "orderCode": "859653",
                  "expiresAt": "2026-06-29T21:00:00Z"
                }
              ],
              "totalElements": 1,
              "totalPages": 1,
              "size": 20,
              "number": 0
            }
        """.trimIndent()
        val service = serviceRespondingWith(body)

        val result = service.getOrders(listOf("PENDING"), null, 0, 20)

        val page = assertInstanceOf(NetworkResult.Success::class.java, result).data as OrderPageDto
        assertEquals(1, page.totalElements)
        assertEquals("859653", page.content.single().orderCode)
    }

    @Test
    fun `given 200 when confirmOrder then returns the confirmed order`() = runTest {
        val body = """{"id":"order-1","status":"CONFIRMED","total":50.0,"items":[]}"""
        val service = serviceRespondingWith(body)

        val result = service.confirmOrder("order-1")

        val success = assertInstanceOf(OrderMutationResult.Success::class.java, result)
        assertEquals("CONFIRMED", success.order.status)
    }

    @Test
    fun `given 422 STOCK_INSUFFICIENT when confirmOrder then parses the ProblemDetail body`() =
        runTest {
            val body = """
                {
                  "type": "/errors/stock-insufficient",
                  "title": "Stock Insufficient",
                  "status": 422,
                  "detail": "Stock insuficiente para 'Foco LED Bosch 9W'.",
                  "errorCode": "STOCK_INSUFFICIENT",
                  "productId": "product-1",
                  "productName": "Foco LED Bosch 9W",
                  "available": 3,
                  "requested": 10,
                  "timestamp": "2026-06-29T20:05:00Z"
                }
            """.trimIndent()
            val service = serviceRespondingWith(body, HttpStatusCode.UnprocessableEntity)

            val result = service.confirmOrder("order-1")

            val error = assertInstanceOf(OrderMutationResult.HttpError::class.java, result)
            assertEquals(422, error.code)
            assertEquals("STOCK_INSUFFICIENT", error.problem?.errorCode)
            assertEquals("Foco LED Bosch 9W", error.problem?.productName)
            assertEquals(3, error.problem?.available)
            assertEquals(10, error.problem?.requested)
        }

    @Test
    fun `given 422 INVALID_STATE_TRANSITION when rejectOrder then surfaces the error code`() =
        runTest {
            val body = """{"status":422,"errorCode":"INVALID_STATE_TRANSITION"}"""
            val service = serviceRespondingWith(body, HttpStatusCode.UnprocessableEntity)

            val result = service.rejectOrder("order-1", reason = null)

            val error = assertInstanceOf(OrderMutationResult.HttpError::class.java, result)
            assertEquals("INVALID_STATE_TRANSITION", error.problem?.errorCode)
        }

    @Test
    fun `rejectOrder sends the reason in the request body`() = runTest {
        val service = serviceRespondingWith("""{"id":"order-1","status":"REJECTED"}""")

        service.rejectOrder("order-1", reason = "Cantidad insuficiente")

        assertEquals("""{"reason":"Cantidad insuficiente"}""", lastBody())
    }

    @Test
    fun `given no reason when rejectOrder then no body is sent`() = runTest {
        val service = serviceRespondingWith("""{"id":"order-1","status":"REJECTED"}""")

        service.rejectOrder("order-1", reason = null)

        assertNull(lastBody())
    }

    @Test
    fun `given 404 with no parseable body when confirmOrder then problem is null`() = runTest {
        val service = serviceRespondingWith("", HttpStatusCode.NotFound)

        val result = service.confirmOrder("order-1")

        val error = assertInstanceOf(OrderMutationResult.HttpError::class.java, result)
        assertEquals(404, error.code)
        assertNull(error.problem)
    }

    @Test
    fun `given transport failure when confirmOrder then returns NetworkError`() = runTest {
        val service = serviceReturning(MockEngine { throw IOException("no connectivity") })

        val result = service.confirmOrder("order-1")

        assertInstanceOf(OrderMutationResult.NetworkError::class.java, result)
    }
}
