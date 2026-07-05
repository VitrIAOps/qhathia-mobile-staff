package com.qhathia.mobile.staff.core.domain.model

/**
 * A product the merchant is registering. [quantity] (stock) is sent to the backend as the product's
 * `stock`; `availability` is not sent explicitly, so the backend derives it from the stock (see
 * `NewProduct.toCreateRequestDto` in `:core:data`).
 */
data class NewProduct(
    val name: String,
    val price: Double,
    val quantity: Int,
    val description: String?,
    val categoryId: String,
    val tags: List<String>,
    val images: List<SelectedImage>,
)
