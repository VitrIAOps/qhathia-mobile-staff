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
