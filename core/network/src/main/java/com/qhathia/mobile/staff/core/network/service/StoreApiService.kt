package com.qhathia.mobile.staff.core.network.service

import com.qhathia.mobile.staff.core.network.NetworkResult
import com.qhathia.mobile.staff.core.network.dto.StoreSettingsDto
import com.qhathia.mobile.staff.core.network.toNetworkResult
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Wraps the store API. `GET` accepts any role; `PUT` is OWNER/ADMIN and partial — an absent field is
 * left untouched, so the body is built with only the fields being changed.
 */
class StoreApiService(private val client: HttpClient) {

    suspend fun getStore(storeId: String): NetworkResult<StoreSettingsDto> =
        runCatching {
            client.get("api/v1/stores/$storeId").body<StoreSettingsDto>()
        }.toNetworkResult()

    /** Sends only the non-null arguments. */
    suspend fun updateStore(
        storeId: String,
        language: String? = null,
    ): NetworkResult<StoreSettingsDto> =
        runCatching {
            val body = buildJsonObject {
                language?.let { put("language", it) }
            }
            client.put("api/v1/stores/$storeId") { setBody(body) }.body<StoreSettingsDto>()
        }.toNetworkResult()
}
