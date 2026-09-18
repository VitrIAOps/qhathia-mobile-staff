package com.qhathia.mobile.staff.core.domain.repository

import com.qhathia.mobile.staff.core.domain.model.NotificationPreference
import com.qhathia.mobile.staff.core.domain.model.NotificationPreferences

/** Port for the push-notification toggles of the current user in the active store. */
interface NotificationPreferencesRepository {

    suspend fun get(): Result<NotificationPreferences>

    /** Saves only [preference]; the result is the full set as applied by the backend. */
    suspend fun set(
        preference: NotificationPreference,
        enabled: Boolean,
    ): Result<NotificationPreferences>
}
