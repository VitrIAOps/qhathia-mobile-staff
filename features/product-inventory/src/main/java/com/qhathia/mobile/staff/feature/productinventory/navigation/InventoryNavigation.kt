package com.qhathia.mobile.staff.feature.productinventory.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.qhathia.mobile.staff.feature.productinventory.ui.InventoryScreen

/** Stable route id for the inventory destination. */
const val INVENTORY_ROUTE = "inventory"

/** Registers the inventory destination. */
fun NavGraphBuilder.inventoryGraph() {
    composable(route = INVENTORY_ROUTE) {
        InventoryScreen()
    }
}
