package com.qhathia.mobile.staff.core.testing

import com.qhathia.mobile.staff.core.domain.model.Availability
import com.qhathia.mobile.staff.core.domain.model.NewProduct
import com.qhathia.mobile.staff.core.domain.model.Product
import com.qhathia.mobile.staff.core.domain.model.ProductDetail
import com.qhathia.mobile.staff.core.domain.model.ProductUpdate
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
        updatedId = id
        updatedProduct = product
        return updateResult
    }
}
