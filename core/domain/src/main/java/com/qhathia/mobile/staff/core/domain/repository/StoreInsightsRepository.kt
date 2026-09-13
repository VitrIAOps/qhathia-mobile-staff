package com.qhathia.mobile.staff.core.domain.repository

import com.qhathia.mobile.staff.core.domain.model.SalesPeriod
import com.qhathia.mobile.staff.core.domain.model.SalesSummary
import com.qhathia.mobile.staff.core.domain.model.StoreSuggestions

/**
 * Port for the Inicio summary: what the store sold over a period, and what it could sell more of.
 *
 * Both calls return [Result]: any transport or HTTP failure arrives as a failed result, which the
 * screen turns into "keep what is on screen and offer a retry".
 */
interface StoreInsightsRepository {

    /** The sales summary for [period], including its buckets and which of them is the best. */
    suspend fun getSalesSummary(period: SalesPeriod): Result<SalesSummary>

    /**
     * Products shoppers searched for and did not find, and products about to run out. Fails if
     * either row could not be loaded, so the screen never shows one fresh row next to a stale one.
     */
    suspend fun getSuggestions(): Result<StoreSuggestions>
}
