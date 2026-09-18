package com.qhathia.mobile.staff.core.domain.model

import java.time.Instant

/**
 * The authenticated user's profile as served by `GET /users/me`. Read-only in the app.
 *
 * @property phone a 9-digit Peruvian mobile without the `+51` prefix, or `null` when not set.
 * @property passwordUpdatedAt `null` when the user never changed the password.
 * @property storeCount how many stores the user can operate.
 */
data class UserProfile(
    val id: String,
    val fullName: String,
    val email: String,
    val phone: String?,
    val passwordUpdatedAt: Instant?,
    val role: UserRole,
    val storeCount: Int,
)
