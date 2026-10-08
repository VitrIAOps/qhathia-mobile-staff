package com.qhathia.mobile.staff.core.domain.model

/** Why the value typed in the discount editor cannot be applied. */
enum class DiscountInputError {
    /** Not a whole number, or above 100. */
    InvalidPercentage,

    /** Not a number with at most two decimals. */
    InvalidAmount,

    /** More than the product's price, which would push the resulting price below zero. */
    AmountAbovePrice,
}

/**
 * The "Aplicar descuento" editor: the product [price], the selected [type] and the raw [input] the
 * merchant typed. Everything else is derived, so the three screens that host the dialog share one
 * set of rules:
 * - a percentage is a whole number from 1 to 100;
 * - an amount is greater than zero, has at most two decimals and does not exceed [price].
 *
 * A discount may bring the price down to exactly zero; that is the limit.
 */
data class DiscountDraft(
    val price: Double,
    val type: DiscountType = DiscountType.PERCENTAGE,
    val input: String = "",
) {
    private val parsed: Parsed
        get() = when (type) {
            DiscountType.PERCENTAGE -> parsePercentage()
            DiscountType.FIXED_AMOUNT -> parseAmount()
        }

    /** Null while the input is empty, zero or valid — an untouched field is not an error. */
    val error: DiscountInputError? get() = parsed.error

    val isValid: Boolean get() = parsed.value != null

    /** The price the merchant would end up with; the original price until the input is valid. */
    val resultingPrice: Double
        get() = toDiscount()?.applyTo(price) ?: price

    fun toDiscount(): Discount? = parsed.value?.let { Discount(type, it) }

    /** Switching mode starts over: a percentage typed so far means nothing as an amount. */
    fun withType(type: DiscountType): DiscountDraft =
        if (type == this.type) this else copy(type = type, input = "")

    fun withInput(input: String): DiscountDraft = copy(input = input)

    fun withQuickPercentage(percentage: Int): DiscountDraft =
        copy(type = DiscountType.PERCENTAGE, input = percentage.toString())

    private fun parsePercentage(): Parsed {
        val text = input.trim()
        if (text.isEmpty()) return Parsed()
        val percentage = text.takeIf { PERCENTAGE_REGEX.matches(it) }?.toIntOrNull()
            ?: return Parsed(error = DiscountInputError.InvalidPercentage)
        return when {
            percentage == 0 -> Parsed()
            percentage > MAX_PERCENTAGE -> Parsed(error = DiscountInputError.InvalidPercentage)
            else -> Parsed(value = percentage.toDouble())
        }
    }

    private fun parseAmount(): Parsed {
        val text = input.trim().replace(',', '.')
        if (text.isEmpty()) return Parsed()
        val amount = text.takeIf { AMOUNT_REGEX.matches(it) }?.toDoubleOrNull()
            ?: return Parsed(error = DiscountInputError.InvalidAmount)
        return when {
            amount == 0.0 -> Parsed()
            amount > price -> Parsed(error = DiscountInputError.AmountAbovePrice)
            else -> Parsed(value = amount)
        }
    }

    private data class Parsed(val value: Double? = null, val error: DiscountInputError? = null)

    companion object {
        /** The quick options offered in percentage mode. */
        val QUICK_PERCENTAGES: List<Int> = listOf(10, 20, 50)

        private const val MAX_PERCENTAGE = 100
        private val PERCENTAGE_REGEX = Regex("^\\d{1,9}$")
        private val AMOUNT_REGEX = Regex("^\\d{1,9}(\\.\\d{1,2})?$")

        /** An editor for [price], prefilled from [discount] when the product already has one. */
        fun from(price: Double, discount: Discount?): DiscountDraft =
            if (discount == null) {
                DiscountDraft(price = price)
            } else {
                DiscountDraft(price = price, type = discount.type, input = discount.valueText())
            }
    }
}
