package com.qhathia.mobile.staff.core.domain.model

/** A product in the store's inventory, as shown in the inventory list. */
data class Product(
    val id: String,
    val categoryId: String?,
    val categoryName: String?,
    val name: String,
    val price: Double,
    val availability: Availability,
    val primaryImageUrl: String?,
)
