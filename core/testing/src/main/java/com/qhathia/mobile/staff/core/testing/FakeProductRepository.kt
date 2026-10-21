package com.qhathia.mobile.staff.core.testing

import com.qhathia.mobile.staff.core.domain.model.Availability
import com.qhathia.mobile.staff.core.domain.model.NewProduct
import com.qhathia.mobile.staff.core.domain.model.Product
import com.qhathia.mobile.staff.core.domain.model.ProductDetail
import com.qhathia.mobile.staff.core.domain.model.ProductUpdate
import com.qhathia.mobile.staff.core.domain.model.SelectedImage
import com.qhathia.mobile.staff.core.domain.repository.ProductRepository
import kotlinx.coroutines.CompletableDeferred

/**
 * Controllable [ProductRepository] for tests. Set the `*Result` fields to the value the next call
 * should return; arguments, call counts and the captured registered product are exposed for
 * assertions.
 */
class FakeProductRepository : ProductRepository {

    var productsResult: Result<List<Product>> = Result.success(listOf(TestFixtures.product()))
    var registerResult: Result<String> = Result.success("new-product-1")
    var productDetailResult: Result<ProductDetail> = Result.success(TestFixtures.productDetail())
    var updateResult: Result<Unit> = Result.success(Unit)
    var deleteResult: Result<Unit> = Result.success(Unit)
    var addImagesResult: Result<Unit> = Result.success(Unit)
    var deleteImageResult: Result<Unit> = Result.success(Unit)

    /** Set to a pending deferred to hold [deleteProduct] in flight (to observe the deleting state). */
    var deleteGate: CompletableDeferred<Unit>? = null

    /** Set to a pending deferred to hold [getProducts] in flight (to observe loading state). */
    var productsGate: CompletableDeferred<Unit>? = null

    var lastCategoryId: String? = null
        private set
    var lastAvailability: Availability? = null
        private set
    var callCount: Int = 0
        private set

    var registeredProduct: NewProduct? = null
        private set
    var registerCallCount: Int = 0
        private set

    var lastRequestedId: String? = null
        private set
    var detailCallCount: Int = 0
        private set

    var updatedId: String? = null
        private set
    var updatedProduct: ProductUpdate? = null
        private set
    var updateCallCount: Int = 0
        private set

    /** Every batch passed to [addProductImages], in call order. */
    val addedImageBatches: MutableList<List<SelectedImage>> = mutableListOf()

    /** Every media id passed to [deleteProductImage], in call order. */
    val deletedImageIds: MutableList<String> = mutableListOf()

    /** Every repository write, in call order, as "update", "deleteImage:<id>" or "addImages:<n>". */
    val writeLog: MutableList<String> = mutableListOf()

    /** Every id passed to [deleteProduct], in call order. */
    val deletedIds: MutableList<String> = mutableListOf()

    override suspend fun getProducts(
        categoryId: String?,
        availability: Availability?,
    ): Result<List<Product>> {
        callCount++
        lastCategoryId = categoryId
        lastAvailability = availability
        productsGate?.await()
        return productsResult
    }

    override suspend fun registerProduct(product: NewProduct): Result<String> {
        registerCallCount++
        registeredProduct = product
        return registerResult
    }

    override suspend fun getProduct(id: String): Result<ProductDetail> {
        detailCallCount++
        lastRequestedId = id
        return productDetailResult
    }

    override suspend fun updateProduct(id: String, product: ProductUpdate): Result<Unit> {
        updateCallCount++
        writeLog += "update"
        updatedId = id
        updatedProduct = product
        return updateResult
    }

    override suspend fun deleteProduct(id: String): Result<Unit> {
        deletedIds += id
        deleteGate?.await()
        return deleteResult
    }

    override suspend fun addProductImages(id: String, images: List<SelectedImage>): Result<Unit> {
        addedImageBatches += images
        writeLog += "addImages:${images.size}"
        return addImagesResult
    }

    override suspend fun deleteProductImage(id: String, mediaId: String): Result<Unit> {
        deletedImageIds += mediaId
        writeLog += "deleteImage:$mediaId"
        return deleteImageResult
    }
}
