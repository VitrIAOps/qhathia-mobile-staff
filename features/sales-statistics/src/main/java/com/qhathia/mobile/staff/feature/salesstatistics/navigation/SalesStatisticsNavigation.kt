package com.qhathia.mobile.staff.feature.salesstatistics.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.qhathia.mobile.staff.core.domain.model.SalesPeriod
import com.qhathia.mobile.staff.feature.salesstatistics.ui.SalesStatisticsScreen

/** Route argument carrying the period the screen opens on, as a [SalesPeriod] name. */
const val SALES_STATISTICS_ARG_PERIOD = "period"

/** Stable route pattern for the sales statistics destination. */
const val SALES_STATISTICS_ROUTE = "sales-statistics/{$SALES_STATISTICS_ARG_PERIOD}"

/** Builds the concrete route opening the screen on [period]. */
fun salesStatisticsRoute(period: SalesPeriod): String = "sales-statistics/${period.name}"

/**
 * Registers the "Resumen de ventas" destination. It is not a top-level tab, so the shell's bottom
 * bar hides while it is shown. [onBack] fires from the top-bar back control; system back pops the
 * same entry.
 */
fun NavGraphBuilder.salesStatisticsGraph(onBack: () -> Unit) {
    composable(
        route = SALES_STATISTICS_ROUTE,
        arguments = listOf(navArgument(SALES_STATISTICS_ARG_PERIOD) { type = NavType.StringType }),
    ) {
        SalesStatisticsScreen(onBack = onBack)
    }
}
