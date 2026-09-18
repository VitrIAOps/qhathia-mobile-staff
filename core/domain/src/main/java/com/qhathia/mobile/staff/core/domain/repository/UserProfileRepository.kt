package com.qhathia.mobile.staff.core.domain.repository

import com.qhathia.mobile.staff.core.domain.model.UserProfile

/** Port for reading the authenticated user's profile (`GET /users/me`). */
interface UserProfileRepository {
    suspend fun getProfile(): Result<UserProfile>
}
