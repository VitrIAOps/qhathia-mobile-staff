package com.qhathia.mobile.staff.core.domain.repository

/**
 * The access token the network layer keeps in memory between requests. It must follow the stored
 * session: cleared whenever the session is replaced or removed, so no request carries the token of a
 * session that no longer exists (F-47). Implemented in the data layer over the HTTP client.
 */
interface AuthTokenCache {

    /** Forgets the cached tokens; the next authenticated request reads the stored session. */
    fun clear()
}
