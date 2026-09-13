package com.qhathia.mobile.staff.core.data.mapper

import com.qhathia.mobile.staff.core.domain.model.MissingProductSuggestion
import com.qhathia.mobile.staff.core.domain.model.RestockSuggestion
import com.qhathia.mobile.staff.core.domain.model.SalesBucket
import com.qhathia.mobile.staff.core.domain.model.SalesPeriod
import com.qhathia.mobile.staff.core.domain.model.SalesSummary
import com.qhathia.mobile.staff.core.network.dto.RestockDto
import com.qhathia.mobile.staff.core.network.dto.SalesDashboardDto
import com.qhathia.mobile.staff.core.network.dto.UnmetSearchGroupDto
import com.qhathia.mobile.staff.core.network.dto.UnmetSearchesDto
import java.math.BigDecimal
import java.math.MathContext
import java.time.Instant
import java.util.Locale

private val SPANISH_PERU: Locale = Locale.forLanguageTag("es-PE")
private val HUNDRED = BigDecimal(100)

/**
 * Maps `GET /dashboard/sales`. The period is the one that was requested rather than the echoed
 * `granularity`, so a missing or unexpected echo cannot relabel the card.
 *
 * The headline is `current.revenue` — the period in progress — falling back to the last bucket,
 * which is the same period. The best bucket and its variation against the preceding bucket are
 * derived here: the backend sends neither.
 */
fun SalesDashboardDto.toSalesSummary(period: SalesPeriod, generatedAt: Instant): SalesSummary {
    val chart = buckets.map { SalesBucket(label = it.label, amount = it.revenue.toMoney()) }
    val highlighted = chart.highestIndex()
    return SalesSummary(
        period = period,
        total = (current?.revenue ?: buckets.lastOrNull()?.revenue ?: 0.0).toMoney(),
        deltaPercent = deltaPercent?.takeIf { it.isFinite() },
        buckets = chart,
        highlightedBucketIndex = highlighted,
        highlightedDeltaPercent = chart.deltaAgainstPreceding(highlighted),
        generatedAt = generatedAt,
    )
}

/**
 * Maps `GET /insights/unmet-searches`. Each group is named after the raw text shoppers typed most
 * often — "taladro percutor bosch" says what to stock, "taladro" does not — and keyed by its
 * canonical term. Blank terms are dropped and repeated terms kept once: the row is a `LazyRow`
 * keyed by id, and a duplicate key crashes it.
 */
fun UnmetSearchesDto.toMissingProductSuggestions(): List<MissingProductSuggestion> =
    groups.mapNotNull { it.toMissingProductSuggestion() }.distinctBy { it.id }

/** Maps `GET /dashboard/restock`, de-duplicated by product for the same `LazyRow` reason. */
fun RestockDto.toRestockSuggestions(): List<RestockSuggestion> =
    items
        .filter { it.productId.isNotBlank() }
        .distinctBy { it.productId }
        .map { item ->
            RestockSuggestion(
                id = item.productId,
                name = item.name,
                imageUrl = item.imageUrl?.takeIf { it.isNotBlank() },
                remainingStock = item.stock,
            )
        }

private fun UnmetSearchGroupDto.toMissingProductSuggestion(): MissingProductSuggestion? {
    val canonical = term.trim().takeIf { it.isNotEmpty() } ?: return null
    val mostFrequent = examples
        .filter { it.rawText.isNotBlank() }
        .maxByOrNull { it.count }
        ?.rawText
        ?.trim()
    return MissingProductSuggestion(
        id = canonical,
        name = (mostFrequent ?: canonical).replaceFirstChar { it.titlecase(SPANISH_PERU) },
        personCount = personCount,
    )
}

private fun Double.toMoney(): BigDecimal = BigDecimal.valueOf(this)

/** Index of the largest bucket (the earliest on a tie), or -1 when nothing in the window sold. */
private fun List<SalesBucket>.highestIndex(): Int {
    val index = indices.maxByOrNull { this[it].amount } ?: return -1
    return if (this[index].amount.signum() > 0) index else -1
}

/** Percent change of bucket [index] against the one before it, or null without a base. */
private fun List<SalesBucket>.deltaAgainstPreceding(index: Int): Double? {
    val bucket = getOrNull(index) ?: return null
    val preceding = getOrNull(index - 1)?.amount?.takeIf { it.signum() > 0 } ?: return null
    return bucket.amount.subtract(preceding)
        .multiply(HUNDRED)
        .divide(preceding, MathContext.DECIMAL64)
        .toDouble()
}
