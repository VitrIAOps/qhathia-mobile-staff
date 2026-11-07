package com.qhathia.mobile.staff.core.ui.format

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols

/**
 * How the app writes Peruvian soles, in every screen and both languages: a point for decimals and a
 * comma for thousands ("S/ 12,345,678.50"), as Peru writes them. The symbols are fixed rather than
 * taken from a locale, so the JVM and Android, Spanish and English, all print the same.
 */
private val PEN_SYMBOLS = DecimalFormatSymbols().apply {
    decimalSeparator = '.'
    groupingSeparator = ','
    minusSign = '-'
}

private const val PREFIX = "S/ "

// DecimalFormat is not thread-safe; a fresh one per call is cheap enough for UI text.
private fun priceFormat() = DecimalFormat("#,##0.00", PEN_SYMBOLS).apply { roundingMode = RoundingMode.HALF_UP }
private fun amountFormat() = DecimalFormat("#,##0", PEN_SYMBOLS).apply { roundingMode = RoundingMode.HALF_UP }

/** A price, always with two decimals: "S/ 189.90", "S/ 12,345,678.50". */
fun Double.toPenPrice(): String = BigDecimal.valueOf(this).toPenPrice()

/** A price, always with two decimals: "S/ 189.90", "S/ 12,345,678.50". */
fun BigDecimal.toPenPrice(): String = PREFIX + priceFormat().format(this)

/** A dashboard figure, in whole soles: "S/ 2,849". */
fun BigDecimal.toPenAmount(): String = PREFIX + amountFormat().format(this)
