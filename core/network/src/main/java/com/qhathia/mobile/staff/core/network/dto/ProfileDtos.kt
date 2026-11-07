package com.qhathia.mobile.staff.core.network.dto

import kotlinx.serialization.Serializable

/**
 * `GET /api/v1/users/me`, and the response of `PUT` (§3, §18). [phone] is 9 digits without `+51`;
 * [passwordUpdatedAt] is null for users who never changed their password.
 */
@Serializable
data class UserProfileDto(
    val id: String,
    val email: String,
    val fullName: String,
    val phone: String? = null,
    val passwordUpdatedAt: String? = null,
    val role: String? = null,
    val stores: List<UserStoreDto> = emptyList(),
)

/** One entry of `stores[]` in the user profile. */
@Serializable
data class UserStoreDto(
    val storeId: String,
    val storeName: String? = null,
    val role: String? = null,
)

/** `GET /api/v1/subscriptions/current` (§18 shape). There is no payment data. */
@Serializable
data class SubscriptionDto(
    val planType: String? = null,
    val planName: String? = null,
    val status: String? = null,
    val priceMonthlyPen: Double? = null,
    val productLimit: Int? = null,
    val hasAdvancedAnalytics: Boolean = false,
    val hasVideoProducts: Boolean = false,
    val hasBulkPriceUpdate: Boolean = false,
    val hasPrioritySupport: Boolean = false,
    val startedAt: String? = null,
    val endsAt: String? = null,
)

/**
 * The display-settings slice of `GET/PUT /api/v1/stores/{storeId}` (§22): the store's language.
 * Other store fields are ignored by the JSON configuration. `pricesIncludeIgv` / `igvRate` were
 * retired by the backend (§27) and are not read.
 */
@Serializable
data class StoreSettingsDto(
    val id: String,
    val language: String? = null,
)

/** `GET /api/v1/catalog/count`. */
@Serializable
data class CatalogCountDto(
    val activeProducts: Int = 0,
    /** Fase 42: what a tablet actually shows, with the same filter as `/catalog/by-category`. */
    val visibleProducts: Int? = null,
    /** Fase 42: why the rest are hidden. */
    val hidden: CatalogHiddenDto? = null,
)

@Serializable
data class CatalogHiddenDto(
    val outOfStock: Int = 0,
    val lowStock: Int = 0,
    val consult: Int = 0,
    val noActiveCategory: Int = 0,
)

/**
 * `GET /api/v1/notifications/preferences` (§20). The backend never returns nulls; the defaults only
 * guard against a field being dropped.
 */
@Serializable
data class NotificationPreferencesDto(
    val newOrder: Boolean = true,
    val orderExpiring: Boolean = true,
    val tabletDisconnected: Boolean = true,
    val productOutOfStock: Boolean = true,
    val lowBattery: Boolean = true,
    val weeklyReport: Boolean = true,
    val sellingSuggestions: Boolean = true,
    val activeCount: Int = 0,
    val totalCount: Int = 7,
)
