package com.qhathia.mobile.staff.core.domain.usecase

import com.qhathia.mobile.staff.core.domain.repository.ProductDiscountRepository
import javax.inject.Inject

/** Leaves the product without a discount by deleting every one it has; one already gone (404) counts. */
class RemoveProductDiscountUseCase @Inject constructor(
    private val repository: ProductDiscountRepository,
) {
    suspend operator fun invoke(productId: String): Result<Unit> {
        val existing = repository.getDiscounts(productId)
            .getOrElse { return Result.failure(it) }
        existing.forEach { stored ->
            repository.deleteDiscount(productId, stored.id)
                .onFailure { if (!it.isNotFound()) return Result.failure(it) }
        }
        return Result.success(Unit)
    }
}
