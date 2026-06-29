package com.qhathia.mobile.staff.core.domain.model

/**
 * A product the merchant is registering. [quantity] (stock) is captured and validated client-side
 * but is not yet sent to the backend — the products API has no quantity field yet (see
 * `NewProduct.toCreateRequestDto` in `:core:data`). New products default to `AVAILABLE`.
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
