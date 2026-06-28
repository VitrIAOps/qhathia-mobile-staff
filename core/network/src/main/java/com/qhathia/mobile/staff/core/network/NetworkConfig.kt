package com.qhathia.mobile.staff.core.network

/**
 * Runtime network configuration supplied by the application module (from its `BuildConfig`),
 * keeping environment values out of the network module itself.
 *
 * @property baseUrl API base URL, e.g. `http://87.99.156.164`.
 * @property enableLogging when true, Ktor logs request/response bodies (debug builds only).
 */
data class NetworkConfig(
    val baseUrl: String,
    val enableLogging: Boolean,
)
