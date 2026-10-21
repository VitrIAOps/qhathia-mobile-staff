package com.qhathia.mobile.staff.core.domain.repository

import com.qhathia.mobile.staff.core.domain.model.UserProfile

/** Port for reading the authenticated user's profile (`GET /users/me`). */
interface UserProfileRepository {
    suspend fun getProfile(): Result<UserProfile>

    /**
     * Partial update of the name and/or phone: a `null` argument leaves that field unchanged, so a
     * phone cannot be removed. The email is not part of it. Failures carry an
     * `UpdateProfileException`.
     */
    suspend fun updateProfile(fullName: String?, phone: String?): Result<UserProfile>
}
