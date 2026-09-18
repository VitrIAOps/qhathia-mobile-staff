package com.qhathia.mobile.staff.core.network.service

import com.qhathia.mobile.staff.core.network.NetworkResult
import com.qhathia.mobile.staff.core.network.dto.NotificationPreferencesDto
import com.qhathia.mobile.staff.core.network.dto.RegisterTokenRequestDto
import com.qhathia.mobile.staff.core.network.dto.UnregisterTokenRequestDto
import com.qhathia.mobile.staff.core.network.toNetworkResult
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Wraps the notification API (§9.6, §20). Runs on the authenticated client so the OWNER/SELLER
 * bearer token (and its refresh) is injected by the Ktor Auth plugin — the backend derives the
 * store/user from the JWT. The token endpoints answer `204 No Content`, so their body is ignored.
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

    suspend fun getPreferences(): NetworkResult<NotificationPreferencesDto> =
        runCatching {
            client.get("api/v1/notifications/preferences").body<NotificationPreferencesDto>()
        }.toNetworkResult()

    /**
     * Partial update carrying only [field] (e.g. `lowBattery`), so two phones of the same user
     * editing different toggles never overwrite each other. Answers the full set as applied.
     */
    suspend fun updatePreference(
        field: String,
        enabled: Boolean,
    ): NetworkResult<NotificationPreferencesDto> =
        runCatching {
            val body = buildJsonObject { put(field, enabled) }
            client.put("api/v1/notifications/preferences") { setBody(body) }
                .body<NotificationPreferencesDto>()
        }.toNetworkResult()
}
