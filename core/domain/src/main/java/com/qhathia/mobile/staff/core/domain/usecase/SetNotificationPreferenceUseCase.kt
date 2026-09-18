package com.qhathia.mobile.staff.core.domain.usecase

import com.qhathia.mobile.staff.core.domain.model.NotificationPreference
import com.qhathia.mobile.staff.core.domain.model.NotificationPreferences
import com.qhathia.mobile.staff.core.domain.repository.NotificationPreferencesRepository
import javax.inject.Inject

/** Saves a single toggle and returns the full set as applied. */
class SetNotificationPreferenceUseCase @Inject constructor(
    private val repository: NotificationPreferencesRepository,
) {
    suspend operator fun invoke(
        preference: NotificationPreference,
        enabled: Boolean,
    ): Result<NotificationPreferences> = repository.set(preference, enabled)
}
