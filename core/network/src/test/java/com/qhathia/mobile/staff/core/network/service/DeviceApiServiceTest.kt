package com.qhathia.mobile.staff.core.network.service

import com.qhathia.mobile.staff.core.network.NetworkResult
import com.qhathia.mobile.staff.core.network.dto.DeviceDto
import com.qhathia.mobile.staff.core.network.dto.PairDeviceRequestDto
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
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
import java.io.IOException

class DeviceApiServiceTest {

    private val pairRequest = PairDeviceRequestDto(
        pairingToken = "AB3CD",
        decision = "APPROVE",
        deviceName = "Tablet",
        platform = "ANDROID",
        appVersion = "1.0.0",
    )

    private fun serviceReturning(handler: MockEngine): DeviceApiService {
        val client = HttpClient(handler) {
            expectSuccess = true
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
            defaultRequest {
                url("http://localhost/")
                contentType(ContentType.Application.Json)
            }
        }
        return DeviceApiService(client)
    }

    @Test
    fun `given 200 response when getDevices then returns Success with parsed list`() = runTest {
        val body = """
            [
              {
                "id": "d1",
                "storeId": "s1",
                "serialNumber": "SN-001",
                "status": "ACTIVE",
                "androidVersion": "13",
                "appVersion": "1.0.0",
                "linkedAt": "2026-04-15T18:30:00Z"
              }
            ]
        """.trimIndent()
        val service = serviceReturning(
            MockEngine {
                respond(
                    content = body,
                    status = HttpStatusCode.OK,
                    headers = headersOf("Content-Type", ContentType.Application.Json.toString()),
                )
            },
        )

        val result = service.getDevices()

        assertInstanceOf(NetworkResult.Success::class.java, result)
        @Suppress("UNCHECKED_CAST")
        val data = (result as NetworkResult.Success<List<DeviceDto>>).data
        assertEquals(1, data.size)
        assertEquals("d1", data.first().id)
        assertEquals("ACTIVE", data.first().status)
    }

    @Test
    fun `given 401 response when getDevices then returns HttpError 401`() = runTest {
        val service = serviceReturning(
            MockEngine { respondError(HttpStatusCode.Unauthorized) },
        )

        val result = service.getDevices()

        val error = assertInstanceOf(NetworkResult.HttpError::class.java, result)
        assertEquals(401, error.code)
    }

    @Test
    fun `given 200 response when pairDevice then returns Success`() = runTest {
        val body = """
            { "sessionId": "sess-1", "status": "APPROVED", "storeId": "s1", "deviceId": "d1" }
        """.trimIndent()
        val service = serviceReturning(
            MockEngine {
                respond(
                    content = body,
                    status = HttpStatusCode.OK,
                    headers = headersOf("Content-Type", ContentType.Application.Json.toString()),
                )
            },
        )

        val result = service.pairDevice(pairRequest)

        assertInstanceOf(NetworkResult.Success::class.java, result)
    }

    @Test
    fun `given 400 response when pairDevice then returns HttpError 400`() = runTest {
        val service = serviceReturning(
            MockEngine { respondError(HttpStatusCode.BadRequest) },
        )

        val result = service.pairDevice(pairRequest)

        val error = assertInstanceOf(NetworkResult.HttpError::class.java, result)
        assertEquals(400, error.code)
    }

    @Test
    fun `given transport failure when pairDevice then returns NetworkError`() = runTest {
        val service = serviceReturning(
            MockEngine { throw IOException("no connectivity") },
        )

        val result = service.pairDevice(pairRequest)

        assertInstanceOf(NetworkResult.NetworkError::class.java, result)
    }
}
