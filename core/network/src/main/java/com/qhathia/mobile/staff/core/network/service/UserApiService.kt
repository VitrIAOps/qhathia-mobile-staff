package com.qhathia.mobile.staff.core.network.service

import com.qhathia.mobile.staff.core.network.NetworkResult
import com.qhathia.mobile.staff.core.network.dto.UserProfileDto
import com.qhathia.mobile.staff.core.network.toNetworkResult
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get

/** Wraps the user profile API. Runs on the authenticated client; any role may call it. */
class UserApiService(private val client: HttpClient) {

    suspend fun getMe(): NetworkResult<UserProfileDto> =
        runCatching {
            client.get("api/v1/users/me").body<UserProfileDto>()
        }.toNetworkResult()
}
