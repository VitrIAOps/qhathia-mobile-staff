package com.qhathia.mobile.staff.core.network

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.io.IOException

class NetworkResultTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    private val problemJson = headersOf("Content-Type", "application/problem+json")

    private fun client(engine: MockEngine) = HttpClient(engine) { expectSuccess = true }

    private suspend fun callWith(engine: MockEngine): NetworkResult<String> {
        val client = client(engine)
        return runCatching { client.get("http://localhost/x").body<String>() }.toNetworkResult(json)
    }

    @Test
    fun `version conflict reads errorCode detail and currentVersion`() = runTest {
        val body = """
            {"type":"/errors/conflict","title":"Conflict","status":409,"instance":"/api/v1/products/p1",
             "timestamp":"2026-10-29T15:00:00Z","errorCode":"PRODUCT_VERSION_CONFLICT",
             "detail":"Otra persona modificó este producto mientras lo editabas.","currentVersion":8}
        """.trimIndent()

        val result = callWith(MockEngine { respond(body, HttpStatusCode.Conflict, problemJson) })

        val error = assertInstanceOf(NetworkResult.HttpError::class.java, result)
        assertEquals(409, error.code)
        assertEquals("PRODUCT_VERSION_CONFLICT", error.problem?.errorCode)
        assertEquals("Otra persona modificó este producto mientras lo editabas.", error.problem?.detail)
        assertEquals(8L, error.problem?.currentVersion)
    }

    @Test
    fun `pending orders are read`() = runTest {
        val body = """
            {"errorCode":"PRODUCT_IN_PENDING_ORDERS","detail":"Está en 1 pedido pendiente.",
             "pendingOrders":[{"id":"o1","orderCode":"400776"}]}
        """.trimIndent()

        val result = callWith(MockEngine { respond(body, HttpStatusCode.Conflict, problemJson) })

        val problem = assertInstanceOf(NetworkResult.HttpError::class.java, result).problem
        assertEquals(1, problem?.pendingOrders?.size)
        assertEquals("o1", problem?.pendingOrders?.first()?.id)
        assertEquals("400776", problem?.pendingOrders?.first()?.orderCode)
    }

    @Test
    fun `empty body keeps the status with no problem`() = runTest {
        val result = callWith(MockEngine { respond("", HttpStatusCode.InternalServerError) })

        val error = assertInstanceOf(NetworkResult.HttpError::class.java, result)
        assertEquals(500, error.code)
        assertNull(error.problem)
    }

    @Test
    fun `html body keeps the status with no problem`() = runTest {
        val html = "<html><body><h1>502 Bad Gateway</h1></body></html>"
        val result = callWith(
            MockEngine {
                respond(html, HttpStatusCode.BadGateway, headersOf("Content-Type", ContentType.Text.Html.toString()))
            },
        )

        val error = assertInstanceOf(NetworkResult.HttpError::class.java, result)
        assertEquals(502, error.code)
        assertNull(error.problem)
    }

    @Test
    fun `transport failure stays a network error`() = runTest {
        val result = callWith(MockEngine { throw IOException("no connectivity") })

        assertInstanceOf(NetworkResult.NetworkError::class.java, result)
    }

    @Test
    fun `success is mapped to Success`() = runTest {
        val result = callWith(MockEngine { respond("ok", HttpStatusCode.OK) })

        assertEquals(NetworkResult.Success("ok"), result)
    }
}
