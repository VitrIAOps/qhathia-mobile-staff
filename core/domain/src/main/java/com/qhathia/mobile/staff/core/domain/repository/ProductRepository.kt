package com.qhathia.mobile.staff.core.domain.repository

import com.qhathia.mobile.staff.core.domain.model.Availability
import com.qhathia.mobile.staff.core.domain.model.Product

/** Port for reading the store's products. */
interface ProductRepository {

    /**
     * Lists the store's products, optionally filtered by [categoryId] and/or [availability].
     * Pass null for a filter to omit it.
     */
    suspend fun getProducts(
        categoryId: String?,
        availability: Availability?,
    ): Result<List<Product>>
}
