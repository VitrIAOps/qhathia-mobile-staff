package com.qhathia.mobile.staff.core.data.local

import com.qhathia.mobile.staff.core.domain.model.AuthTokens
import com.qhathia.mobile.staff.core.domain.model.Session
import com.qhathia.mobile.staff.core.domain.model.Store
import com.qhathia.mobile.staff.core.domain.model.User
import com.qhathia.mobile.staff.core.domain.model.UserRole
import kotlinx.serialization.Serializable

/**
 * Serializable mirror of [Session] persisted (encrypted) as a single JSON blob. [sessionId] defaults
 * to `null` so blobs written before it existed still decode — nobody is signed out by the update.
 */
@Serializable
internal data class PersistedSession(
    val accessToken: String,
    val refreshToken: String,
    val expiresInSeconds: Long,
    val userId: String,
    val userEmail: String,
    val userFullName: String,
    val userRole: String,
    val store: PersistedStore?,
    val sessionId: String? = null,
)

@Serializable
internal data class PersistedStore(
    val id: String,
    val shoppingCenterId: String?,
    val name: String,
    val category: String,
)

internal fun Session.toPersisted(): PersistedSession = PersistedSession(
    accessToken = tokens.accessToken,
    refreshToken = tokens.refreshToken,
    expiresInSeconds = tokens.expiresInSeconds,
    userId = user.id,
    userEmail = user.email,
    userFullName = user.fullName,
    userRole = user.role.name,
    store = activeStore?.let { PersistedStore(it.id, it.shoppingCenterId, it.name, it.category) },
    sessionId = sessionId,
)

internal fun PersistedSession.toDomain(): Session = Session(
    tokens = AuthTokens(accessToken, refreshToken, expiresInSeconds),
    user = User(
        id = userId,
        email = userEmail,
        fullName = userFullName,
        role = runCatching { UserRole.valueOf(userRole) }.getOrDefault(UserRole.UNKNOWN),
    ),
    activeStore = store?.let { Store(it.id, it.shoppingCenterId, it.name, it.category) },
    sessionId = sessionId,
)
