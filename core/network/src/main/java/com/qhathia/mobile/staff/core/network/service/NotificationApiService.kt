package com.qhathia.mobile.staff.core.network.service

import com.qhathia.mobile.staff.core.network.NetworkResult
import com.qhathia.mobile.staff.core.network.dto.RegisterTokenRequestDto
import com.qhathia.mobile.staff.core.network.dto.UnregisterTokenRequestDto
import com.qhathia.mobile.staff.core.network.toNetworkResult
import io.ktor.client.HttpClient
import io.ktor.client.request.delete
import io.ktor.client.request.post
import io.ktor.client.request.setBody

/**
 * Wraps the notification-token API (§9.6). Runs on the authenticated client so the OWNER/SELLER
 * bearer token (and its refresh) is injected by the Ktor Auth plugin — the backend derives the
 * store/user from the JWT. Both endpoints answer `204 No Content`, so the response body is ignored
 * and success maps to `Unit`.
 */
class NotificationApiService(private val client: HttpClient) {

    suspend fun registerToken(body: RegisterTokenRequestDto): NetworkResult<Unit> =
        runCatching {
            client.post("api/v1/notifications/tokens") { setBody(body) }
            Unit
        }.toNetworkResult()

    suspend fun unregisterToken(body: UnregisterTokenRequestDto): NetworkResult<Unit> =
        runCatching {
            client.delete("api/v1/notifications/tokens") { setBody(body) }
            Unit
        }.toNetworkResult()
}
