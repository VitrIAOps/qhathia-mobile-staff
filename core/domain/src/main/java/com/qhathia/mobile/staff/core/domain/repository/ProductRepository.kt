package com.qhathia.mobile.staff.core.domain.repository

import com.qhathia.mobile.staff.core.domain.model.Availability
import com.qhathia.mobile.staff.core.domain.model.NewProduct
import com.qhathia.mobile.staff.core.domain.model.Product
import com.qhathia.mobile.staff.core.domain.model.ProductDetail
import com.qhathia.mobile.staff.core.domain.model.ProductUpdate

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

    /**
     * Loads one product with the fields the list endpoint omits (description, stock, tags, media).
     * Failures carry an `UpdateProductException`.
     */
    suspend fun getProduct(id: String): Result<ProductDetail>

    /**
     * Replaces the editable fields of the product [id]. The endpoint is a full replace, so [product]
     * must carry every field, not just the changed ones. Failures carry an `UpdateProductException`.
     */
    suspend fun updateProduct(id: String, product: ProductUpdate): Result<Unit>

    /**
     * Deletes the product [id]. It is a soft delete with no way back: the product leaves the
     * inventory and the kiosk catalog. Failures carry an `UpdateProductException` (404 → not found).
     */
    suspend fun deleteProduct(id: String): Result<Unit>
}
