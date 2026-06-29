package com.qhathia.mobile.staff.core.domain.model

/**
 * An image the merchant picked for a new product, referenced by its content [uri] (string form, so
 * the domain stays framework-free) and resolved [mimeType] (e.g. `image/jpeg`, `image/png`).
 */
data class SelectedImage(
    val uri: String,
    val mimeType: String,
)
