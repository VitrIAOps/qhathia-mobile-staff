package com.qhathia.mobile.staff.core.data.repository

import com.qhathia.mobile.staff.core.data.mapper.fieldName
import com.qhathia.mobile.staff.core.data.mapper.toDomain
import com.qhathia.mobile.staff.core.domain.model.NotificationPreference
import com.qhathia.mobile.staff.core.domain.model.NotificationPreferences
import com.qhathia.mobile.staff.core.domain.repository.NotificationPreferencesRepository
import com.qhathia.mobile.staff.core.network.service.NotificationApiService
import javax.inject.Inject

/**
 * [NotificationPreferencesRepository] over `/notifications/preferences`. Each save sends only the
 * toggle that changed.
 */
class NotificationPreferencesRepositoryImpl @Inject constructor(
    private val apiService: NotificationApiService,
) : NotificationPreferencesRepository {

    override suspend fun get(): Result<NotificationPreferences> =
        apiService.getPreferences().asDomainResult { it.toDomain() }

    override suspend fun set(
        preference: NotificationPreference,
        enabled: Boolean,
    ): Result<NotificationPreferences> =
        apiService.updatePreference(preference.fieldName, enabled).asDomainResult { it.toDomain() }
}
