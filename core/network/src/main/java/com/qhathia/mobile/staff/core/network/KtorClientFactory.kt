package com.qhathia.mobile.staff.core.network

import android.util.Log
import kotlin.coroutines.cancellation.CancellationException
import io.ktor.http.HttpStatusCode
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.HttpResponse
import io.ktor.client.HttpClientConfig
import com.qhathia.mobile.staff.core.network.dto.ProblemDto
import com.qhathia.mobile.staff.core.network.service.REFRESH_TOKEN_HEADER
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

    /** Logcat tag of the debug HTTP log: `adb logcat -s KtorClient`. */
    private const val LOG_TAG = "KtorClient"

    /**
     * @param tokenProvider when non-null, installs the bearer [Auth] plugin (token load + refresh).
     *   Pass null for the public client used by login/refresh, avoiding any refresh recursion.
     */
    fun create(
        config: NetworkConfig,
        json: Json,
        tokenProvider: AuthTokenProvider?,
    ): HttpClient = HttpClient(CIO) { configure(config, json, tokenProvider) }

    /** The whole client setup, apart from the engine, so tests can run it on a mock engine. */
    internal fun HttpClientConfig<*>.configure(
        config: NetworkConfig,
        json: Json,
        tokenProvider: AuthTokenProvider?,
    ) {
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
                // Bodies stay, so the backend's answers can be read; their secrets (passwords,
                // tokens, the exit PIN, phone and email) do not. Debug builds only.
                logger = object : Logger {
                    override fun log(message: String) {
                        Log.d(LOG_TAG, redactSecrets(message))
                    }
                }
                level = LogLevel.BODY
                // Credentials never reach the log, even in debug builds.
                sanitizeHeader { name ->
                    name.equals(HttpHeaders.Authorization, ignoreCase = true) ||
                        name.equals(REFRESH_TOKEN_HEADER, ignoreCase = true)
                }
            }
        }
        if (tokenProvider != null) {
            install(Auth) {
                // A wrong current password is a 401 too, but not an expired token: refreshing and
                // replaying would cost a request and, if the refresh failed, the whole session.
                reAuthorizeOnResponse { response ->
                    response.status == HttpStatusCode.Unauthorized &&
                        response.problemErrorCode(json) != ERROR_CURRENT_PASSWORD_INCORRECT
                }
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

/** The `errorCode` of a ProblemDetail body, best effort; null when it cannot be read. */
private suspend fun HttpResponse.problemErrorCode(json: Json): String? =
    try {
        json.decodeFromString<ProblemDto>(bodyAsText()).errorCode
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null
    }

/** The 401 that means "the current password is wrong", not "the token expired" (Fase 36). */
internal const val ERROR_CURRENT_PASSWORD_INCORRECT = "CURRENT_PASSWORD_INCORRECT"
