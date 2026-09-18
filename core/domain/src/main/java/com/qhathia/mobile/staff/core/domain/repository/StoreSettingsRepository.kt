package com.qhathia.mobile.staff.core.domain.repository

import com.qhathia.mobile.staff.core.domain.model.AppLanguage
import com.qhathia.mobile.staff.core.domain.model.StoreSettings

/** Port for the active store's display settings (`GET/PUT /stores/{storeId}`). */
interface StoreSettingsRepository {

    suspend fun get(): Result<StoreSettings>

    /** Mirrors the device language to the store. OWNER/ADMIN only. */
    suspend fun updateLanguage(language: AppLanguage): Result<StoreSettings>

    /** OWNER/ADMIN only. */
    suspend fun updatePricesIncludeIgv(pricesIncludeIgv: Boolean): Result<StoreSettings>
}
