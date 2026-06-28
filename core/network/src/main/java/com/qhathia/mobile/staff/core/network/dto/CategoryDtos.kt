package com.qhathia.mobile.staff.core.network.dto

import kotlinx.serialization.Serializable

/** Row of `GET /api/v1/categories`. */
@Serializable
data class CategoryDto(
    val id: String,
    val storeId: String? = null,
    val name: String,
    val slug: String? = null,
    val position: Int = 0,
    val active: Boolean = true,
)
