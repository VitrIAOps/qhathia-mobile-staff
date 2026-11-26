package com.qhathia.mobile.staff.core.network

import com.qhathia.mobile.staff.core.network.KtorClientFactory.configure
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class KtorClientFactoryTest {

    private val json = Json { ignoreUnknownKeys = true }

    private class CountingTokens : AuthTokenProvider {
        var refreshCount = 0
        override suspend fun loadTokens() = BearerTokens("access", "refresh")
        override suspend fun refreshTokens(): BearerTokens {
            refreshCount++
            return BearerTokens("access-2", "refresh-2")
        }
    }

    private fun clientAnswering401(body: String, tokens: AuthTokenProvider, requests: MutableList<String>) =
        HttpClient(
            MockEngine { request ->
                requests += request.url.encodedPath
                respond(
                    body,
                    HttpStatusCode.Unauthorized,
                    headersOf("Content-Type" to listOf("application/problem+json"), "WWW-Authenticate" to listOf("Bearer")),
                )
            },
        ) {
            configure(NetworkConfig(baseUrl = "http://localhost/", enableLogging = false), json, tokens)
        }

    @Test
    fun `a wrong current password is not refreshed nor replayed`() = runTest {
        val tokens = CountingTokens()
        val requests = mutableListOf<String>()
        val client = clientAnswering401(
            """{"errorCode":"CURRENT_PASSWORD_INCORRECT","detail":"La contraseña actual es incorrecta"}""",
            tokens,
            requests,
        )

        runCatching { client.post("api/v1/auth/change-password") }

        assertEquals(0, tokens.refreshCount)
        assertEquals(1, requests.size)
    }

    @Test
    fun `any other 401 still refreshes and replays once`() = runTest {
        val tokens = CountingTokens()
        val requests = mutableListOf<String>()
        val client = clientAnswering401("""{"errorCode":"UNAUTHORIZED"}""", tokens, requests)

        val failure = runCatching { client.post("api/v1/products") }.exceptionOrNull()

        assertEquals(1, tokens.refreshCount)
        assertEquals(2, requests.size)
        assertEquals(true, failure is ResponseException)
    }

    @Test
    fun `after forgetBearerTokens the next request loads the current session's token`() = runTest {
        var stored = BearerTokens("access-old", "refresh-old")
        val tokens = object : AuthTokenProvider {
            override suspend fun loadTokens() = stored
            override suspend fun refreshTokens(): BearerTokens? = null
        }
        val sent = mutableListOf<String?>()
        val client = HttpClient(
            MockEngine { request ->
                sent += request.headers[HttpHeaders.Authorization]
                respond("{}", HttpStatusCode.OK, headersOf("Content-Type" to listOf("application/json")))
            },
        ) {
            configure(NetworkConfig(baseUrl = "http://localhost/", enableLogging = false), json, tokens)
        }

        client.get("api/v1/orders")
        stored = BearerTokens("access-new", "refresh-new") // sign out and log in again
        client.get("api/v1/orders")
        client.forgetBearerTokens()
        client.get("api/v1/orders")

        assertEquals(listOf("Bearer access-old", "Bearer access-old", "Bearer access-new"), sent)
    }
}
