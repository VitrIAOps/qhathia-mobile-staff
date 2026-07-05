package com.qhathia.mobile.staff.core.domain.repository

import com.qhathia.mobile.staff.core.domain.model.Availability
import com.qhathia.mobile.staff.core.domain.model.NewProduct
import com.qhathia.mobile.staff.core.domain.model.Product

/** Port for reading and writing the store's products. */
interface ProductRepository {

    /**
     * Lists the store's products, optionally filtered by [categoryId] and/or [availability].
     * Pass null for a filter to omit it.
     */
    suspend fun getProducts(
        categoryId: String?,
        availability: Availability?,
    ): Result<List<Product>>

    /**
     * Registers a product with its images in a single atomic request and returns the created
     * product's id. The backend uploads the images and creates product + media transactionally, so
     * no client-side attach/rollback is needed. Failures carry a `RegisterProductException`.
     */
    suspend fun registerProduct(product: NewProduct): Result<String>
}
