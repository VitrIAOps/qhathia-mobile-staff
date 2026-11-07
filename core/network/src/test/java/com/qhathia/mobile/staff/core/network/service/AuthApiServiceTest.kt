package com.qhathia.mobile.staff.core.network.service

import com.qhathia.mobile.staff.core.network.NetworkResult
import com.qhathia.mobile.staff.core.network.dto.AuthResponseDto
import com.qhathia.mobile.staff.core.network.dto.ChangePasswordRequestDto
import com.qhathia.mobile.staff.core.network.dto.LoginRequestDto
import com.qhathia.mobile.staff.core.network.dto.LogoutRequestDto
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
import javax.inject.Provider
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test
import java.io.IOException

class AuthApiServiceTest {

    private val request = LoginRequestDto(email = "owner@tienda.com", password = "secret123")

    private fun serviceReturning(handler: MockEngine): AuthApiService {
        val client = HttpClient(handler) {
            expectSuccess = true
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
            defaultRequest {
                url("http://localhost/")
                contentType(ContentType.Application.Json)
            }
        }
        return AuthApiService(client, Provider { client }, Json { ignoreUnknownKeys = true })
    }

    @Test
    fun `given 200 response when login then returns Success with parsed body`() = runTest {
        val body = """
            {
              "accessToken": "eyJ-access",
              "refreshToken": "ref-123",
              "tokenType": "Bearer",
              "expiresIn": 86400,
              "user": { "id": "u1", "email": "owner@tienda.com", "fullName": "Juan", "role": "OWNER" },
              "store": { "id": "s1", "shoppingCenterId": "sc1", "name": "Tienda", "category": "TEXTILE" },
              "stores": []
            }
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

        val result = service.login(request)

        assertInstanceOf(NetworkResult.Success::class.java, result)
        @Suppress("UNCHECKED_CAST")
        val data = (result as NetworkResult.Success<AuthResponseDto>).data
        assertEquals("eyJ-access", data.accessToken)
        assertEquals("OWNER", data.user?.role)
    }

    @Test
    fun `given 401 response when login then returns HttpError 401`() = runTest {
        val service = serviceReturning(
            MockEngine { respondError(HttpStatusCode.Unauthorized) },
        )

        val result = service.login(request)

        val error = assertInstanceOf(NetworkResult.HttpError::class.java, result)
        assertEquals(401, error.code)
    }

    @Test
    fun `given 400 response when login then returns HttpError 400`() = runTest {
        val service = serviceReturning(
            MockEngine { respondError(HttpStatusCode.BadRequest) },
        )

        val result = service.login(request)

        val error = assertInstanceOf(NetworkResult.HttpError::class.java, result)
        assertEquals(400, error.code)
    }

    @Test
    fun `given transport failure when login then returns NetworkError`() = runTest {
        val service = serviceReturning(
            MockEngine { throw IOException("no connectivity") },
        )

        val result = service.login(request)

        assertInstanceOf(NetworkResult.NetworkError::class.java, result)
    }

    @Test
    fun `given 204 response when logout then returns Success`() = runTest {
        val service = serviceReturning(
            MockEngine { respond(content = "", status = HttpStatusCode.NoContent) },
        )

        val result = service.logout(LogoutRequestDto(refreshToken = "ref-123"))

        assertInstanceOf(NetworkResult.Success::class.java, result)
    }

    @Test
    fun `given 401 response when logout then returns HttpError 401`() = runTest {
        val service = serviceReturning(
            MockEngine { respondError(HttpStatusCode.Unauthorized) },
        )

        val result = service.logout(LogoutRequestDto(refreshToken = "ref-123"))

        val error = assertInstanceOf(NetworkResult.HttpError::class.java, result)
        assertEquals(401, error.code)
    }

    @Test
    fun `given 401 CURRENT_PASSWORD_INCORRECT when changePassword then exposes errorCode and detail`() =
        runTest {
            val service = serviceReturning(
                MockEngine {
                    respond(
                        content = """{"errorCode":"CURRENT_PASSWORD_INCORRECT","detail":"La contraseña actual no es correcta"}""",
                        status = HttpStatusCode.Unauthorized,
                        headers = headersOf("Content-Type", "application/problem+json"),
                    )
                },
            )

            val result = service.changePassword(ChangePasswordRequestDto("old", "new-password"))

            val error = assertInstanceOf(NetworkResult.HttpError::class.java, result)
            assertEquals(401, error.code)
            assertEquals("CURRENT_PASSWORD_INCORRECT", error.problem?.errorCode)
            assertEquals("La contraseña actual no es correcta", error.problem?.detail)
        }
}
