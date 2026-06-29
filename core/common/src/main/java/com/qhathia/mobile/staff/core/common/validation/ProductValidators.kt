package com.qhathia.mobile.staff.core.common.validation

/** Maximum product name length accepted by the products API. */
const val MAX_PRODUCT_NAME_LENGTH = 200

/** A product name is valid when it is non-blank and within [MAX_PRODUCT_NAME_LENGTH]. */
fun isValidProductName(value: String): Boolean {
    val trimmed = value.trim()
    return trimmed.isNotEmpty() && trimmed.length <= MAX_PRODUCT_NAME_LENGTH
}

private val PRICE_REGEX = Regex("^\\d+(\\.\\d{1,2})?$")

/**
 * Parses a PEN price. Valid when it is a number greater than zero with at most two decimals
 * (a comma decimal separator is accepted). Returns the parsed amount, or null when invalid.
 */
fun parsePrice(value: String): Double? {
    val normalized = value.trim().replace(',', '.')
    if (!PRICE_REGEX.matches(normalized)) return null
    val amount = normalized.toDoubleOrNull() ?: return null
    return amount.takeIf { it > 0.0 }
}

fun isValidPrice(value: String): Boolean = parsePrice(value) != null

/**
 * Parses a stock quantity. Valid when it is a non-negative integer; a negative quantity blocks
 * registration. Returns the parsed quantity, or null when invalid.
 */
fun parseQuantity(value: String): Int? {
    val quantity = value.trim().toIntOrNull() ?: return null
    return quantity.takeIf { it >= 0 }
}

fun isValidQuantity(value: String): Boolean = parseQuantity(value) != null
