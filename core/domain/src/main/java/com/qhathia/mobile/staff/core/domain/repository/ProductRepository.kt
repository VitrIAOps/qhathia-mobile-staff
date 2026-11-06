package com.qhathia.mobile.staff.core.domain.repository

import com.qhathia.mobile.staff.core.domain.model.Availability
import com.qhathia.mobile.staff.core.domain.model.NewProduct
import com.qhathia.mobile.staff.core.domain.model.Product
import com.qhathia.mobile.staff.core.domain.model.ProductDetail
import com.qhathia.mobile.staff.core.domain.model.ProductUpdate
import com.qhathia.mobile.staff.core.domain.model.SelectedImage

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
     * Updates the product [id] with [product] (a partial update) and returns the product as stored,
     * with its new version. Failures carry an `UpdateProductException`; a stale version is
     * `UpdateProductError.Conflict`.
     */
    suspend fun updateProduct(id: String, product: ProductUpdate): Result<ProductDetail>

    /**
     * Deletes the product [id]. It is a soft delete with no way back: the product leaves the
     * inventory and the kiosk catalog. Failures carry an `UpdateProductException` (404 → not found).
     */
    suspend fun deleteProduct(id: String): Result<Unit>

    /**
     * Compresses and uploads [images] to the existing product [id] in one request. They are added
     * after the current photos and never become the main one. Failures carry an
     * `UpdateProductException`.
     */
    suspend fun addProductImages(id: String, images: List<SelectedImage>): Result<Unit>

    /** Removes the photo [mediaId] from the product [id]. Failures carry an `UpdateProductException`. */
    suspend fun deleteProductImage(id: String, mediaId: String): Result<Unit>
}
