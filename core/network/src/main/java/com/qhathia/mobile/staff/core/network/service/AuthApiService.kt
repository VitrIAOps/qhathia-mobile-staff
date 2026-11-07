package com.qhathia.mobile.staff.core.network.service

import com.qhathia.mobile.staff.core.network.NetworkResult
import com.qhathia.mobile.staff.core.network.dto.ActiveSessionDto
import com.qhathia.mobile.staff.core.network.dto.AuthResponseDto
import com.qhathia.mobile.staff.core.network.dto.ChangePasswordRequestDto
import com.qhathia.mobile.staff.core.network.dto.LoginRequestDto
import com.qhathia.mobile.staff.core.network.dto.LogoutRequestDto
import com.qhathia.mobile.staff.core.network.dto.RefreshRequestDto
import com.qhathia.mobile.staff.core.network.toNetworkResult
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import kotlinx.serialization.json.Json
import javax.inject.Provider

/** Header carrying the caller's refresh token so the backend can tell which session is "this one". */
const val REFRESH_TOKEN_HEADER = "X-Refresh-Token"

/**
 * Wraps the Auth API endpoints. [login] and [refresh] run on the unauthenticated [publicClient] so
 * a failed refresh can never re-trigger the bearer Auth plugin. Every other call requires the JWT,
 * so it runs on the authenticated client — injected as a [Provider] to break the DI cycle (the
 * authenticated client depends, transitively, on this service via the token provider); it is only
 * resolved when such a call is actually made. `204 No Content` answers ignore the body.
 */
class AuthApiService(
    private val publicClient: HttpClient,
    private val authenticatedClient: Provider<HttpClient>,
    private val json: Json,
) {

    suspend fun login(request: LoginRequestDto): NetworkResult<AuthResponseDto> =
        runCatching {
            publicClient.post("api/v1/auth/login") { setBody(request) }.body<AuthResponseDto>()
        }.toNetworkResult(json)

    suspend fun refresh(request: RefreshRequestDto): NetworkResult<AuthResponseDto> =
        runCatching {
            publicClient.post("api/v1/auth/refresh") { setBody(request) }.body<AuthResponseDto>()
        }.toNetworkResult(json)

    suspend fun logout(request: LogoutRequestDto): NetworkResult<Unit> =
        runCatching {
            authenticatedClient.get().post("api/v1/auth/logout") { setBody(request) }
            Unit
        }.toNetworkResult(json)

    /**
     * The user's sessions. With [refreshToken] the backend flags this device as `current`; without
     * it every row comes back `current = false`.
     */
    suspend fun getSessions(refreshToken: String?): NetworkResult<List<ActiveSessionDto>> =
        runCatching {
            authenticatedClient.get().get("api/v1/auth/sessions") {
                refreshTokenHeader(refreshToken)
            }.body<List<ActiveSessionDto>>()
        }.toNetworkResult(json)

    suspend fun revokeSession(sessionId: String): NetworkResult<Unit> =
        runCatching {
            authenticatedClient.get().delete("api/v1/auth/sessions/$sessionId")
            Unit
        }.toNetworkResult(json)

    /**
     * Closes every session except the one [refreshToken] belongs to. Without the token the backend
     * closes all of them, including this device's — callers must always pass it.
     */
    suspend fun revokeOthers(refreshToken: String?): NetworkResult<Unit> =
        runCatching {
            authenticatedClient.get().post("api/v1/auth/sessions/revoke-others") {
                refreshTokenHeader(refreshToken)
            }
            Unit
        }.toNetworkResult(json)

    suspend fun changePassword(request: ChangePasswordRequestDto): NetworkResult<Unit> =
        runCatching {
            authenticatedClient.get().post("api/v1/auth/change-password") { setBody(request) }
            Unit
        }.toNetworkResult(json)

    private fun HttpRequestBuilder.refreshTokenHeader(refreshToken: String?) {
        if (refreshToken != null) header(REFRESH_TOKEN_HEADER, refreshToken)
    }
}
