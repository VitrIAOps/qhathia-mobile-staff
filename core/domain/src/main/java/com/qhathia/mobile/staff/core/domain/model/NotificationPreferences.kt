package com.qhathia.mobile.staff.core.domain.model

/**
 * The push-notification toggles of the current user in the active store.
 *
 * @property activeCount how many toggles are on, as counted by the backend.
 * @property totalCount how many toggles exist, so the UI never hardcodes "7".
 * @property supported the toggles the backend actually sends today; the others are stored but
 *   silent, so the app shows them as "Próximamente".
 * @property orderExpiringWarningMinutes how long before an order expires its warning is sent.
 */
data class NotificationPreferences(
    val values: Map<NotificationPreference, Boolean>,
    val activeCount: Int,
    val totalCount: Int,
    val supported: Set<NotificationPreference> = DEFAULT_SUPPORTED,
    val orderExpiringWarningMinutes: Int = DEFAULT_WARNING_MINUTES,
) {
    /** A toggle absent from [values] reads as on: absence of preferences never silences. */
    fun isEnabled(preference: NotificationPreference): Boolean = values[preference] ?: true

    fun isSupported(preference: NotificationPreference): Boolean = preference in supported

    /**
     * "N de M alertas activas" counts only alerts the backend sends. Derived from [values], so an
     * optimistic toggle moves it without waiting for the server's counts.
     */
    val supportedActiveCount: Int get() = supported.count(::isEnabled)

    val supportedCount: Int get() = supported.size

    companion object {
        /** The two alerts the backend has always sent, used when it does not say. */
        val DEFAULT_SUPPORTED: Set<NotificationPreference> =
            setOf(NotificationPreference.NEW_ORDER, NotificationPreference.ORDER_EXPIRING)

        const val DEFAULT_WARNING_MINUTES = 10
    }
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
