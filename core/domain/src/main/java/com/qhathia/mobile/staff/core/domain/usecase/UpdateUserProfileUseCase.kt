package com.qhathia.mobile.staff.core.domain.usecase

import com.qhathia.mobile.staff.core.domain.model.UserProfile
import com.qhathia.mobile.staff.core.domain.repository.SessionRepository
import com.qhathia.mobile.staff.core.domain.repository.UserProfileRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Updates the merchant's name and/or phone. A `null` argument leaves that field unchanged; the
 * email is not part of it. Failures carry an `UpdateProfileException`.
 *
 * The stored session keeps its own copy of the name (Inicio greets with it), so a successful update
 * copies the saved name into it. If that local write fails the update still succeeds: the server has
 * the new name, and the next sign-in brings it back.
 */
class UpdateUserProfileUseCase @Inject constructor(
    private val userProfileRepository: UserProfileRepository,
    private val sessionRepository: SessionRepository,
) {
    suspend operator fun invoke(fullName: String?, phone: String?): Result<UserProfile> =
        userProfileRepository.updateProfile(fullName = fullName, phone = phone)
            .onSuccess { saved -> syncSessionName(saved.fullName) }

    private suspend fun syncSessionName(fullName: String) {
        try {
            val session = sessionRepository.observeSession().first() ?: return
            if (session.user.fullName == fullName) return
            sessionRepository.saveSession(session.copy(user = session.user.copy(fullName = fullName)))
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            // Best effort: see the class KDoc.
        }
    }
}
