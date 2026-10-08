package com.qhathia.mobile.staff.core.domain.model

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant

/** How a discount's [Discount.value] is read: a share of the price, or soles off it. */
enum class DiscountType { PERCENTAGE, FIXED_AMOUNT }

/**
 * A discount on a product's price. [value] is a whole percentage (1–100) for
 * [DiscountType.PERCENTAGE] and an amount in soles for [DiscountType.FIXED_AMOUNT].
 */
data class Discount(
    val type: DiscountType,
    val value: Double,
) {
    /** The price after the discount, rounded to cents. Zero is the floor — never negative. */
    fun applyTo(price: Double): Double {
        val reduction = when (type) {
            DiscountType.PERCENTAGE -> price * value / PERCENT_BASE
            DiscountType.FIXED_AMOUNT -> value
        }
        return BigDecimal.valueOf((price - reduction).coerceAtLeast(0.0))
            .setScale(2, RoundingMode.HALF_UP)
            .toDouble()
    }

    /**
     * Whether this discount is still valid for [price]: a percentage up to 100, or an amount up to
     * the price itself. Used to drop a discount when the merchant edits the price under it.
     */
    fun fits(price: Double): Boolean = when (type) {
        DiscountType.PERCENTAGE -> value > 0.0 && value <= PERCENT_BASE
        DiscountType.FIXED_AMOUNT -> value > 0.0 && value <= price
    }

    /**
     * The value as the merchant types and reads it: `20` for a percentage, `15` or `15.50` for an
     * amount. Seeds the discount editor and fills the "Descuento de …" summary.
     */
    fun valueText(): String {
        val isWhole = value % 1.0 == 0.0
        return when {
            type == DiscountType.PERCENTAGE || isWhole -> value.toLong().toString()
            else -> BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).toPlainString()
        }
    }

    private companion object {
        const val PERCENT_BASE = 100.0
    }
}

/** A discount as stored by the backend for a product. */
data class ProductDiscount(
    val id: String,
    val discount: Discount,
    val startsAt: Instant,
    val endsAt: Instant,
    val active: Boolean,
)
