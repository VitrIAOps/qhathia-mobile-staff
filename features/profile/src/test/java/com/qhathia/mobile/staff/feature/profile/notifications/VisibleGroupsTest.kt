package com.qhathia.mobile.staff.feature.profile.notifications

import com.qhathia.mobile.staff.core.domain.model.NotificationPreference
import com.qhathia.mobile.staff.core.domain.model.NotificationPreferences
import com.qhathia.mobile.staff.feature.profile.R
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class VisibleGroupsTest {

    private fun preferences(supported: Set<NotificationPreference>) = NotificationPreferences(
        values = NotificationPreference.entries.associateWith { true },
        activeCount = 7,
        totalCount = 7,
        supported = supported,
    )

    @Test
    fun `only the toggles the backend sends are shown, and empty groups are left out`() {
        val groups = visibleGroups(
            preferences(setOf(NotificationPreference.NEW_ORDER, NotificationPreference.ORDER_EXPIRING)),
        )

        assertEquals(listOf(R.string.notifications_group_orders), groups.map { it.title })
        assertEquals(
            listOf(NotificationPreference.NEW_ORDER, NotificationPreference.ORDER_EXPIRING),
            groups.single().toggles.map { it.preference },
        )
    }

    @Test
    fun `a toggle that becomes supported brings its group back`() {
        val groups = visibleGroups(
            preferences(setOf(NotificationPreference.NEW_ORDER, NotificationPreference.LOW_BATTERY)),
        )

        assertEquals(
            listOf(R.string.notifications_group_orders, R.string.notifications_group_tablet),
            groups.map { it.title },
        )
        assertEquals(listOf(NotificationPreference.LOW_BATTERY), groups[1].toggles.map { it.preference })
    }
}
