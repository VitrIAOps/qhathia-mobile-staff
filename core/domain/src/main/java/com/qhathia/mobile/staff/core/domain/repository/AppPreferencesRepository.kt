package com.qhathia.mobile.staff.core.domain.repository

import com.qhathia.mobile.staff.core.domain.model.AppLanguage
import kotlinx.coroutines.flow.Flow

/**
 * Port for preferences owned by this device, independent of any session: signing out never clears
 * them.
 */
interface AppPreferencesRepository {

    /** The app language chosen on this device, or `null` before one was ever stored. */
    fun observeLanguage(): Flow<AppLanguage?>

    suspend fun setLanguage(language: AppLanguage)

    /** Whether fingerprint sign-in is enabled on this device. Defaults to `false`. */
    fun observeBiometricEnabled(): Flow<Boolean>

    suspend fun setBiometricEnabled(enabled: Boolean)
}
