package com.qhathia.mobile.staff.core.network.service

import com.qhathia.mobile.staff.core.network.NetworkResult
import com.qhathia.mobile.staff.core.network.dto.CategoryDto
import com.qhathia.mobile.staff.core.network.toNetworkResult
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get

/** Wraps `GET /api/v1/categories` on the authenticated client. */
class CategoryApiService(private val client: HttpClient) {

    suspend fun getCategories(): NetworkResult<List<CategoryDto>> =
        runCatching {
            client.get("api/v1/categories").body<List<CategoryDto>>()
        }.toNetworkResult()
}
