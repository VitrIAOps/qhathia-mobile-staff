package com.qhathia.mobile.staff.core.domain.model

/** One saved photo of a product: its media [id] (needed to delete it) and its [url]. */
data class ProductImage(
    val id: String,
    val url: String,
)
