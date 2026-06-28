package com.qhathia.mobile.staff.core.domain.usecase

import com.qhathia.mobile.staff.core.domain.model.Availability
import com.qhathia.mobile.staff.core.domain.model.Product
import com.qhathia.mobile.staff.core.domain.repository.ProductRepository
import javax.inject.Inject

/** Lists the store's products, optionally filtered by category and/or availability. */
class GetProductsUseCase @Inject constructor(
    private val repository: ProductRepository,
) {
    suspend operator fun invoke(
        categoryId: String? = null,
        availability: Availability? = null,
    ): Result<List<Product>> = repository.getProducts(categoryId, availability)
}
