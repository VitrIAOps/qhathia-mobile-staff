package com.qhathia.mobile.staff.core.data.repository

import com.qhathia.mobile.staff.core.data.mapper.toDomain
import com.qhathia.mobile.staff.core.domain.model.ActiveSession
import com.qhathia.mobile.staff.core.domain.model.ChangePasswordError
import com.qhathia.mobile.staff.core.domain.model.ChangePasswordException
import com.qhathia.mobile.staff.core.domain.repository.AccountSecurityRepository
import com.qhathia.mobile.staff.core.domain.repository.SessionRepository
import com.qhathia.mobile.staff.core.network.NetworkResult
import com.qhathia.mobile.staff.core.network.dto.ChangePasswordRequestDto
import com.qhathia.mobile.staff.core.network.service.AuthApiService
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * [AccountSecurityRepository] over the Auth API. The stored refresh token identifies this device to
 * the backend: it lets `GET /sessions` flag the current one, keeps this device alive on
 * `revoke-others`, and keeps it signed in after a password change.
 */
class AccountSecurityRepositoryImpl @Inject constructor(
    private val apiService: AuthApiService,
    private val sessionRepository: SessionRepository,
) : AccountSecurityRepository {

    override suspend fun getSessions(): Result<List<ActiveSession>> =
        apiService.getSessions(currentRefreshToken()).asDomainResult { sessions ->
            sessions.map { it.toDomain() }
        }

    override suspend fun revokeSession(sessionId: String): Result<Unit> {
        val result = apiService.revokeSession(sessionId)
        // 404: the session is already gone (expired, or closed from another device) — the goal holds.
        if (result is NetworkResult.HttpError && result.code == HTTP_NOT_FOUND) {
            return Result.success(Unit)
        }
        return result.asDomainResult { }
    }

    override suspend fun revokeOtherSessions(): Result<Unit> {
        // Without the header the backend closes every session, this device's included.
        val refreshToken = currentRefreshToken()
            ?: return Result.failure(IllegalStateException("No session to keep"))
        return apiService.revokeOthers(refreshToken).asDomainResult { }
    }

    override suspend fun changePassword(currentPassword: String, newPassword: String): Result<Unit> {
        val request = ChangePasswordRequestDto(
            currentPassword = currentPassword,
            newPassword = newPassword,
            currentRefreshToken = currentRefreshToken(),
        )
        return when (val result = apiService.changePassword(request)) {
            is NetworkResult.Success -> Result.success(Unit)
            is NetworkResult.HttpError ->
                Result.failure(ChangePasswordException(result.toChangePasswordError()))
            is NetworkResult.NetworkError ->
                Result.failure(ChangePasswordException(ChangePasswordError.Network))
        }
    }

    private suspend fun currentRefreshToken(): String? =
        sessionRepository.observeSession().first()?.tokens?.refreshToken

    /**
     * By the ProblemDetail `errorCode` first: only `CURRENT_PASSWORD_INCORRECT` means a wrong
     * password. Any other 401 is about the session, which the client's auth plugin already handled.
     */
    private fun NetworkResult.HttpError.toChangePasswordError(): ChangePasswordError = when {
        code == 401 && problem?.errorCode == ERROR_CURRENT_PASSWORD_INCORRECT -> ChangePasswordError.WrongCurrent
        code == 400 -> ChangePasswordError.SameAsCurrent
        code == 422 -> ChangePasswordError.Validation
        else -> ChangePasswordError.Unknown("HTTP $code")
    }

    private companion object {
        const val HTTP_NOT_FOUND = 404
        const val ERROR_CURRENT_PASSWORD_INCORRECT = "CURRENT_PASSWORD_INCORRECT"
    }
}
