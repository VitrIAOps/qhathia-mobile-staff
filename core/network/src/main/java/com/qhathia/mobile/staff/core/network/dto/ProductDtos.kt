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

/**
 * The `product` JSON part of `POST /api/v1/products/with-images`. `availability` is intentionally
 * omitted so the backend derives it from [stock] (`0 → OUT_OF_STOCK`, `>0 → AVAILABLE`) per the
 * contract's `stock ↔ availability` sync.
 */
@Serializable
data class CreateProductRequestDto(
    val name: String,
    val description: String? = null,
    val price: Double,
    val stock: Int,
    val allowDiscountInquiry: Boolean = false,
    val categoryId: String? = null,
    val tags: List<String> = emptyList(),
    val position: Int = 0,
)

/**
 * Response of `POST /api/v1/products/with-images`. Only `id` is consumed; extra fields are ignored
 * by Json.
 */
@Serializable
data class ProductResponseDto(
    val id: String,
)

/**
 * Response of `POST /api/v1/products/draft-from-image`. The endpoint is fail-open: on LLM failure it
 * still returns 200 with an empty [description], null [categoryId] and empty [tags] (with
 * `source = "stub"`). [categoryId] is a store category UUID or null; [tags] are a closed,
 * pre-validated subset. Property names match the JSON keys, so no `@SerialName` is needed.
 */
@Serializable
data class DraftFromImageResponseDto(
    val description: String = "",
    val categoryId: String? = null,
    val tags: List<String> = emptyList(),
    val source: String = "",
)

/** The `currentDescription` JSON body of `POST /api/v1/products/regenerate-description` (max 2000 chars). */
@Serializable
data class RegenerateDescriptionRequestDto(
    val currentDescription: String,
)

/**
 * Response of `POST /api/v1/products/regenerate-description`. Fail-open: on LLM failure the endpoint
 * echoes the original description back in [description].
 */
@Serializable
data class RegenerateDescriptionResponseDto(
    val description: String = "",
)
