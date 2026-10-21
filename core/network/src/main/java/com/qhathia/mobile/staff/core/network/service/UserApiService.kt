package com.qhathia.mobile.staff.core.network.service

import com.qhathia.mobile.staff.core.network.NetworkResult
import com.qhathia.mobile.staff.core.network.dto.UserProfileDto
import com.qhathia.mobile.staff.core.network.toNetworkResult
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Wraps the user profile API. Runs on the authenticated client; any role may call it. `PUT` is
 * partial — an absent field is left untouched — so its body holds only the fields being changed.
 * There is deliberately no email parameter: the email is the login and the backend ignores it.
 */
class UserApiService(private val client: HttpClient) {

    suspend fun getMe(): NetworkResult<UserProfileDto> =
        runCatching {
            client.get("api/v1/users/me").body<UserProfileDto>()
        }.toNetworkResult()

    /** `PUT /api/v1/users/me` with only the non-null arguments. `422` when a value is invalid. */
    suspend fun updateMe(fullName: String? = null, phone: String? = null): NetworkResult<UserProfileDto> =
        runCatching {
            val body = buildJsonObject {
                fullName?.let { put("fullName", it) }
                phone?.let { put("phone", it) }
            }
            client.put("api/v1/users/me") { setBody(body) }.body<UserProfileDto>()
        }.toNetworkResult()
}
