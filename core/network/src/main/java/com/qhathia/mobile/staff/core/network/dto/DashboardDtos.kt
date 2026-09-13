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
