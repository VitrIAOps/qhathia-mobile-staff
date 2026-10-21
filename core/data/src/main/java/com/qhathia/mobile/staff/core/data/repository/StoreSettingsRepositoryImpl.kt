package com.qhathia.mobile.staff.core.data.repository

import com.qhathia.mobile.staff.core.data.mapper.toDomain
import com.qhathia.mobile.staff.core.domain.model.AppLanguage
import com.qhathia.mobile.staff.core.domain.model.StoreSettings
import com.qhathia.mobile.staff.core.domain.repository.SessionRepository
import com.qhathia.mobile.staff.core.domain.repository.StoreSettingsRepository
import com.qhathia.mobile.staff.core.network.service.StoreApiService
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * [StoreSettingsRepository] over `/stores/{storeId}` for the session's active store. A session
 * without an active store (a multi-store user before `switch-store`) fails every call.
 */
class StoreSettingsRepositoryImpl @Inject constructor(
    private val apiService: StoreApiService,
    private val sessionRepository: SessionRepository,
) : StoreSettingsRepository {

    override suspend fun get(): Result<StoreSettings> = withActiveStore { storeId ->
        apiService.getStore(storeId).asDomainResult { it.toDomain() }
    }

    override suspend fun updateLanguage(language: AppLanguage): Result<StoreSettings> =
        withActiveStore { storeId ->
            apiService.updateStore(storeId, language = language.tag)
                .asDomainResult { it.toDomain() }
        }

    private suspend fun withActiveStore(
        block: suspend (storeId: String) -> Result<StoreSettings>,
    ): Result<StoreSettings> {
        val storeId = sessionRepository.observeSession().first()?.activeStore?.id
            ?: return Result.failure(IllegalStateException("No active store"))
        return block(storeId)
    }
}
