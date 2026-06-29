package com.qhathia.mobile.staff.core.network.dto

import kotlinx.serialization.Serializable

/**
 * Row of `GET /api/v1/products`. Optional fields are nullable/defaulted because a product may have
 * no category or image, and the products API exposes no stock quantity yet (the row's stock
 * indicator is mocked in the data layer until backend adds the field).
 */
@Serializable
data class ProductDto(
    val id: String,
    val categoryId: String? = null,
    val categoryName: String? = null,
    val name: String,
    val price: Double = 0.0,
    val availability: String? = null,
    val allowDiscountInquiry: Boolean = false,
    val position: Int = 0,
    val primaryImageUrl: String? = null,
)

/** Body of `POST /api/v1/products`. `availability` defaults to `AVAILABLE` per the API contract. */
@Serializable
data class CreateProductRequestDto(
    val name: String,
    val description: String? = null,
    val price: Double,
    val availability: String = "AVAILABLE",
    val allowDiscountInquiry: Boolean = false,
    val categoryId: String? = null,
    val tags: List<String> = emptyList(),
    val position: Int = 0,
)

/** Response of `POST /api/v1/products`. Only `id` is consumed; extra fields are ignored by Json. */
@Serializable
data class ProductResponseDto(
    val id: String,
)

/** Body of `POST /api/v1/products/{id}/media`. */
@Serializable
data class CreateMediaRequestDto(
    val url: String,
    val mediaType: String = "IMAGE",
    val position: Int = 0,
    val primary: Boolean = false,
)

/** Response of `POST /api/v1/products/{id}/media`. */
@Serializable
data class MediaResponseDto(
    val id: String,
    val url: String? = null,
    val mediaType: String? = null,
    val position: Int = 0,
    val primary: Boolean = false,
)
