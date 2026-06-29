package com.qhathia.mobile.staff.core.testing

import com.qhathia.mobile.staff.core.domain.model.Availability
import com.qhathia.mobile.staff.core.domain.model.NewProduct
import com.qhathia.mobile.staff.core.domain.model.Product
import com.qhathia.mobile.staff.core.domain.repository.ProductRepository

/**
 * Controllable [ProductRepository] for tests. Set the `*Result` fields to the value the next call
 * should return; arguments, call counts and the captured create/attach/deactivate interactions are
 * exposed for assertions.
 */
class FakeProductRepository : ProductRepository {

    /** A media-attach interaction captured for assertions. */
    data class AttachedMedia(
        val productId: String,
        val url: String,
        val position: Int,
        val primary: Boolean,
    )

    var productsResult: Result<List<Product>> = Result.success(listOf(TestFixtures.product()))
    var createResult: Result<String> = Result.success("new-product-1")
    var attachResult: Result<Unit> = Result.success(Unit)
    var deactivateResult: Result<Unit> = Result.success(Unit)

    var lastCategoryId: String? = null
        private set
    var lastAvailability: Availability? = null
        private set
    var callCount: Int = 0
        private set

    var createdProduct: NewProduct? = null
        private set
    val attachedMedia: MutableList<AttachedMedia> = mutableListOf()
    var deactivatedProductId: String? = null
        private set

    override suspend fun getProducts(
        categoryId: String?,
        availability: Availability?,
    ): Result<List<Product>> {
        callCount++
        lastCategoryId = categoryId
        lastAvailability = availability
        return productsResult
    }

    override suspend fun createProduct(product: NewProduct): Result<String> {
        createdProduct = product
        return createResult
    }

    override suspend fun attachMedia(
        productId: String,
        url: String,
        position: Int,
        primary: Boolean,
    ): Result<Unit> {
        attachedMedia += AttachedMedia(productId, url, position, primary)
        return attachResult
    }

    override suspend fun deactivateProduct(productId: String): Result<Unit> {
        deactivatedProductId = productId
        return deactivateResult
    }
}
