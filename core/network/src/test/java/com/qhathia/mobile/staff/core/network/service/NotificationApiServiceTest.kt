package com.qhathia.mobile.staff.core.network.service

import com.qhathia.mobile.staff.core.network.NetworkResult
import com.qhathia.mobile.staff.core.network.dto.RegisterTokenRequestDto
import com.qhathia.mobile.staff.core.network.dto.UnregisterTokenRequestDto
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
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test
import java.io.IOException

class NotificationApiServiceTest {

    private val registerBody = RegisterTokenRequestDto(token = "fcm-abc", platform = "ANDROID")
    private val unregisterBody = UnregisterTokenRequestDto(token = "fcm-abc")

    private fun serviceReturning(handler: MockEngine): NotificationApiService {
        val client = HttpClient(handler) {
            expectSuccess = true
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
            defaultRequest {
                url("http://localhost/")
                contentType(ContentType.Application.Json)
            }
        }
        return NotificationApiService(client)
    }

    @Test
    fun `given 204 response when registerToken then returns Success`() = runTest {
        val service = serviceReturning(
            MockEngine { respond(content = "", status = HttpStatusCode.NoContent) },
        )

        val result = service.registerToken(registerBody)

        assertInstanceOf(NetworkResult.Success::class.java, result)
    }

    @Test
    fun `given 204 response when unregisterToken then returns Success`() = runTest {
        val service = serviceReturning(
            MockEngine { respond(content = "", status = HttpStatusCode.NoContent) },
        )

        val result = service.unregisterToken(unregisterBody)

        assertInstanceOf(NetworkResult.Success::class.java, result)
    }

    @Test
    fun `given 403 response when registerToken then returns HttpError 403`() = runTest {
        val service = serviceReturning(
            MockEngine { respondError(HttpStatusCode.Forbidden) },
        )

        val result = service.registerToken(registerBody)

        val error = assertInstanceOf(NetworkResult.HttpError::class.java, result)
        assertEquals(403, error.code)
    }

    @Test
    fun `given transport failure when unregisterToken then returns NetworkError`() = runTest {
        val service = serviceReturning(
            MockEngine { throw IOException("no connectivity") },
        )

        val result = service.unregisterToken(unregisterBody)

        assertInstanceOf(NetworkResult.NetworkError::class.java, result)
    }
}
