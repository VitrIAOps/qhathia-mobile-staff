package com.qhathia.mobile.staff.core.network.service

import com.qhathia.mobile.staff.core.network.NetworkResult
import com.qhathia.mobile.staff.core.network.dto.AuthResponseDto
import com.qhathia.mobile.staff.core.network.dto.LoginRequestDto
import com.qhathia.mobile.staff.core.network.dto.RefreshRequestDto
import com.qhathia.mobile.staff.core.network.toNetworkResult
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody

/**
 * Wraps the public Auth API endpoints. Both calls run on the unauthenticated client so a failed
 * refresh can never re-trigger the bearer Auth plugin.
 */
class AuthApiService(private val client: HttpClient) {

    suspend fun login(request: LoginRequestDto): NetworkResult<AuthResponseDto> =
        runCatching {
            client.post("api/v1/auth/login") { setBody(request) }.body<AuthResponseDto>()
        }.toNetworkResult()

    suspend fun refresh(request: RefreshRequestDto): NetworkResult<AuthResponseDto> =
        runCatching {
            client.post("api/v1/auth/refresh") { setBody(request) }.body<AuthResponseDto>()
        }.toNetworkResult()
}
