package com.qhathia.mobile.staff.core.data.repository

import com.qhathia.mobile.staff.core.domain.repository.AuthTokenCache
import com.qhathia.mobile.staff.core.network.di.AuthenticatedClient
import com.qhathia.mobile.staff.core.network.forgetBearerTokens
import io.ktor.client.HttpClient
import javax.inject.Inject
import javax.inject.Provider

/**
 * [AuthTokenCache] over the authenticated Ktor client. Takes a [Provider] so building it does not
 * build the client (which needs the session-backed token provider) and no dependency cycle forms.
 */
class KtorBearerTokenCache @Inject constructor(
    @AuthenticatedClient private val client: Provider<HttpClient>,
) : AuthTokenCache {

    override fun clear() {
        client.get().forgetBearerTokens()
    }
}
