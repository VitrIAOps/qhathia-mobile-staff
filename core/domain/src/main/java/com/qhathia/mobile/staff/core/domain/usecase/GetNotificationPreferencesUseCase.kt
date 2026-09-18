package com.qhathia.mobile.staff.core.domain.usecase

import com.qhathia.mobile.staff.core.domain.model.NotificationPreferences
import com.qhathia.mobile.staff.core.domain.repository.NotificationPreferencesRepository
import javax.inject.Inject

/** Loads the seven push-notification toggles. */
class GetNotificationPreferencesUseCase @Inject constructor(
    private val repository: NotificationPreferencesRepository,
) {
    suspend operator fun invoke(): Result<NotificationPreferences> = repository.get()
}
