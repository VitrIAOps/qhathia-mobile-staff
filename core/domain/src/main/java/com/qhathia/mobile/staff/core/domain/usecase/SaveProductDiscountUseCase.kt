package com.qhathia.mobile.staff.core.domain.usecase

import com.qhathia.mobile.staff.core.domain.model.Discount
import com.qhathia.mobile.staff.core.domain.model.DiscountError
import com.qhathia.mobile.staff.core.domain.model.DiscountException
import com.qhathia.mobile.staff.core.domain.model.ProductDiscount
import com.qhathia.mobile.staff.core.domain.repository.ProductDiscountRepository
import java.time.Clock
import java.time.Duration
import java.time.temporal.ChronoUnit
import javax.inject.Inject

/**
 * Sets the product's discount, replacing whatever it had. The API has no update call, so this
 * creates the new discount, which runs for one week from now, and only then deletes the ones the
 * product had. A failure midway never leaves the product without a discount: if the create fails
 * nothing was deleted, and if a delete fails the product briefly has two (the kiosk applies the
 * cheaper one) and the failure is returned so the caller can retry. A discount already gone (404)
 * counts as deleted.
 */
class SaveProductDiscountUseCase @Inject constructor(
    private val repository: ProductDiscountRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(productId: String, discount: Discount): Result<ProductDiscount> {
        val existing = repository.getDiscounts(productId)
            .getOrElse { return Result.failure(it) }
        val startsAt = clock.instant().truncatedTo(ChronoUnit.SECONDS)
        val created = repository.createDiscount(productId, discount, startsAt, startsAt.plus(DISCOUNT_DURATION))
            .getOrElse { return Result.failure(it) }
        existing.forEach { stored ->
            repository.deleteDiscount(productId, stored.id)
                .onFailure { if (!it.isNotFound()) return Result.failure(it) }
        }
        return Result.success(created)
    }

    companion object {
        /** How long a discount created from the app stays in effect. */
        val DISCOUNT_DURATION: Duration = Duration.ofDays(7)
    }
}

/** A discount that is already gone answers 404 on delete; for a delete that is success. */
internal fun Throwable.isNotFound(): Boolean = (this as? DiscountException)?.error == DiscountError.NotFound
