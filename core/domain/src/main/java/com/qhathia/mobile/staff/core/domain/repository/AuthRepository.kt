package com.qhathia.mobile.staff.core.domain.repository

import com.qhathia.mobile.staff.core.domain.model.AuthTokens
import com.qhathia.mobile.staff.core.domain.model.Session

/**
 * Port for authentication against the Auth API. Implemented in the data layer, which maps
 * transport results to a domain [Result]; failures carry a
 * [com.qhathia.mobile.staff.core.domain.model.LoginException].
 */
interface AuthRepository {

    /** Authenticate with email and password, returning the full [Session] on success. */
    suspend fun login(email: String, password: String): Result<Session>

    /** Exchange a refresh token for a rotated token pair. */
    suspend fun refresh(refreshToken: String): Result<AuthTokens>

    /** Invalidate the session server-side, revoking [refreshToken]. Called on explicit sign out. */
    suspend fun logout(refreshToken: String): Result<Unit>
}
