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
    /**
     * What the next [updateProduct] returns. `null` echoes a success, as the backend does: the
     * current [productDetailResult] product with the sent fields applied (an absent stock kept) and
     * its version bumped by one.
     * [updateResults] takes precedence, one entry per call, for multi-save tests.
     */
    var updateResult: Result<ProductDetail>? = null
    val updateResults: ArrayDeque<Result<ProductDetail>> = ArrayDeque()
    var deleteResult: Result<Unit> = Result.success(Unit)

    /** One result per [deleteProduct] call, ahead of [deleteResult] (e.g. a 409 then a forced 204). */
    val deleteResults: ArrayDeque<Result<Unit>> = ArrayDeque()

    /** The `force` flag of every delete, in call order. */
    val deleteForces: MutableList<Boolean> = mutableListOf()

    /** Set to a pending deferred to hold [updateProduct] in flight (to observe saving). */
    var updateGate: CompletableDeferred<Unit>? = null

    /** Set to a pending deferred to hold [registerProduct] in flight (to observe submitting). */
    var registerGate: CompletableDeferred<Unit>? = null
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

    /** Every product sent to [registerProduct], in call order. */
    val registeredProducts: MutableList<NewProduct> = mutableListOf()

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

    /** Every update sent, in call order. */
    val updatedProducts: MutableList<ProductUpdate> = mutableListOf()

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
        registeredProducts += product
        registerGate?.await()
        return registerResult
    }

    override suspend fun getProduct(id: String): Result<ProductDetail> {
        detailCallCount++
        lastRequestedId = id
        return productDetailResult
    }

    override suspend fun updateProduct(id: String, product: ProductUpdate): Result<ProductDetail> {
        updateCallCount++
        writeLog += "update"
        updatedId = id
        updatedProduct = product
        updatedProducts += product
        updateGate?.await()
        updateResults.removeFirstOrNull()?.let { return it }
        updateResult?.let { return it }
        val stored = productDetailResult.getOrNull() ?: TestFixtures.productDetail()
        return Result.success(
            stored.copy(
                name = product.name,
                price = product.price,
                stock = product.stock ?: stored.stock,
                description = product.description.ifEmpty { null },
                tags = product.tags,
                version = (product.version ?: stored.version ?: 0L) + 1,
            ),
        )
    }

    override suspend fun deleteProduct(id: String, force: Boolean): Result<Unit> {
        deletedIds += id
        deleteForces += force
        deleteGate?.await()
        deleteResults.removeFirstOrNull()?.let { return it }
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
