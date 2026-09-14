package com.qhathia.mobile.staff.core.ui.component.sales

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs

/** Peruvian soles as the design writes them: `S/ 5,770`, grouped, without decimals. */
fun BigDecimal.toSolesLabel(): String {
    val format = NumberFormat.getIntegerInstance(Locale.US)
    return "S/ ${format.format(setScale(0, RoundingMode.HALF_UP))}"
}

/** Which way a percentage moved once rounded for display. */
enum class PercentTrend { UP, DOWN, FLAT }

/**
 * Decided on the rounded magnitude, so a change too small to print reads as flat — never as `-0%`
 * in the error colour next to a figure that did not move.
 */
val Double.percentTrend: PercentTrend
    get() = when {
        percentMagnitude().signum() == 0 -> PercentTrend.FLAT
        this > 0 -> PercentTrend.UP
        else -> PercentTrend.DOWN
    }

/** `+24%` / `-6%` / `0%` — the sign is part of the number, not of the sentence around it. */
fun Double.toSignedPercentLabel(): String = when (percentTrend) {
    PercentTrend.UP -> "+${toPercentMagnitudeLabel()}"
    PercentTrend.DOWN -> "-${toPercentMagnitudeLabel()}"
    PercentTrend.FLAT -> "0%"
}

/** `24%` for both `24.3` and `-24.3` — for sentences that say "más" or "menos" themselves. */
fun Double.toPercentMagnitudeLabel(): String = "${percentMagnitude().toPlainString()}%"

/**
 * Whole percentages, as the design writes them, except below 1% where one decimal keeps a real
 * `0.4%` change from collapsing into `0%`.
 */
private fun Double.percentMagnitude(): BigDecimal {
    if (!isFinite()) return BigDecimal.ZERO
    val magnitude = BigDecimal.valueOf(abs(this))
    val scale = if (magnitude >= BigDecimal.ONE) 0 else 1
    return magnitude.setScale(scale, RoundingMode.HALF_UP).stripTrailingZeros()
}
