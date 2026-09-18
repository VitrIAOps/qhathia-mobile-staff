package com.qhathia.mobile.staff.feature.profile.navigation

import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.qhathia.mobile.staff.feature.profile.help.HelpScreen
import com.qhathia.mobile.staff.feature.profile.hub.ProfileDestination
import com.qhathia.mobile.staff.feature.profile.hub.ProfileHubScreen
import com.qhathia.mobile.staff.feature.profile.notifications.NotificationPreferencesScreen
import com.qhathia.mobile.staff.feature.profile.personal.PersonalDataScreen
import com.qhathia.mobile.staff.feature.profile.security.ChangePasswordScreen
import com.qhathia.mobile.staff.feature.profile.security.SecurityScreen
import com.qhathia.mobile.staff.feature.profile.settings.AppSettingsScreen
import com.qhathia.mobile.staff.feature.profile.subscription.SubscriptionScreen

/** The Perfil tab root: Mi perfil. */
const val PROFILE_ROUTE = "profile"

private const val PERSONAL_DATA_ROUTE = "profile/personal"
private const val NOTIFICATIONS_ROUTE = "profile/notifications"
private const val SECURITY_ROUTE = "profile/security"
private const val CHANGE_PASSWORD_ROUTE = "profile/security/password"
private const val SETTINGS_ROUTE = "profile/settings"
private const val SUBSCRIPTION_ROUTE = "profile/subscription"
private const val HELP_ROUTE = "profile/help"

/** Result Cambiar contraseña leaves on the Seguridad entry. */
private const val PASSWORD_CHANGED_KEY = "password_changed"

/**
 * Registers Mi perfil and its sub-screens. Every sub-screen is a flat route pushed over Mi perfil, so
 * the shell — which shows the bottom bar only on top-level routes — hides it there, and back returns
 * to Mi perfil.
 *
 * Dispositivos lives in its own feature module and Solicitudes is a tab, so both are handed back to
 * the shell through [onNavigateToDevices] and [onNavigateToOrderRequests].
 */
fun NavGraphBuilder.profileGraph(
    navController: NavController,
    onNavigateToDevices: () -> Unit,
    onNavigateToOrderRequests: () -> Unit,
) {
    val back: () -> Unit = { navController.popBackStack() }

    composable(PROFILE_ROUTE) {
        ProfileHubScreen(
            onNavigate = { destination ->
                when (destination) {
                    ProfileDestination.PERSONAL_DATA -> navController.navigate(PERSONAL_DATA_ROUTE)
                    ProfileDestination.NOTIFICATIONS -> navController.navigate(NOTIFICATIONS_ROUTE)
                    ProfileDestination.SECURITY -> navController.navigate(SECURITY_ROUTE)
                    ProfileDestination.SETTINGS -> navController.navigate(SETTINGS_ROUTE)
                    ProfileDestination.SUBSCRIPTION -> navController.navigate(SUBSCRIPTION_ROUTE)
                    ProfileDestination.HELP -> navController.navigate(HELP_ROUTE)
                    ProfileDestination.DEVICES -> onNavigateToDevices()
                    ProfileDestination.ORDER_REQUESTS -> onNavigateToOrderRequests()
                }
            },
        )
    }
    composable(PERSONAL_DATA_ROUTE) { PersonalDataScreen(onBack = back) }
    composable(NOTIFICATIONS_ROUTE) { NotificationPreferencesScreen(onBack = back) }
    composable(SECURITY_ROUTE) { entry ->
        val passwordChanged by entry.savedStateHandle
            .getStateFlow(PASSWORD_CHANGED_KEY, false)
            .collectAsStateWithLifecycle()
        SecurityScreen(
            passwordChanged = passwordChanged,
            onPasswordChangedConsumed = { entry.savedStateHandle[PASSWORD_CHANGED_KEY] = false },
            onNavigateToChangePassword = { navController.navigate(CHANGE_PASSWORD_ROUTE) },
            onBack = back,
        )
    }
    composable(CHANGE_PASSWORD_ROUTE) {
        ChangePasswordScreen(
            onPasswordChanged = {
                navController.previousBackStackEntry?.savedStateHandle?.set(PASSWORD_CHANGED_KEY, true)
                navController.popBackStack()
            },
            onBack = back,
        )
    }
    composable(SETTINGS_ROUTE) { AppSettingsScreen(onBack = back) }
    composable(SUBSCRIPTION_ROUTE) { SubscriptionScreen(onBack = back) }
    composable(HELP_ROUTE) { HelpScreen(onBack = back) }
}
