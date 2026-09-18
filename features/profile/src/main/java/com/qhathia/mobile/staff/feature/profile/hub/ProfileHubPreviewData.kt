package com.qhathia.mobile.staff.feature.profile.hub

import com.qhathia.mobile.staff.core.domain.model.PlanFeatures
import com.qhathia.mobile.staff.core.domain.model.Subscription
import com.qhathia.mobile.staff.core.domain.model.SubscriptionStatus
import com.qhathia.mobile.staff.core.domain.model.UserProfile
import com.qhathia.mobile.staff.core.domain.model.UserRole
import com.qhathia.mobile.staff.feature.profile.common.LoadState
import java.math.BigDecimal
import java.time.Instant

/** Sample data for the profile previews, matching the Figma mockups. */
internal object ProfileHubPreviewData {

    val now: Instant = Instant.parse("2026-03-10T15:00:00Z")

    val profile = UserProfile(
        id = "user-1",
        fullName = "Carla Mendoza",
        email = "carla.mendoza@gmail.com",
        phone = "999888777",
        passwordUpdatedAt = Instant.parse("2025-11-10T15:00:00Z"),
        role = UserRole.OWNER,
        storeCount = 3,
    )

    val subscription = Subscription(
        planType = "PRO",
        planName = "Plan Pro",
        status = SubscriptionStatus.ACTIVE,
        priceMonthlyPen = BigDecimal("89.00"),
        features = PlanFeatures(
            productLimit = 200,
            hasAdvancedAnalytics = true,
            hasVideoProducts = true,
            hasBulkPriceUpdate = false,
            hasPrioritySupport = true,
        ),
        startedAt = Instant.parse("2025-08-15T05:00:00Z"),
        endsAt = Instant.parse("2026-09-15T05:00:00Z"),
    )

    val owner = ProfileHubUiState(
        sessionName = profile.fullName,
        sessionEmail = profile.email,
        role = UserRole.OWNER,
        profile = LoadState.Loaded(profile),
        subscription = LoadState.Loaded(subscription),
        alertCounts = AlertCounts(active = 6, total = 7),
        connectedTablets = 1,
        now = now,
    )
}
