package com.qhathia.mobile.staff.core.network.service

import com.qhathia.mobile.staff.core.network.NetworkResult
import com.qhathia.mobile.staff.core.network.dto.CreateProductRequestDto
import com.qhathia.mobile.staff.core.network.dto.UpdateProductRequestDto
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
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test

class ProductApiServiceTest {

    private val json = Json { ignoreUnknownKeys = true }

    private fun serviceAnswering(body: String, status: HttpStatusCode): ProductApiService {
        val client = HttpClient(
            MockEngine {
                respond(body, status, headersOf("Content-Type", "application/problem+json"))
            },
        ) {
            expectSuccess = true
            install(ContentNegotiation) { json(json) }
            defaultRequest {
                url("http://localhost/")
                contentType(ContentType.Application.Json)
            }
        }
        return ProductApiService(client, json)
    }

    @Test
    fun `given 409 PRODUCT_VERSION_CONFLICT when updateProduct then exposes currentVersion`() = runTest {
        val service = serviceAnswering(
            """{"errorCode":"PRODUCT_VERSION_CONFLICT","detail":"Otra persona modificó este producto.","currentVersion":8}""",
            HttpStatusCode.Conflict,
        )

        val result = service.updateProduct("p1", UpdateProductRequestDto(name = "Taladro", price = 199.0, stock = 3))

        val error = assertInstanceOf(NetworkResult.HttpError::class.java, result)
        assertEquals(409, error.code)
        assertEquals("PRODUCT_VERSION_CONFLICT", error.problem?.errorCode)
        assertEquals(8L, error.problem?.currentVersion)
    }

    @Test
    fun `given 409 PRODUCT_IN_PENDING_ORDERS when deleteProduct then exposes the pending orders`() = runTest {
        val service = serviceAnswering(
            """{"errorCode":"PRODUCT_IN_PENDING_ORDERS","detail":"Está en 1 pedido pendiente (#400776).",
               "pendingOrders":[{"id":"o1","orderCode":"400776"}]}""",
            HttpStatusCode.Conflict,
        )

        val result = service.deleteProduct("p1")

        val error = assertInstanceOf(NetworkResult.HttpError::class.java, result)
        assertEquals("PRODUCT_IN_PENDING_ORDERS", error.problem?.errorCode)
        assertEquals(listOf("400776"), error.problem?.pendingOrders?.map { it.orderCode })
    }

    private val requests = mutableListOf<HttpRequestData>()

    private fun recordingService(status: HttpStatusCode = HttpStatusCode.OK, body: String = "{}"): ProductApiService {
        val client = HttpClient(
            MockEngine { request ->
                requests += request
                respond(body, status, headersOf("Content-Type", "application/json"))
            },
        ) {
            expectSuccess = true
            install(ContentNegotiation) { json(json) }
            defaultRequest {
                url("http://localhost/")
                contentType(ContentType.Application.Json)
            }
        }
        return ProductApiService(client, json)
    }

    @Test
    fun `creating with images sends the idempotency key`() = runTest {
        val service = recordingService(body = """{"id":"p1"}""")

        service.createProductWithImages(
            CreateProductRequestDto(name = "Taladro", price = 199.0, stock = 3),
            emptyList(),
            idempotencyKey = "8a3f6f8e-1c2d-4e5f-9a0b-1c2d3e4f5a6b",
        )

        assertEquals("8a3f6f8e-1c2d-4e5f-9a0b-1c2d3e4f5a6b", requests.single().headers["Idempotency-Key"])
    }

    @Test
    fun `deleting sends force only when asked`() = runTest {
        val service = recordingService(status = HttpStatusCode.NoContent, body = "")

        service.deleteProduct("p1")
        service.deleteProduct("p1", force = true)

        assertEquals(null, requests[0].url.parameters["force"])
        assertEquals("true", requests[1].url.parameters["force"])
    }
}
