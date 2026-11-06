package com.qhathia.mobile.staff.core.domain.model

import java.time.Instant

/**
 * A single product with everything the detail screen needs. Kept apart from [Product] because the
 * list endpoint (`GET /api/v1/products`) exposes neither description, stock, tags nor the full media
 * set — only `GET /api/v1/products/{id}` does.
 *
 * [stock] is null for products the merchant registered as "consultar disponibilidad".
 * [images] are ordered as the DS shows them: the primary photo first, then by backend position.
 * [discountedPrice] is what the product costs right now, as the backend prices it (cheapest
 * discount in force wins); `null` means no discount is in force. [discountEndsAt] is when that
 * discount ends, `null` without one. [version] is the optimistic-lock version to send back with an
 * update; `null` from an older backend. [active] is `false` for a deleted product, which staff can
 * still read but must not edit.
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
    val images: List<ProductImage>,
    val allowDiscountInquiry: Boolean,
    val position: Int,
    val discountedPrice: Double? = null,
    val discountEndsAt: Instant? = null,
    val version: Long? = null,
    val active: Boolean = true,
)
