package com.qhathia.mobile.staff.core.domain.model

import java.math.BigDecimal
import java.time.Instant

/** A product that carried the period's revenue. */
data class TopSellingProduct(
    val id: String,
    val name: String,
    val imageUrl: String?,
    val revenue: BigDecimal,
    val unitsSold: Int,
    /** False when the product was deleted after selling; it stays in the ranking, marked. */
    val active: Boolean = true,
)

/**
 * An active product with stock that sold nothing in the period. [immobilizedAmount] is
 * `stock × price` — the money standing still on the shelf.
 */
data class StalledProduct(
    val id: String,
    val name: String,
    val imageUrl: String?,
    val stock: Int,
    val price: BigDecimal,
    val immobilizedAmount: BigDecimal,
)

/**
 * What sold and what did not over a period. [needsAttention] is ordered by immobilized money,
 * highest first, so its first product is the one most worth acting on.
 *
 * [immobilizedTotal] adds up the listed [needsAttention] products only. [needsAttentionTotalCount]
 * is how many products sold nothing in total, which can be more than are listed; null when the
 * backend did not report it.
 *
 * [periodRevenue] is the store's whole revenue for the period and [productsSold] how many distinct
 * products sold, so the top sellers can be read as "S/ X de S/ Y"; [asOf] is when the figures were
 * computed. All three are null on a backend that does not send them.
 */
data class SalesRanking(
    val periodStart: Instant?,
    val periodEnd: Instant?,
    val topSellersRevenue: BigDecimal,
    val topSellers: List<TopSellingProduct>,
    val immobilizedTotal: BigDecimal,
    val needsAttention: List<StalledProduct>,
    val needsAttentionTotalCount: Int? = null,
    val periodRevenue: BigDecimal? = null,
    val productsSold: Int? = null,
    val asOf: Instant? = null,
)
