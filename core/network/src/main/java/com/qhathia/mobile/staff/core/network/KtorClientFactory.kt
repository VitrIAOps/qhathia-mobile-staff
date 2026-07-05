package com.qhathia.mobile.staff.core.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.header
import io.ktor.client.request.url
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * Builds the project's [HttpClient] (Ktor, CIO engine). The instance is created once and provided
 * as a singleton by [com.qhathia.mobile.staff.core.network.di.NetworkModule]; it must not be
 * instantiated elsewhere.
 */
object KtorClientFactory {

    private const val REQUEST_TIMEOUT_MS = 30_000L
    private const val CONNECT_TIMEOUT_MS = 15_000L
    private const val SOCKET_TIMEOUT_MS = 30_000L

    /**
     * @param tokenProvider when non-null, installs the bearer [Auth] plugin (token load + refresh).
     *   Pass null for the public client used by login/refresh, avoiding any refresh recursion.
     */
    fun create(
        config: NetworkConfig,
        json: Json,
        tokenProvider: AuthTokenProvider?,
    ): HttpClient = HttpClient(CIO) {
        // Non-2xx responses throw a ResponseException, which toNetworkResult() maps to HttpError.
        expectSuccess = true

        install(ContentNegotiation) {
            json(json)
        }
        install(HttpTimeout) {
            requestTimeoutMillis = REQUEST_TIMEOUT_MS
            connectTimeoutMillis = CONNECT_TIMEOUT_MS
            socketTimeoutMillis = SOCKET_TIMEOUT_MS
        }
        if (config.enableLogging) {
            install(Logging) {
                logger = object : Logger {
                    override fun log(message: String) {
                        println("[KtorClient] $message")
                    }
                }
                level = LogLevel.BODY
            }
        }
        if (tokenProvider != null) {
            install(Auth) {
                bearer {
                    loadTokens { tokenProvider.loadTokens() }
                    refreshTokens { tokenProvider.refreshTokens() }
                }
            }
        }
        defaultRequest {
            url(config.baseUrl)
            contentType(ContentType.Application.Json)
            header(HttpHeaders.AcceptLanguage, "es")
        }
    }
}
