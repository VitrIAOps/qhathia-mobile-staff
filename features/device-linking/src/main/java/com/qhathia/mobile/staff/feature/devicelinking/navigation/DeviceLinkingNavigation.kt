package com.qhathia.mobile.staff.feature.devicelinking.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.qhathia.mobile.staff.feature.devicelinking.ui.DeviceLinkingScreen

/** Stable route id for the linked-devices destination. */
const val DEVICE_LINKING_ROUTE = "device_linking"

/**
 * Registers the linked-devices destination. It is not a top-level destination, so the app shell
 * hides the bottom navigation bar while it is shown. [onBack] returns to the Profile section.
 */
fun NavGraphBuilder.deviceLinkingGraph(onBack: () -> Unit) {
    composable(route = DEVICE_LINKING_ROUTE) {
        DeviceLinkingScreen(onBack = onBack)
    }
}

/** Convenience navigation to the linked-devices destination. */
fun NavController.navigateToDeviceLinking() = navigate(DEVICE_LINKING_ROUTE)
