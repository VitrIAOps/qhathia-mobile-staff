package com.qhathia.mobile.staff.core.data.mapper

import com.qhathia.mobile.staff.core.domain.model.ActiveSession
import com.qhathia.mobile.staff.core.domain.model.AppLanguage
import com.qhathia.mobile.staff.core.domain.model.NotificationPreference
import com.qhathia.mobile.staff.core.domain.model.NotificationPreferences
import com.qhathia.mobile.staff.core.domain.model.PlanFeatures
import com.qhathia.mobile.staff.core.domain.model.StoreSettings
import com.qhathia.mobile.staff.core.domain.model.Subscription
import com.qhathia.mobile.staff.core.domain.model.SubscriptionStatus
import com.qhathia.mobile.staff.core.domain.model.UserProfile
import com.qhathia.mobile.staff.core.domain.model.UserRole
import com.qhathia.mobile.staff.core.network.dto.ActiveSessionDto
import com.qhathia.mobile.staff.core.network.dto.NotificationPreferencesDto
import com.qhathia.mobile.staff.core.network.dto.StoreSettingsDto
import com.qhathia.mobile.staff.core.network.dto.SubscriptionDto
import com.qhathia.mobile.staff.core.network.dto.UserProfileDto
import java.math.BigDecimal

fun UserProfileDto.toDomain(): UserProfile = UserProfile(
    id = id,
    fullName = fullName,
    email = email,
    phone = phone?.filter { it.isDigit() }?.takeIf { it.isNotEmpty() },
    passwordUpdatedAt = passwordUpdatedAt.parseIsoInstant(),
    role = UserRole.entries.firstOrNull { it.name == role?.uppercase() } ?: UserRole.UNKNOWN,
    storeCount = stores.size,
)

fun SubscriptionDto.toDomain(): Subscription = Subscription(
    planType = planType.orEmpty(),
    planName = planName.orEmpty(),
    status = when (status?.uppercase()) {
        "ACTIVE" -> SubscriptionStatus.ACTIVE
        "CANCELLED", "CANCELED" -> SubscriptionStatus.CANCELLED
        "EXPIRED" -> SubscriptionStatus.EXPIRED
        else -> SubscriptionStatus.UNKNOWN
    },
    priceMonthlyPen = priceMonthlyPen?.takeIf { it.isFinite() }?.let(BigDecimal::valueOf)
        ?: BigDecimal.ZERO,
    features = PlanFeatures(
        productLimit = productLimit,
        hasAdvancedAnalytics = hasAdvancedAnalytics,
        hasVideoProducts = hasVideoProducts,
        hasBulkPriceUpdate = hasBulkPriceUpdate,
        hasPrioritySupport = hasPrioritySupport,
    ),
    startedAt = startedAt.parseIsoInstant(),
    endsAt = endsAt.parseIsoInstant(),
)

fun NotificationPreferencesDto.toDomain(): NotificationPreferences = NotificationPreferences(
    values = mapOf(
        NotificationPreference.NEW_ORDER to newOrder,
        NotificationPreference.ORDER_EXPIRING to orderExpiring,
        NotificationPreference.TABLET_DISCONNECTED to tabletDisconnected,
        NotificationPreference.PRODUCT_OUT_OF_STOCK to productOutOfStock,
        NotificationPreference.LOW_BATTERY to lowBattery,
        NotificationPreference.WEEKLY_REPORT to weeklyReport,
        NotificationPreference.SELLING_SUGGESTIONS to sellingSuggestions,
    ),
    activeCount = activeCount,
    totalCount = totalCount,
)

/** The JSON field of `/notifications/preferences` that stores this toggle. */
val NotificationPreference.fieldName: String
    get() = when (this) {
        NotificationPreference.NEW_ORDER -> "newOrder"
        NotificationPreference.ORDER_EXPIRING -> "orderExpiring"
        NotificationPreference.TABLET_DISCONNECTED -> "tabletDisconnected"
        NotificationPreference.PRODUCT_OUT_OF_STOCK -> "productOutOfStock"
        NotificationPreference.LOW_BATTERY -> "lowBattery"
        NotificationPreference.WEEKLY_REPORT -> "weeklyReport"
        NotificationPreference.SELLING_SUGGESTIONS -> "sellingSuggestions"
    }

/** A session never used since it was created reports its creation time as last use. */
fun ActiveSessionDto.toDomain(): ActiveSession = ActiveSession(
    sessionId = sessionId,
    deviceLabel = deviceLabel?.trim().orEmpty(),
    lastUsedAt = lastUsedAt.parseIsoInstant() ?: createdAt.parseIsoInstant(),
    isCurrent = current,
)

fun StoreSettingsDto.toDomain(): StoreSettings = StoreSettings(
    storeId = id,
    language = AppLanguage.fromTag(language),
    pricesIncludeIgv = pricesIncludeIgv,
    igvRate = if (igvRate.isFinite()) BigDecimal.valueOf(igvRate) else BigDecimal("0.18"),
)
