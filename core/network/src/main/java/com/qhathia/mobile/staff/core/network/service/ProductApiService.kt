package com.qhathia.mobile.staff.core.network.service

import com.qhathia.mobile.staff.core.network.NetworkResult
import com.qhathia.mobile.staff.core.network.dto.CreateMediaRequestDto
import com.qhathia.mobile.staff.core.network.dto.CreateProductRequestDto
import com.qhathia.mobile.staff.core.network.dto.MediaResponseDto
import com.qhathia.mobile.staff.core.network.dto.ProductDto
import com.qhathia.mobile.staff.core.network.dto.ProductResponseDto
import com.qhathia.mobile.staff.core.network.toNetworkResult
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody

/**
 * Wraps the products API. Runs on the authenticated client so the bearer token (and its refresh)
 * is injected by the Ktor Auth plugin. Optional list filters are only sent when non-null.
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

    suspend fun createProduct(body: CreateProductRequestDto): NetworkResult<ProductResponseDto> =
        runCatching {
            client.post("api/v1/products") { setBody(body) }.body<ProductResponseDto>()
        }.toNetworkResult()

    suspend fun attachMedia(
        productId: String,
        body: CreateMediaRequestDto,
    ): NetworkResult<MediaResponseDto> =
        runCatching {
            client.post("api/v1/products/$productId/media") { setBody(body) }.body<MediaResponseDto>()
        }.toNetworkResult()

    suspend fun deactivateProduct(productId: String): NetworkResult<Unit> =
        runCatching {
            client.delete("api/v1/products/$productId")
            Unit
        }.toNetworkResult()
}
