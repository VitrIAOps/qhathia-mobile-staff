package com.qhathia.mobile.staff.core.data.mapper

import com.qhathia.mobile.staff.core.domain.model.AuthTokens
import com.qhathia.mobile.staff.core.domain.model.Session
import com.qhathia.mobile.staff.core.domain.model.Store
import com.qhathia.mobile.staff.core.domain.model.User
import com.qhathia.mobile.staff.core.domain.model.UserRole
import com.qhathia.mobile.staff.core.network.dto.AuthResponseDto
import com.qhathia.mobile.staff.core.network.dto.StoreDto
import com.qhathia.mobile.staff.core.network.dto.UserDto

/** Maps the token fields of an auth response (shared by login and refresh). */
fun AuthResponseDto.toTokens(): AuthTokens = AuthTokens(
    accessToken = accessToken,
    refreshToken = refreshToken,
    expiresInSeconds = expiresIn,
)

/**
 * Maps a full login response to a domain [Session].
 * @throws IllegalArgumentException if the response has no user (not a valid login response).
 */
fun AuthResponseDto.toSession(): Session = Session(
    tokens = toTokens(),
    user = requireNotNull(user) { "Auth response is missing the user" }.toDomain(),
    activeStore = store?.toDomain(),
)

fun UserDto.toDomain(): User = User(
    id = id,
    email = email,
    fullName = fullName,
    role = role.toUserRole(),
)

fun StoreDto.toDomain(): Store = Store(
    id = id,
    shoppingCenterId = shoppingCenterId,
    name = name,
    category = category,
)

private fun String.toUserRole(): UserRole = when (uppercase()) {
    "OWNER" -> UserRole.OWNER
    "SELLER" -> UserRole.SELLER
    "ADMIN" -> UserRole.ADMIN
    "DEVICE" -> UserRole.DEVICE
    else -> UserRole.UNKNOWN
}
