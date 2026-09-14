package com.qhathia.mobile.staff.core.data.mapper

import com.qhathia.mobile.staff.core.domain.model.MissingProductSuggestion
import com.qhathia.mobile.staff.core.domain.model.RestockSuggestion
import com.qhathia.mobile.staff.core.domain.model.SalesBucket
import com.qhathia.mobile.staff.core.domain.model.SalesKpis
import com.qhathia.mobile.staff.core.domain.model.SalesPeriod
import com.qhathia.mobile.staff.core.domain.model.SalesRanking
import com.qhathia.mobile.staff.core.domain.model.SalesSummary
import com.qhathia.mobile.staff.core.domain.model.StalledProduct
import com.qhathia.mobile.staff.core.domain.model.StoreActivity
import com.qhathia.mobile.staff.core.domain.model.TopSellingProduct
import com.qhathia.mobile.staff.core.network.dto.RestockDto
import com.qhathia.mobile.staff.core.network.dto.SalesDashboardDto
import com.qhathia.mobile.staff.core.network.dto.SalesRankingDto
import com.qhathia.mobile.staff.core.network.dto.StoreActivityDto
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
 * The headline, bounds and KPIs come from `current` — the period in progress — falling back to the
 * last bucket, which is the same period. The best bucket and its variation against the preceding
 * bucket are derived here: the backend sends neither.
 */
fun SalesDashboardDto.toSalesSummary(period: SalesPeriod, generatedAt: Instant): SalesSummary {
    val chart = buckets.map { SalesBucket(label = it.label, amount = it.revenue.toMoney()) }
    val highlighted = chart.highestIndex()
    val inProgress = current ?: buckets.lastOrNull()
    return SalesSummary(
        period = period,
        total = (inProgress?.revenue ?: 0.0).toMoney(),
        deltaPercent = deltaPercent.finiteOrNull(),
        buckets = chart,
        highlightedBucketIndex = highlighted,
        highlightedDeltaPercent = chart.deltaAgainstPreceding(highlighted),
        generatedAt = generatedAt,
        periodStart = inProgress?.periodStart.toInstantOrNull(),
        periodEnd = inProgress?.periodEnd.toInstantOrNull(),
        kpis = SalesKpis(
            salesCount = inProgress?.salesCount ?: 0,
            averageTicket = (inProgress?.averageTicket ?: 0.0).toMoney(),
            unitsSold = inProgress?.unitsSold ?: 0,
        ),
    )
}

/**
 * Maps `GET /dashboard/ranking`. Both lists feed keyed rows, so repeated or blank product ids are
 * dropped. Stalled stock is ordered by immobilized money, highest first, whatever order the backend
 * used — the first row is the one the screen flags hardest.
 */
fun SalesRankingDto.toSalesRanking(): SalesRanking = SalesRanking(
    periodStart = periodStart.toInstantOrNull(),
    periodEnd = periodEnd.toInstantOrNull(),
    topSellersRevenue = topSellers.totalRevenue.toMoney(),
    topSellers = topSellers.items
        .filter { it.productId.isNotBlank() }
        .distinctBy { it.productId }
        .map { item ->
            TopSellingProduct(
                id = item.productId,
                name = item.name,
                imageUrl = item.imageUrl.blankToNull(),
                revenue = item.revenue.toMoney(),
                unitsSold = item.unitsSold,
            )
        },
    immobilizedTotal = needsAttention.totalImmobilized.toMoney(),
    needsAttention = needsAttention.items
        .filter { it.productId.isNotBlank() }
        .distinctBy { it.productId }
        .map { item ->
            StalledProduct(
                id = item.productId,
                name = item.name,
                imageUrl = item.imageUrl.blankToNull(),
                stock = item.stock,
                price = item.price.toMoney(),
                immobilizedAmount = item.immobilizedAmount.toMoney(),
            )
        }
        .sortedByDescending { it.immobilizedAmount },
)

/** Maps `GET /dashboard/activity`, keeping a missing comparison base as null. */
fun StoreActivityDto.toStoreActivity(): StoreActivity = StoreActivity(
    periodStart = periodStart.toInstantOrNull(),
    periodEnd = periodEnd.toInstantOrNull(),
    uniqueSessions = uniqueSessions,
    uniqueSessionsDeltaPercent = uniqueSessionsDeltaPercent.finiteOrNull(),
    productViews = productViews,
    productViewsDeltaPercent = productViewsDeltaPercent.finiteOrNull(),
)

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
                imageUrl = item.imageUrl.blankToNull(),
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

private fun Double?.finiteOrNull(): Double? = this?.takeIf { it.isFinite() }

private fun String?.blankToNull(): String? = this?.takeIf { it.isNotBlank() }

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
