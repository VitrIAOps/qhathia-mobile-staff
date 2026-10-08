package com.qhathia.mobile.staff.core.testing

import com.qhathia.mobile.staff.core.domain.model.Discount
import com.qhathia.mobile.staff.core.domain.model.ProductDiscount
import com.qhathia.mobile.staff.core.domain.repository.ProductDiscountRepository
import java.time.Instant

/**
 * In-memory [ProductDiscountRepository] for tests. [discounts] is the stored state: creates append
 * to it and deletes remove from it, so a replace can be asserted on the end result. Set a `*Failure`
 * to make that operation fail; [calls] records every request in order.
 */
class FakeProductDiscountRepository : ProductDiscountRepository {

    val discounts = mutableListOf<ProductDiscount>()

    var getFailure: Throwable? = null
    var createFailure: Throwable? = null
    var deleteFailure: Throwable? = null

    /** Each request as `get:<productId>`, `delete:<discountId>` or `create:<productId>`. */
    val calls = mutableListOf<String>()

    var lastCreated: ProductDiscount? = null
        private set

    override suspend fun getDiscounts(productId: String): Result<List<ProductDiscount>> {
        calls += "get:$productId"
        getFailure?.let { return Result.failure(it) }
        return Result.success(discounts.toList())
    }

    override suspend fun createDiscount(
        productId: String,
        discount: Discount,
        startsAt: Instant,
        endsAt: Instant,
    ): Result<ProductDiscount> {
        calls += "create:$productId"
        createFailure?.let { return Result.failure(it) }
        val created = ProductDiscount(
            id = "discount-${discounts.size + 1}-new",
            discount = discount,
            startsAt = startsAt,
            endsAt = endsAt,
            active = true,
        )
        discounts += created
        lastCreated = created
        return Result.success(created)
    }

    override suspend fun deleteDiscount(productId: String, discountId: String): Result<Unit> {
        calls += "delete:$discountId"
        deleteFailure?.let { return Result.failure(it) }
        discounts.removeAll { it.id == discountId }
        return Result.success(Unit)
    }
}
