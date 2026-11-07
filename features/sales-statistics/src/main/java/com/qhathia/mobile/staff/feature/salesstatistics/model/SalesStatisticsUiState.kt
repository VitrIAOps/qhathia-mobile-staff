package com.qhathia.mobile.staff.feature.salesstatistics.model

import androidx.compose.runtime.Immutable
import com.qhathia.mobile.staff.core.domain.model.Category
import com.qhathia.mobile.staff.core.domain.model.SalesPeriod
import com.qhathia.mobile.staff.core.domain.model.SalesRanking
import com.qhathia.mobile.staff.core.domain.model.SalesSummary
import com.qhathia.mobile.staff.core.domain.model.StoreActivity
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * Immutable snapshot of the "Resumen de ventas" screen.
 *
 * [period] and [selectedCategoryId] (null = "Todos") are shared by every tab, but Actividad ignores
 * the category — its endpoint has no such filter — while keeping it for when the merchant returns to
 * Resumen or Ranking. Each tab carries its own [TabContent], so switching tabs never blanks one that
 * already loaded for the current filters.
 */
@Immutable
data class SalesStatisticsUiState(
    val selectedTab: StatisticsTab = StatisticsTab.SUMMARY,
    val period: SalesPeriod = SalesPeriod.WEEKLY,
    val categories: ImmutableList<Category> = persistentListOf(),
    val selectedCategoryId: String? = null,
    val isFiltersExpanded: Boolean = false,
    val summary: TabContent<SalesSummary> = TabContent(),
    val ranking: TabContent<SalesRanking> = TabContent(),
    val activity: TabContent<StoreActivity> = TabContent(),
) {
    /** The category selector is hidden on Actividad, which cannot be filtered by category. */
    val isCategoryFilterVisible: Boolean get() = selectedTab != StatisticsTab.ACTIVITY

    /** Share needs something to share: the tab on screen loaded and not reloading. */
    val isShareEnabled: Boolean
        get() = when (selectedTab) {
            StatisticsTab.SUMMARY -> summary.data != null && !summary.isLoading
            StatisticsTab.RANKING -> ranking.data != null && !ranking.isLoading
            StatisticsTab.ACTIVITY -> activity.data != null && !activity.isLoading
        }
}
