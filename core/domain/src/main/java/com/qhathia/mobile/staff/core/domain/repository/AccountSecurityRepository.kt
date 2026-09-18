package com.qhathia.mobile.staff.core.domain.repository

import com.qhathia.mobile.staff.core.domain.model.ActiveSession

/**
 * Port for account security: the user's signed-in devices and password changes. The refresh token
 * these calls need is taken from the stored session by the implementation.
 */
interface AccountSecurityRepository {

    suspend fun getSessions(): Result<List<ActiveSession>>

    /** Closes [sessionId]. A session that no longer exists counts as closed. */
    suspend fun revokeSession(sessionId: String): Result<Unit>

    /** Closes every session except this device's. */
    suspend fun revokeOtherSessions(): Result<Unit>

    /**
     * Changes the password keeping this device signed in. Failures carry a
     * [com.qhathia.mobile.staff.core.domain.model.ChangePasswordException].
     */
    suspend fun changePassword(currentPassword: String, newPassword: String): Result<Unit>
}
