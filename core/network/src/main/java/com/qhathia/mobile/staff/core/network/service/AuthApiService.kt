package com.qhathia.mobile.staff.core.network.service

import com.qhathia.mobile.staff.core.network.NetworkResult
import com.qhathia.mobile.staff.core.network.dto.AuthResponseDto
import com.qhathia.mobile.staff.core.network.dto.LoginRequestDto
import com.qhathia.mobile.staff.core.network.dto.LogoutRequestDto
import com.qhathia.mobile.staff.core.network.dto.RefreshRequestDto
import com.qhathia.mobile.staff.core.network.toNetworkResult
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import javax.inject.Provider

/**
 * Wraps the Auth API endpoints. [login] and [refresh] run on the unauthenticated [publicClient] so
 * a failed refresh can never re-trigger the bearer Auth plugin. [logout] requires the JWT, so it
 * runs on the authenticated client — injected as a [Provider] to break the DI cycle (the
 * authenticated client depends, transitively, on this service via the token provider); it is only
 * resolved when [logout] is actually called. Logout answers `204 No Content`, so the body is ignored.
 */
class AuthApiService(
    private val publicClient: HttpClient,
    private val authenticatedClient: Provider<HttpClient>,
) {

    suspend fun login(request: LoginRequestDto): NetworkResult<AuthResponseDto> =
        runCatching {
            publicClient.post("api/v1/auth/login") { setBody(request) }.body<AuthResponseDto>()
        }.toNetworkResult()

    suspend fun refresh(request: RefreshRequestDto): NetworkResult<AuthResponseDto> =
        runCatching {
            publicClient.post("api/v1/auth/refresh") { setBody(request) }.body<AuthResponseDto>()
        }.toNetworkResult()

    suspend fun logout(request: LogoutRequestDto): NetworkResult<Unit> =
        runCatching {
            authenticatedClient.get().post("api/v1/auth/logout") { setBody(request) }
            Unit
        }.toNetworkResult()
}
