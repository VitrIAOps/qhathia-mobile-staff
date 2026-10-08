package com.qhathia.mobile.staff.core.domain.repository

import com.qhathia.mobile.staff.core.domain.model.Discount
import com.qhathia.mobile.staff.core.domain.model.ProductDiscount
import java.time.Instant

/**
 * Port for a product's discounts. The API only lists, creates and deletes — there is no update — so
 * replacing a discount is composed from these three calls by the use cases. Failures carry a
 * `DiscountException`.
 */
interface ProductDiscountRepository {

    /** Every discount of the product, active or not. */
    suspend fun getDiscounts(productId: String): Result<List<ProductDiscount>>

    suspend fun createDiscount(
        productId: String,
        discount: Discount,
        startsAt: Instant,
        endsAt: Instant,
    ): Result<ProductDiscount>

    suspend fun deleteDiscount(productId: String, discountId: String): Result<Unit>
}
