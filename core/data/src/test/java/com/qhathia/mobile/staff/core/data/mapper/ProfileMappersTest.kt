package com.qhathia.mobile.staff.core.data.mapper

import com.qhathia.mobile.staff.core.domain.model.AppLanguage
import com.qhathia.mobile.staff.core.domain.model.NotificationPreference
import com.qhathia.mobile.staff.core.domain.model.SubscriptionStatus
import com.qhathia.mobile.staff.core.domain.model.UserRole
import com.qhathia.mobile.staff.core.network.dto.ActiveSessionDto
import com.qhathia.mobile.staff.core.network.dto.NotificationPreferencesDto
import com.qhathia.mobile.staff.core.network.dto.StoreSettingsDto
import com.qhathia.mobile.staff.core.network.dto.SubscriptionDto
import com.qhathia.mobile.staff.core.network.dto.UserProfileDto
import com.qhathia.mobile.staff.core.network.dto.UserStoreDto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant

class ProfileMappersTest {

    private fun profileDto(
        phone: String? = "999888777",
        passwordUpdatedAt: String? = null,
        role: String? = "OWNER",
    ) = UserProfileDto(
        id = "u1",
        email = "carla@tienda.pe",
        fullName = "Carla Mendoza",
        phone = phone,
        passwordUpdatedAt = passwordUpdatedAt,
        role = role,
        stores = listOf(UserStoreDto("s1"), UserStoreDto("s2"), UserStoreDto("s3")),
    )

    @Test
    fun `profile keeps phone digits, a never-changed password as null and counts stores`() {
        val profile = profileDto(phone = "999 888 777").toDomain()

        assertEquals("999888777", profile.phone)
        assertNull(profile.passwordUpdatedAt)
        assertEquals(UserRole.OWNER, profile.role)
        assertEquals(3, profile.storeCount)
    }

    @Test
    fun `blank phone is null and an unknown role degrades to UNKNOWN`() {
        val profile = profileDto(phone = "  ", role = "CASHIER").toDomain()

        assertNull(profile.phone)
        assertEquals(UserRole.UNKNOWN, profile.role)
    }

    @Test
    fun `timestamps accept Z and explicit offsets, and garbage becomes null`() {
        assertEquals(
            Instant.parse("2026-09-13T14:41:55Z"),
            profileDto(passwordUpdatedAt = "2026-09-13T09:41:55-05:00").toDomain().passwordUpdatedAt,
        )
        assertEquals(
            Instant.parse("2026-09-13T09:41:55Z"),
            profileDto(passwordUpdatedAt = "2026-09-13T09:41:55Z").toDomain().passwordUpdatedAt,
        )
        assertNull(profileDto(passwordUpdatedAt = "yesterday").toDomain().passwordUpdatedAt)
    }

    @Test
    fun `subscription maps price, flags, unlimited products and open-ended period`() {
        val subscription = SubscriptionDto(
            planType = "PRO",
            planName = "Plan Pro",
            status = "ACTIVE",
            priceMonthlyPen = 89.0,
            productLimit = null,
            hasVideoProducts = true,
            startedAt = "2025-08-15T05:00:00Z",
            endsAt = null,
        ).toDomain()

        assertEquals(SubscriptionStatus.ACTIVE, subscription.status)
        assertEquals(0, subscription.priceMonthlyPen.compareTo(BigDecimal("89")))
        assertNull(subscription.features.productLimit)
        assertTrue(subscription.features.hasVideoProducts)
        assertFalse(subscription.features.hasAdvancedAnalytics)
        assertEquals(Instant.parse("2025-08-15T05:00:00Z"), subscription.startedAt)
        assertNull(subscription.endsAt)
    }

    @Test
    fun `unknown subscription status and missing price degrade safely`() {
        val subscription = SubscriptionDto(status = "TRIALING", priceMonthlyPen = null).toDomain()

        assertEquals(SubscriptionStatus.UNKNOWN, subscription.status)
        assertEquals(BigDecimal.ZERO, subscription.priceMonthlyPen)
    }

    @Test
    fun `notification preferences map all seven toggles and the counts`() {
        val prefs = NotificationPreferencesDto(lowBattery = false, activeCount = 6, totalCount = 7).toDomain()

        assertFalse(prefs.isEnabled(NotificationPreference.LOW_BATTERY))
        assertTrue(NotificationPreference.entries.filter { it != NotificationPreference.LOW_BATTERY }.all(prefs::isEnabled))
        assertEquals(6, prefs.activeCount)
        assertEquals(7, prefs.totalCount)
    }

    @Test
    fun `supported toggles and the warning minutes come from the backend`() {
        val prefs = NotificationPreferencesDto(
            supported = mapOf(
                "newOrder" to true,
                "orderExpiring" to true,
                "lowBattery" to true,
                "weeklyReport" to false,
            ),
            orderExpiringWarningMinutes = 15,
        ).toDomain()

        assertEquals(
            setOf(NotificationPreference.NEW_ORDER, NotificationPreference.ORDER_EXPIRING, NotificationPreference.LOW_BATTERY),
            prefs.supported,
        )
        assertEquals(15, prefs.orderExpiringWarningMinutes)
    }

    @Test
    fun `without a supported map only new order and order expiring are supported`() {
        val prefs = NotificationPreferencesDto().toDomain()

        assertEquals(setOf(NotificationPreference.NEW_ORDER, NotificationPreference.ORDER_EXPIRING), prefs.supported)
        assertEquals(10, prefs.orderExpiringWarningMinutes)
    }

    @Test
    fun `the supported count ignores toggles the backend does not send`() {
        val prefs = NotificationPreferencesDto(
            newOrder = false,
            supported = mapOf("newOrder" to true, "orderExpiring" to true),
        ).toDomain()

        assertEquals(1, prefs.supportedActiveCount)
        assertEquals(2, prefs.supportedCount)
    }

    @Test
    fun `each toggle names its contract field`() {
        assertEquals(
            listOf(
                "newOrder",
                "orderExpiring",
                "tabletDisconnected",
                "productOutOfStock",
                "lowBattery",
                "weeklyReport",
                "sellingSuggestions",
            ),
            NotificationPreference.entries.map { it.fieldName },
        )
    }

    @Test
    fun `a session never used reports its creation time`() {
        val session = ActiveSessionDto(
            sessionId = "s1",
            deviceLabel = " Redmi Note 12 ",
            createdAt = "2026-09-10T14:22:03Z",
            lastUsedAt = null,
            current = true,
        ).toDomain()

        assertEquals("Redmi Note 12", session.deviceLabel)
        assertEquals(Instant.parse("2026-09-10T14:22:03Z"), session.lastUsedAt)
        assertTrue(session.isCurrent)
    }

    @Test
    fun `store settings resolve the language tag and keep unsupported ones null`() {
        assertEquals(AppLanguage.ENGLISH, StoreSettingsDto(id = "s1", language = "en").toDomain().language)
        assertNull(StoreSettingsDto(id = "s1", language = "fr").toDomain().language)
    }
}
