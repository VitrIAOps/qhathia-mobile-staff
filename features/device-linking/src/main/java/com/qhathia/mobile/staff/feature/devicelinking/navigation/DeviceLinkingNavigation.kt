package com.qhathia.mobile.staff.feature.devicelinking.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.qhathia.mobile.staff.feature.devicelinking.ui.DeviceLinkingScreen

/** Stable route id for the Dispositivos destination. */
const val DEVICE_LINKING_ROUTE = "device_linking"

/**
 * Registers Dispositivos. It is not a top-level destination, so the app shell hides the bottom
 * navigation bar while it is shown. [onBack] returns to Mi perfil.
 */
fun NavGraphBuilder.deviceLinkingGraph(onBack: () -> Unit) {
    composable(route = DEVICE_LINKING_ROUTE) {
        DeviceLinkingScreen(onBack = onBack)
    }
}

/** Convenience navigation to the Dispositivos destination. */
fun NavController.navigateToDeviceLinking() = navigate(DEVICE_LINKING_ROUTE)
