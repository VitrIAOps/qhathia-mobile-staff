package com.qhathia.mobile.staff.feature.profile.hub

import com.qhathia.mobile.staff.core.domain.model.Subscription
import com.qhathia.mobile.staff.core.domain.model.UserProfile
import com.qhathia.mobile.staff.core.domain.model.UserRole
import com.qhathia.mobile.staff.feature.profile.common.LoadState
import com.qhathia.mobile.staff.feature.profile.common.valueOrNull
import java.time.Instant

/** Where Mi perfil can take the merchant. */
enum class ProfileDestination {
    PERSONAL_DATA,
    NOTIFICATIONS,
    SECURITY,
    SETTINGS,
    DEVICES,
    HELP,
    SUBSCRIPTION,
    ORDER_REQUESTS,
}

/**
 * Snapshot of Mi perfil. Each block loads on its own, so one failing request never blanks the
 * others. Until the profile arrives, name and email come from the session, which is always at hand.
 *
 * @property subscription `Loaded(null)` means the store has no subscription.
 * @property alertCounts active and total notification toggles, or `null` when unknown.
 * @property connectedTablets tablets currently connected, or `null` when unknown.
 */
data class ProfileHubUiState(
    val sessionName: String = "",
    val sessionEmail: String = "",
    val role: UserRole = UserRole.UNKNOWN,
    val profile: LoadState<UserProfile> = LoadState.Loading,
    val subscription: LoadState<Subscription?> = LoadState.Loading,
    val alertCounts: AlertCounts? = null,
    val connectedTablets: Int? = null,
    val isLogoutConfirmVisible: Boolean = false,
    val now: Instant = Instant.EPOCH,
) {
    /** Subscription and device management are OWNER/ADMIN only. */
    val canManageStore: Boolean
        get() = role == UserRole.OWNER || role == UserRole.ADMIN

    val displayName: String get() = profile.valueOrNull?.fullName ?: sessionName
    val displayEmail: String get() = profile.valueOrNull?.email ?: sessionEmail
    val storeCount: Int? get() = profile.valueOrNull?.storeCount
}

data class AlertCounts(val active: Int, val total: Int)

sealed interface ProfileHubUiEvent {
    /** Reload every block — sent on start and whenever the screen resumes. */
    data object Refresh : ProfileHubUiEvent

    data class Open(val destination: ProfileDestination) : ProfileHubUiEvent

    data object LogoutRequested : ProfileHubUiEvent
    data object LogoutDismissed : ProfileHubUiEvent
    data object LogoutConfirmed : ProfileHubUiEvent
}

sealed interface ProfileHubUiEffect {
    data class Navigate(val destination: ProfileDestination) : ProfileHubUiEffect
}
