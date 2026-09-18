package com.qhathia.mobile.staff.core.network.service

import com.qhathia.mobile.staff.core.network.NetworkResult
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.client.request.url
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import javax.inject.Provider

class ProfileApiServicesTest {

    private val requests = mutableListOf<HttpRequestData>()
    private val bodies = mutableListOf<String>()

    private fun client(
        responder: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
    ): HttpClient = HttpClient(
        MockEngine { request ->
            requests += request
            bodies += request.body.toByteArray().decodeToString()
            responder(request)
        },
    ) {
        expectSuccess = true
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        defaultRequest {
            url("http://localhost/")
            contentType(ContentType.Application.Json)
        }
    }

    private fun MockRequestHandleScope.json(body: String, status: HttpStatusCode = HttpStatusCode.OK) =
        respond(body, status, headersOf("Content-Type", ContentType.Application.Json.toString()))

    private fun bodyObject(index: Int = 0): JsonObject = Json.parseToJsonElement(bodies[index]).jsonObject

    @Test
    fun `sessions carry the refresh token header only when given one`() = runTest {
        val http = client { json("[]") }
        val service = AuthApiService(http, Provider { http })

        service.getSessions("rt-1")
        service.revokeOthers(null)

        assertEquals("rt-1", requests[0].headers[REFRESH_TOKEN_HEADER])
        assertNull(requests[1].headers[REFRESH_TOKEN_HEADER])
    }

    @Test
    fun `preference update body holds only the changed field`() = runTest {
        val service = NotificationApiService(client { json("""{"lowBattery":false,"activeCount":6,"totalCount":7}""") })

        val result = service.updatePreference("lowBattery", false)

        assertInstanceOf(NetworkResult.Success::class.java, result)
        assertEquals(setOf("lowBattery"), bodyObject().keys)
        assertEquals("false", bodyObject()["lowBattery"].toString())
    }

    @Test
    fun `store update body holds only the non-null fields`() = runTest {
        val service = StoreApiService(client { json("""{"id":"s1","language":"en"}""") })

        service.updateStore("s1", language = "en")

        assertEquals("/api/v1/stores/s1", requests.single().url.encodedPath)
        assertEquals(setOf("language"), bodyObject().keys)
    }

    @Test
    fun `subscription 404 surfaces as an http error for the repository to interpret`() = runTest {
        val service = SubscriptionApiService(client { json("{}", HttpStatusCode.NotFound) })

        val result = service.getCurrent()

        assertEquals(404, assertInstanceOf(NetworkResult.HttpError::class.java, result).code)
    }
}
