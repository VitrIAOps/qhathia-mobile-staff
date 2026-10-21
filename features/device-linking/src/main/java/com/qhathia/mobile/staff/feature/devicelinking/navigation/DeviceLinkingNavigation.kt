package com.qhathia.mobile.staff.feature.devicelinking.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.qhathia.mobile.staff.feature.devicelinking.settings.TabletSettingsScreen
import com.qhathia.mobile.staff.feature.devicelinking.ui.DeviceLinkingScreen

/** Stable route id for the Dispositivos destination. */
const val DEVICE_LINKING_ROUTE = "device_linking"

/** Route argument carrying the id of the tablet whose settings are shown. */
const val TABLET_SETTINGS_ARG_ID = "deviceId"

/** Stable route pattern for Ajustes de la tablet. */
const val TABLET_SETTINGS_ROUTE = "device_settings/{$TABLET_SETTINGS_ARG_ID}"

/** Builds the concrete route for [deviceId]. Only the id travels — the screen loads the rest. */
fun tabletSettingsRoute(deviceId: String): String = "device_settings/$deviceId"

/**
 * Registers Dispositivos and Ajustes de la tablet. Neither is a top-level destination, so the app
 * shell hides the bottom navigation bar while they are shown. [onBack] pops the current screen;
 * [onOpenTabletSettings] opens a tablet's settings from its card.
 */
fun NavGraphBuilder.deviceLinkingGraph(
    onBack: () -> Unit,
    onOpenTabletSettings: (deviceId: String) -> Unit,
) {
    composable(route = DEVICE_LINKING_ROUTE) {
        DeviceLinkingScreen(onBack = onBack, onOpenTabletSettings = onOpenTabletSettings)
    }
    composable(
        route = TABLET_SETTINGS_ROUTE,
        arguments = listOf(navArgument(TABLET_SETTINGS_ARG_ID) { type = NavType.StringType }),
    ) {
        TabletSettingsScreen(onBack = onBack)
    }
}

/** Convenience navigation to the Dispositivos destination. */
fun NavController.navigateToDeviceLinking() = navigate(DEVICE_LINKING_ROUTE)
