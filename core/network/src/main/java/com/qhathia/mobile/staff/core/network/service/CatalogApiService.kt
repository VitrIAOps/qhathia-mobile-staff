package com.qhathia.mobile.staff.core.network.service

import com.qhathia.mobile.staff.core.network.NetworkResult
import com.qhathia.mobile.staff.core.network.dto.CatalogCountDto
import com.qhathia.mobile.staff.core.network.toNetworkResult
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get

/** Wraps the aggregated catalog API. Runs on the authenticated client. */
class CatalogApiService(private val client: HttpClient) {

    /** The store's active product count, without downloading the manifest. */
    suspend fun getCount(): NetworkResult<CatalogCountDto> =
        runCatching {
            client.get("api/v1/catalog/count").body<CatalogCountDto>()
        }.toNetworkResult()
}
