package com.qhathia.mobile.staff.core.network.service

import com.qhathia.mobile.staff.core.network.NetworkResult
import com.qhathia.mobile.staff.core.network.dto.ProductDto
import com.qhathia.mobile.staff.core.network.toNetworkResult
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

/**
 * Wraps `GET /api/v1/products`. Runs on the authenticated client so the bearer token (and its
 * refresh) is injected by the Ktor Auth plugin. Optional filters are only sent when non-null.
 */
class ProductApiService(private val client: HttpClient) {

    suspend fun getProducts(
        categoryId: String?,
        availability: String?,
    ): NetworkResult<List<ProductDto>> =
        runCatching {
            client.get("api/v1/products") {
                categoryId?.let { parameter("categoryId", it) }
                availability?.let { parameter("availability", it) }
            }.body<List<ProductDto>>()
        }.toNetworkResult()
}
