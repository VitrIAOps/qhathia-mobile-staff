package com.qhathia.mobile.staff.core.network.dto

import kotlinx.serialization.Serializable

/** Request body for `POST /api/v1/auth/login`. */
@Serializable
data class LoginRequestDto(
    val email: String,
    val password: String,
)

/** Request body for `POST /api/v1/auth/refresh`. */
@Serializable
data class RefreshRequestDto(
    val refreshToken: String,
)

/**
 * Response shared by login, register, switch-store and refresh. `user`/`store`/`stores` are
 * absent on the refresh response, so they are nullable/defaulted.
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
