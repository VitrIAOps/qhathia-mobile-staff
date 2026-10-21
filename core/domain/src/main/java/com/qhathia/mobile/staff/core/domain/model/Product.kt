package com.qhathia.mobile.staff.core.domain.model

import java.time.Instant

/**
 * A product in the store's inventory, as shown in the inventory list.
 *
 * @property stock units on hand; `null` when unknown (legacy and "consult" products), never guessed.
 * @property discountedPrice the price with the discount in effect, as computed by the server;
 *   `null` when no discount is in effect. The app never derives it for display.
 * @property discountEndsAt when that discount ends; `null` without one.
 */
data class Product(
    val id: String,
    val categoryId: String?,
    val categoryName: String?,
    val name: String,
    val price: Double,
    val availability: Availability,
    val primaryImageUrl: String?,
    val stock: Int? = null,
    val discountedPrice: Double? = null,
    val discountEndsAt: Instant? = null,
)
