package com.qhathia.mobile.staff.core.domain.model

/**
 * The edit sent to `PUT /api/v1/products/{id}`. The update is partial (Fase 40): what is not sent is
 * kept by the backend, so fields the form does not expose (brand, discount-inquiry flag, position)
 * never travel and cannot overwrite another vendor's change.
 *
 * - [stock] `null` keeps the stored stock. The form sends it only when the merchant changed it, so
 *   a sale confirmed while the form was open is not undone.
 * - [description] is always sent; `""` clears it.
 * - [version] is the version read with the product; a stale one fails with
 *   [UpdateProductError.Conflict]. `null` skips the check.
 *
 * `availability` is deliberately absent: the backend derives it from the stock.
 */
data class ProductUpdate(
    val name: String,
    val price: Double,
    val stock: Int?,
    val description: String,
    val categoryId: String,
    val tags: List<String>,
    val version: Long?,
)
