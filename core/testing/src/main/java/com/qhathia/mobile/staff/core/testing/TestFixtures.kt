package com.qhathia.mobile.staff.core.testing

import com.qhathia.mobile.staff.core.domain.model.AuthTokens
import com.qhathia.mobile.staff.core.domain.model.Session
import com.qhathia.mobile.staff.core.domain.model.Store
import com.qhathia.mobile.staff.core.domain.model.User
import com.qhathia.mobile.staff.core.domain.model.UserRole

/** Shared domain fixtures for tests. */
object TestFixtures {

    fun tokens(
        accessToken: String = "access-token",
        refreshToken: String = "refresh-token",
        expiresInSeconds: Long = 86_400L,
    ) = AuthTokens(accessToken, refreshToken, expiresInSeconds)

    fun user(
        id: String = "user-1",
        email: String = "owner@tienda.com",
        fullName: String = "Juan Pérez",
        role: UserRole = UserRole.OWNER,
    ) = User(id, email, fullName, role)

    fun store(
        id: String = "store-1",
        shoppingCenterId: String? = "sc-1",
        name: String = "Tienda Gamarra",
        category: String = "TEXTILE",
    ) = Store(id, shoppingCenterId, name, category)

    fun session(
        tokens: AuthTokens = tokens(),
        user: User = user(),
        activeStore: Store? = store(),
    ) = Session(tokens, user, activeStore)
}
