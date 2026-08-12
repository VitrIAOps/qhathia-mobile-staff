package com.qhathia.mobile.staff.core.domain.model

/**
 * A single product with everything the detail screen needs. Kept apart from [Product] because the
 * list endpoint (`GET /api/v1/products`) exposes neither description, stock, tags nor the full media
 * set — only `GET /api/v1/products/{id}` does.
 *
 * [stock] is null for products the merchant registered as "consultar disponibilidad".
 * [imageUrls] is ordered as the DS shows it: the primary photo first, then by backend position.
 */
data class ProductDetail(
    val id: String,
    val name: String,
    val description: String?,
    val price: Double,
    val stock: Int?,
    val categoryId: String?,
    val categoryName: String?,
    val tags: List<String>,
    val imageUrls: List<String>,
    val allowDiscountInquiry: Boolean,
    val position: Int,
)
