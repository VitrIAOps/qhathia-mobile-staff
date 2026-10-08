package com.qhathia.mobile.staff.core.domain.usecase

import com.qhathia.mobile.staff.core.domain.model.Discount
import com.qhathia.mobile.staff.core.domain.model.ProductDiscount
import com.qhathia.mobile.staff.core.domain.repository.ProductDiscountRepository
import java.time.Clock
import java.time.Duration
import java.time.temporal.ChronoUnit
import javax.inject.Inject

/**
 * Sets the product's discount, replacing whatever it had. The API has no update call, so this
 * deletes every existing discount first and then creates the new one, which runs for one week from
 * now. The steps are separate requests: if the create fails after the deletes, the product is left
 * without a discount and the failure is returned.
 */
class SaveProductDiscountUseCase @Inject constructor(
    private val repository: ProductDiscountRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(productId: String, discount: Discount): Result<ProductDiscount> {
        val existing = repository.getDiscounts(productId)
            .getOrElse { return Result.failure(it) }
        existing.forEach { stored ->
            repository.deleteDiscount(productId, stored.id)
                .onFailure { return Result.failure(it) }
        }
        val startsAt = clock.instant().truncatedTo(ChronoUnit.SECONDS)
        return repository.createDiscount(productId, discount, startsAt, startsAt.plus(DISCOUNT_DURATION))
    }

    companion object {
        /** How long a discount created from the app stays in effect. */
        val DISCOUNT_DURATION: Duration = Duration.ofDays(7)
    }
}
