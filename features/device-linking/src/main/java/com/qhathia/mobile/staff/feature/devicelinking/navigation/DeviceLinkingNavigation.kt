package com.qhathia.mobile.staff.feature.devicelinking.navigation

import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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

/** Back-stack key under which Ajustes de la tablet hands Dispositivos the id of an unlinked tablet. */
const val TABLET_UNLINKED_RESULT_KEY = "tablet_unlinked_device_id"

/** Builds the concrete route for [deviceId]. Only the id travels — the screen loads the rest. */
fun tabletSettingsRoute(deviceId: String): String = "device_settings/$deviceId"

/**
 * Registers Dispositivos and Ajustes de la tablet. Neither is a top-level destination, so the app
 * shell hides the bottom navigation bar while they are shown. [onBack] pops the current screen;
 * [onOpenTabletSettings] opens a tablet's settings from its card. [onTabletUnlinked] must store the
 * id under [TABLET_UNLINKED_RESULT_KEY] on the previous back-stack entry and pop.
 */
fun NavGraphBuilder.deviceLinkingGraph(
    onBack: () -> Unit,
    onOpenTabletSettings: (deviceId: String) -> Unit,
    onTabletUnlinked: (deviceId: String) -> Unit,
) {
    composable(route = DEVICE_LINKING_ROUTE) { entry ->
        val unlinkedDeviceId by entry.savedStateHandle
            .getStateFlow<String?>(TABLET_UNLINKED_RESULT_KEY, null)
            .collectAsStateWithLifecycle()
        DeviceLinkingScreen(
            onBack = onBack,
            onOpenTabletSettings = onOpenTabletSettings,
            unlinkedDeviceId = unlinkedDeviceId,
            onUnlinkedConsumed = { entry.savedStateHandle[TABLET_UNLINKED_RESULT_KEY] = null },
        )
    }
    composable(
        route = TABLET_SETTINGS_ROUTE,
        arguments = listOf(navArgument(TABLET_SETTINGS_ARG_ID) { type = NavType.StringType }),
    ) {
        TabletSettingsScreen(onBack = onBack, onUnlinked = onTabletUnlinked)
    }
}

/** Convenience navigation to the Dispositivos destination. */
fun NavController.navigateToDeviceLinking() = navigate(DEVICE_LINKING_ROUTE)
