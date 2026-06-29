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

    /** Creates a product and returns its id. Failures carry a `RegisterProductException`. */
    suspend fun createProduct(product: NewProduct): Result<String>

    /** Attaches an already-uploaded media [url] to a product. */
    suspend fun attachMedia(
        productId: String,
        url: String,
        position: Int,
        primary: Boolean,
    ): Result<Unit>

    /** Soft-deletes a product — used to roll back a partially-registered product. */
    suspend fun deactivateProduct(productId: String): Result<Unit>
}
