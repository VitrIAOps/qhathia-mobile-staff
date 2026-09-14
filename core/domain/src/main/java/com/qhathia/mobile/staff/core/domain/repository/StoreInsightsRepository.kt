package com.qhathia.mobile.staff.core.domain.repository

import com.qhathia.mobile.staff.core.domain.model.SalesPeriod
import com.qhathia.mobile.staff.core.domain.model.SalesRanking
import com.qhathia.mobile.staff.core.domain.model.SalesSummary
import com.qhathia.mobile.staff.core.domain.model.StoreActivity
import com.qhathia.mobile.staff.core.domain.model.StoreSuggestions

/**
 * Port for the store dashboard: what the store sold over a period, what sold and what did not, how
 * shoppers used the tablet, and what it could sell more of.
 *
 * Every call returns [Result]: any transport or HTTP failure arrives as a failed result, which the
 * screens turn into "keep what is on screen and offer a retry".
 */
interface StoreInsightsRepository {

    /**
     * The sales summary for [period], including its buckets and which of them is the best. A
     * [categoryId] limits the figures to that category's order lines; null means every category.
     */
    suspend fun getSalesSummary(period: SalesPeriod, categoryId: String? = null): Result<SalesSummary>

    /**
     * Products shoppers searched for and did not find, and products about to run out. Fails if
     * either row could not be loaded, so the screen never shows one fresh row next to a stale one.
     */
    suspend fun getSuggestions(): Result<StoreSuggestions>

    /** The top sellers and the stock that did not move over [period], optionally for one category. */
    suspend fun getSalesRanking(period: SalesPeriod, categoryId: String?): Result<SalesRanking>

    /** Tablet sessions and product views over [period]. The backend does not filter these by category. */
    suspend fun getStoreActivity(period: SalesPeriod): Result<StoreActivity>
}
