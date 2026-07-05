package com.qhathia.mobile.staff.core.testing

import com.qhathia.mobile.staff.core.domain.model.Availability
import com.qhathia.mobile.staff.core.domain.model.NewProduct
import com.qhathia.mobile.staff.core.domain.model.Product
import com.qhathia.mobile.staff.core.domain.repository.ProductRepository

/**
 * Controllable [ProductRepository] for tests. Set the `*Result` fields to the value the next call
 * should return; arguments, call counts and the captured registered product are exposed for
 * assertions.
 */
class FakeProductRepository : ProductRepository {

    var productsResult: Result<List<Product>> = Result.success(listOf(TestFixtures.product()))
    var registerResult: Result<String> = Result.success("new-product-1")

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

    override suspend fun getProducts(
        categoryId: String?,
        availability: Availability?,
    ): Result<List<Product>> {
        callCount++
        lastCategoryId = categoryId
        lastAvailability = availability
        return productsResult
    }

    override suspend fun registerProduct(product: NewProduct): Result<String> {
        registerCallCount++
        registeredProduct = product
        return registerResult
    }
}
