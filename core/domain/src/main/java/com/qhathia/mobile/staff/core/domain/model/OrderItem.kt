package com.qhathia.mobile.staff.core.domain.model

/**
 * One line of an [OrderDetail]. [unitPrice] is the price snapshot taken when the kiosk created the
 * order, so it does not follow later price edits.
 *
 * [productStock] and [productAvailability] are the product's *live* values, carried by the detail
 * endpoint so the dialog can warn about low stock before the merchant confirms. A null
 * [productStock] means the product is sold in consult mode and has no tracked stock — no warning is
 * shown for it. [productAvailability] is null when the backend reports a value this app does not
 * model.
 *
 * [productImageUrl] is live too: the product's current primary photo, not the one it had when the
 * order was created. Null means the product has no images and the dialog shows its placeholder.
 */
data class OrderItem(
    val id: String,
    val productId: String,
    val productName: String,
    val quantity: Int,
    val unitPrice: Double,
    val lineTotal: Double,
    val productStock: Int?,
    val productAvailability: Availability?,
    val productImageUrl: String?,
)
