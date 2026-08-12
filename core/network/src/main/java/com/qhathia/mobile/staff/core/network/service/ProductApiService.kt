package com.qhathia.mobile.staff.core.network.service

import com.qhathia.mobile.staff.core.network.NetworkResult
import com.qhathia.mobile.staff.core.network.dto.CreateProductRequestDto
import com.qhathia.mobile.staff.core.network.dto.DraftFromImageResponseDto
import com.qhathia.mobile.staff.core.network.dto.MultipartImage
import com.qhathia.mobile.staff.core.network.dto.ProductDetailDto
import com.qhathia.mobile.staff.core.network.dto.ProductDto
import com.qhathia.mobile.staff.core.network.dto.ProductResponseDto
import com.qhathia.mobile.staff.core.network.dto.RegenerateDescriptionRequestDto
import com.qhathia.mobile.staff.core.network.dto.RegenerateDescriptionResponseDto
import com.qhathia.mobile.staff.core.network.toNetworkResult
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import kotlinx.serialization.json.Json

/**
 * Wraps the products API. Runs on the authenticated client so the bearer token (and its refresh)
 * is injected by the Ktor Auth plugin. Optional list filters are only sent when non-null.
 */
class ProductApiService(
    private val client: HttpClient,
    private val json: Json,
) {

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

    /** Loads one product with its media and every field the list row omits. */
    suspend fun getProduct(id: String): NetworkResult<ProductDetailDto> =
        runCatching {
            client.get("api/v1/products/$id").body<ProductDetailDto>()
        }.toNetworkResult()

    /**
     * Replaces the editable fields of a product. The endpoint is a full replace and takes the same
     * body shape as the create request, so [product] must carry every field.
     */
    suspend fun updateProduct(
        id: String,
        product: CreateProductRequestDto,
    ): NetworkResult<ProductDetailDto> =
        runCatching {
            client.put("api/v1/products/$id") {
                setBody(product)
            }.body<ProductDetailDto>()
        }.toNetworkResult()

    /**
     * Creates a product and its images in a single atomic multipart request: a `product` JSON part
     * plus one `images` part per photo (the first is treated as primary by the backend). The backend
     * uploads the images to storage and creates product + media rows in one transaction.
     */
    suspend fun createProductWithImages(
        product: CreateProductRequestDto,
        images: List<MultipartImage>,
    ): NetworkResult<ProductResponseDto> =
        runCatching {
            val parts = formData {
                append(
                    "product",
                    json.encodeToString(CreateProductRequestDto.serializer(), product),
                    Headers.build { append(HttpHeaders.ContentType, "application/json") },
                )
                images.forEach { image ->
                    append(
                        "images",
                        image.bytes,
                        Headers.build {
                            append(HttpHeaders.ContentType, image.contentType)
                            append(HttpHeaders.ContentDisposition, "filename=\"${image.filename}\"")
                        },
                    )
                }
            }
            client.post("api/v1/products/with-images") {
                setBody(MultiPartFormDataContent(parts))
            }.body<ProductResponseDto>()
        }.toNetworkResult()

    /**
     * Drafts a description, suggested category and tags from a single product [image] via the LLM.
     * The endpoint is fail-open (always 200 on LLM failure) and does not persist the image.
     */
    suspend fun draftFromImage(image: MultipartImage): NetworkResult<DraftFromImageResponseDto> =
        runCatching {
            val parts = formData {
                append(
                    "image",
                    image.bytes,
                    Headers.build {
                        append(HttpHeaders.ContentType, image.contentType)
                        append(HttpHeaders.ContentDisposition, "filename=\"${image.filename}\"")
                    },
                )
            }
            client.post("api/v1/products/draft-from-image") {
                setBody(MultiPartFormDataContent(parts))
            }.body<DraftFromImageResponseDto>()
        }.toNetworkResult()

    /**
     * Optimizes an existing description via the LLM, preserving all facts. Fail-open: on LLM failure
     * the endpoint echoes the original description back.
     */
    suspend fun regenerateDescription(
        body: RegenerateDescriptionRequestDto,
    ): NetworkResult<RegenerateDescriptionResponseDto> =
        runCatching {
            client.post("api/v1/products/regenerate-description") {
                setBody(body)
            }.body<RegenerateDescriptionResponseDto>()
        }.toNetworkResult()
}
