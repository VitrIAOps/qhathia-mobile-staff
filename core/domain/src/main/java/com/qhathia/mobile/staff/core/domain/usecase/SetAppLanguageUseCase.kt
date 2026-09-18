package com.qhathia.mobile.staff.core.domain.usecase

import com.qhathia.mobile.staff.core.domain.model.AppLanguage
import com.qhathia.mobile.staff.core.domain.model.UserRole
import com.qhathia.mobile.staff.core.domain.repository.AppPreferencesRepository
import com.qhathia.mobile.staff.core.domain.repository.SessionRepository
import com.qhathia.mobile.staff.core.domain.repository.StoreSettingsRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Changes the app language. The device owns the value, so it is persisted locally first and that
 * write alone decides the result. Only then, and only for roles allowed to edit the store, the value
 * is mirrored to the store as best effort — a failed mirror never reverts the language and is re-sent
 * the next time Configuración opens.
 */
class SetAppLanguageUseCase @Inject constructor(
    private val appPreferencesRepository: AppPreferencesRepository,
    private val storeSettingsRepository: StoreSettingsRepository,
    private val sessionRepository: SessionRepository,
) {
    suspend operator fun invoke(language: AppLanguage): Result<Unit> {
        val local = runCatching { appPreferencesRepository.setLanguage(language) }
        if (local.isFailure) return local

        val role = sessionRepository.observeSession().first()?.user?.role
        if (role == UserRole.OWNER || role == UserRole.ADMIN) {
            storeSettingsRepository.updateLanguage(language)
        }
        return Result.success(Unit)
    }
}
