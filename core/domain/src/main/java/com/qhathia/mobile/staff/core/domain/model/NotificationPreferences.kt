package com.qhathia.mobile.staff.core.domain.model

/**
 * The push-notification toggles of the current user in the active store.
 *
 * @property activeCount how many toggles are on, as counted by the backend.
 * @property totalCount how many toggles exist, so the UI never hardcodes "7".
 */
data class NotificationPreferences(
    val values: Map<NotificationPreference, Boolean>,
    val activeCount: Int,
    val totalCount: Int,
) {
    /** A toggle absent from [values] reads as on: absence of preferences never silences. */
    fun isEnabled(preference: NotificationPreference): Boolean = values[preference] ?: true
}

/** The seven toggles, in the order the screen groups them. */
enum class NotificationPreference {
    NEW_ORDER,
    ORDER_EXPIRING,
    TABLET_DISCONNECTED,
    PRODUCT_OUT_OF_STOCK,
    LOW_BATTERY,
    WEEKLY_REPORT,
    SELLING_SUGGESTIONS,
}
