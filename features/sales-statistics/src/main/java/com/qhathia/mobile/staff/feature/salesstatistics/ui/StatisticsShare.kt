package com.qhathia.mobile.staff.feature.salesstatistics.ui

import androidx.annotation.StringRes
import com.qhathia.mobile.staff.core.ui.component.sales.toSolesLabel
import com.qhathia.mobile.staff.feature.salesstatistics.R
import com.qhathia.mobile.staff.feature.salesstatistics.model.SalesStatisticsUiState
import com.qhathia.mobile.staff.feature.salesstatistics.model.StatisticsTab
import java.time.Instant

/** How many top sellers the shared text lists. */
private const val SHARED_TOP_SELLERS = 5

/** The period bounds of the data [tab] shows, or nulls before it loaded. */
internal fun SalesStatisticsUiState.boundsOf(tab: StatisticsTab): Pair<Instant?, Instant?> = when (tab) {
    StatisticsTab.SUMMARY -> summary.data?.let { it.periodStart to it.periodEnd }
    StatisticsTab.RANKING -> ranking.data?.let { it.periodStart to it.periodEnd }
    StatisticsTab.ACTIVITY -> activity.data?.let { it.periodStart to it.periodEnd }
} ?: (null to null)

/**
 * The plain text "Compartir" sends for [tab]: the title and [range], then the tab's figures. Only
 * what the screen already loaded, and never customer data. Null when the tab has nothing loaded.
 *
 * [text] resolves a string resource with its arguments (`Resources.getString`), which keeps this
 * function free of Android and testable on the JVM.
 */
internal fun shareText(
    tab: StatisticsTab,
    state: SalesStatisticsUiState,
    range: String?,
    text: (resId: Int, args: Array<out Any>) -> String,
): String? {
    fun s(@StringRes id: Int, vararg args: Any) = text(id, args)
    val period = range.orEmpty()
    return when (tab) {
        StatisticsTab.SUMMARY -> state.summary.data?.let { summary ->
            s(
                R.string.statistics_share_summary,
                period,
                summary.total.toSolesLabel(),
                summary.kpis.salesCount,
                summary.kpis.averageTicket.toSolesLabel(),
                summary.kpis.unitsSold,
            )
        }

        StatisticsTab.RANKING -> state.ranking.data?.let { ranking ->
            val rows = ranking.topSellers.take(SHARED_TOP_SELLERS).mapIndexed { index, product ->
                s(R.string.statistics_share_ranking_row, index + 1, product.name, product.revenue.toSolesLabel())
            }
            (listOf(s(R.string.statistics_share_ranking_header, period)) + rows).joinToString("\n")
        }

        StatisticsTab.ACTIVITY -> state.activity.data?.let { activity ->
            val orders = activity.confirmedOrders
            if (orders != null) {
                s(R.string.statistics_share_activity, period, activity.uniqueSessions, activity.productViews, orders)
            } else {
                s(R.string.statistics_share_activity_no_orders, period, activity.uniqueSessions, activity.productViews)
            }
        }
    }
}
