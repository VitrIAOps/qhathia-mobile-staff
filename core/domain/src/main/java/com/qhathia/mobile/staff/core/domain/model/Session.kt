package com.qhathia.mobile.staff.core.domain.model

/**
 * The authenticated merchant session, the source of truth for who is logged in and which
 * store is active. Persisted (encrypted) by the data layer and observed by the rest of the app.
 *
 * @property activeStore the store carried by the JWT. It is `null` when the user has 2+ stores
 *   (the backend omits the active store until `switch-store` is called) or is a global admin.
 * @property sessionId the server id of this device's session ("este equipo" in Seguridad). It is
 *   `null` for sessions created before the backend issued one; it survives token refresh.
 */
data class Session(
    val tokens: AuthTokens,
    val user: User,
    val activeStore: Store?,
    val sessionId: String? = null,
)

/** Bearer token pair plus access-token lifetime, as returned by login and refresh. */
data class AuthTokens(
    val accessToken: String,
    val refreshToken: String,
    val expiresInSeconds: Long,
)

/** The authenticated user identity. */
data class User(
    val id: String,
    val email: String,
    val fullName: String,
    val role: UserRole,
)

/** A merchant store the user operates. */
data class Store(
    val id: String,
    val shoppingCenterId: String?,
    val name: String,
    val category: String,
)

/** Role of the user. `UNKNOWN` guards against unrecognized server values. */
enum class UserRole {
    OWNER,
    SELLER,
    ADMIN,
    DEVICE,
    UNKNOWN,
}
