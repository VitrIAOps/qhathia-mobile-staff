package com.qhathia.mobile.staff.core.domain.usecase

import com.qhathia.mobile.staff.core.domain.model.ProductDiscount
import com.qhathia.mobile.staff.core.domain.repository.ProductDiscountRepository
import java.time.Clock
import javax.inject.Inject

/**
 * The product's current discount: the active one that has not ended yet, or null when there is
 * none. If the backend holds several, the one ending last wins.
 */
class GetProductDiscountUseCase @Inject constructor(
    private val repository: ProductDiscountRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(productId: String): Result<ProductDiscount?> =
        repository.getDiscounts(productId).map { discounts ->
            val now = clock.instant()
            discounts
                .filter { it.active && it.endsAt.isAfter(now) }
                .maxByOrNull { it.endsAt }
        }
}
