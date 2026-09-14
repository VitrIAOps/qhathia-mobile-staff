package com.qhathia.mobile.staff.core.network.dto

import kotlinx.serialization.Serializable

/**
 * `GET /api/v1/dashboard/sales`. [buckets] runs oldest to newest and its last entry is the period
 * in progress — the same one as [current]. [deltaPercent] compares [current] with [previous] and is
 * null when [previous] sold nothing: there is no base to compare against.
 */
@Serializable
data class SalesDashboardDto(
    val granularity: String? = null,
    val timezone: String? = null,
    val current: SalesPeriodDto? = null,
    val previous: SalesPeriodDto? = null,
    val deltaPercent: Double? = null,
    val buckets: List<SalesPeriodDto> = emptyList(),
)

/**
 * One period of [SalesDashboardDto], cut in `America/Lima` time. Amounts use each order line's
 * price snapshot, so history does not move when prices are adjusted.
 */
@Serializable
data class SalesPeriodDto(
    val label: String = "",
    val periodStart: String? = null,
    val periodEnd: String? = null,
    val revenue: Double = 0.0,
    val salesCount: Int = 0,
    val averageTicket: Double = 0.0,
    val unitsSold: Int = 0,
)

/** `GET /api/v1/dashboard/restock` — the current inventory below [threshold]. */
@Serializable
data class RestockDto(
    val threshold: Int = 0,
    val items: List<RestockItemDto> = emptyList(),
)

/** One product of [RestockDto]. */
@Serializable
data class RestockItemDto(
    val productId: String,
    val name: String = "",
    val imageUrl: String? = null,
    val stock: Int = 0,
    val price: Double = 0.0,
    val availability: String? = null,
)

/**
 * `GET /api/v1/dashboard/ranking` — "lo que más vende" and "lo que necesita atención" over the
 * requested period.
 */
@Serializable
data class SalesRankingDto(
    val periodStart: String? = null,
    val periodEnd: String? = null,
    val topSellers: TopSellersDto = TopSellersDto(),
    val needsAttention: NeedsAttentionDto = NeedsAttentionDto(),
)

@Serializable
data class TopSellersDto(
    val totalRevenue: Double = 0.0,
    val items: List<TopSellerItemDto> = emptyList(),
)

@Serializable
data class TopSellerItemDto(
    val productId: String,
    val name: String = "",
    val imageUrl: String? = null,
    val revenue: Double = 0.0,
    val unitsSold: Int = 0,
)

/**
 * Active products with stock that sold nothing in the period. `immobilizedAmount` is
 * `stock × price`; consult-mode products (no stock) are excluded by the backend.
 */
@Serializable
data class NeedsAttentionDto(
    val totalImmobilized: Double = 0.0,
    val items: List<StalledItemDto> = emptyList(),
)

@Serializable
data class StalledItemDto(
    val productId: String,
    val name: String = "",
    val imageUrl: String? = null,
    val stock: Int = 0,
    val price: Double = 0.0,
    val immobilizedAmount: Double = 0.0,
)

/**
 * `GET /api/v1/dashboard/activity` — distinct tablet sessions and product views. A tablet that
 * emits no events yields zeros, not an error.
 */
@Serializable
data class StoreActivityDto(
    val periodStart: String? = null,
    val periodEnd: String? = null,
    val uniqueSessions: Int = 0,
    val uniqueSessionsDeltaPercent: Double? = null,
    val productViews: Int = 0,
    val productViewsDeltaPercent: Double? = null,
)
