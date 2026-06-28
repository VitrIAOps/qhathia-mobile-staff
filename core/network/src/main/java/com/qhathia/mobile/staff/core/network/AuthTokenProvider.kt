package com.qhathia.mobile.staff.core.network

import io.ktor.client.plugins.auth.providers.BearerTokens

/**
 * Supplies and refreshes bearer tokens for the authenticated Ktor client. Implemented by the
 * data layer (which owns session storage and the refresh call). Defined here so the Ktor `Auth`
 * plugin can depend on it without the network module knowing about session storage.
 */
interface AuthTokenProvider {

    /** The currently stored tokens, or `null` when there is no session. */
    suspend fun loadTokens(): BearerTokens?

    /**
     * Obtain a fresh token pair (token rotation) and persist it. Returns `null` if refresh fails,
     * in which case the implementation clears the session.
     */
    suspend fun refreshTokens(): BearerTokens?
}
