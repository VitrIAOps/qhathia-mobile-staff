package com.qhathia.mobile.staff.core.domain.model

/**
 * The editable fields sent to `PUT /api/v1/products/{id}`, which is a full replace — every field
 * travels on every save, including the ones the form does not expose ([allowDiscountInquiry],
 * [position]), which the caller carries over from the loaded [ProductDetail] so the replace does not
 * silently reset them.
 *
 * `availability` is deliberately absent: the backend derives it from [stock] the same way it does on
 * create (`0 → OUT_OF_STOCK`, `> 0 → AVAILABLE`).
 */
data class ProductUpdate(
    val name: String,
    val price: Double,
    val stock: Int,
    val description: String?,
    val categoryId: String,
    val tags: List<String>,
    val allowDiscountInquiry: Boolean,
    val position: Int,
)
