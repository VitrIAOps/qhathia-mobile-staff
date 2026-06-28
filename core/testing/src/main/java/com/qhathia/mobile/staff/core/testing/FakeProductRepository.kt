package com.qhathia.mobile.staff.core.testing

import com.qhathia.mobile.staff.core.domain.model.Availability
import com.qhathia.mobile.staff.core.domain.model.Product
import com.qhathia.mobile.staff.core.domain.repository.ProductRepository

/**
 * Controllable [ProductRepository] for tests. Set [productsResult] to the value the next call
 * should return; the most recent filter arguments and call count are captured for assertions.
 */
class FakeProductRepository : ProductRepository {

    var productsResult: Result<List<Product>> = Result.success(listOf(TestFixtures.product()))

    var lastCategoryId: String? = null
        private set
    var lastAvailability: Availability? = null
        private set
    var callCount: Int = 0
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
}
