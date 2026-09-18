package com.qhathia.mobile.staff.core.network.dto

import kotlinx.serialization.Serializable

/**
 * Request body for `POST /api/v1/auth/login`. [deviceLabel] names the phone in the Seguridad
 * session list (max 100); the backend falls back to the User-Agent when it is absent.
 */
@Serializable
data class LoginRequestDto(
    val email: String,
    val password: String,
    val deviceLabel: String? = null,
)

/** Request body for `POST /api/v1/auth/refresh`. */
@Serializable
data class RefreshRequestDto(
    val refreshToken: String,
)

/** Request body for `POST /api/v1/auth/logout`. */
@Serializable
data class LogoutRequestDto(
    val refreshToken: String,
)

/**
 * Response shared by login, register, switch-store and refresh. `user`/`store`/`stores` are
 * absent on the refresh response, so they are nullable/defaulted. [sessionId] identifies this
 * device's session and is only present on token-issuing responses.
 */
@Serializable
data class AuthResponseDto(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String? = null,
    val expiresIn: Long = 0L,
    val user: UserDto? = null,
    val store: StoreDto? = null,
    val stores: List<StoreDto> = emptyList(),
    val sessionId: String? = null,
)

@Serializable
data class UserDto(
    val id: String,
    val email: String,
    val fullName: String,
    val role: String,
)

@Serializable
data class StoreDto(
    val id: String,
    val shoppingCenterId: String? = null,
    val name: String,
    val category: String,
)

/** Row of `GET /api/v1/auth/sessions`. The refresh token never travels in this list. */
@Serializable
data class ActiveSessionDto(
    val sessionId: String,
    val deviceLabel: String? = null,
    val createdAt: String? = null,
    val lastUsedAt: String? = null,
    val current: Boolean = false,
)

/**
 * Body of `POST /api/v1/auth/change-password`. [currentRefreshToken] keeps this device's session
 * alive; without it every session is closed.
 */
@Serializable
data class ChangePasswordRequestDto(
    val currentPassword: String,
    val newPassword: String,
    val currentRefreshToken: String? = null,
)
