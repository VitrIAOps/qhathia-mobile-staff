package com.qhathia.mobile.staff.core.network.di

import javax.inject.Qualifier

/** HttpClient without authentication — used for public endpoints (login, refresh). */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class PublicClient

/** HttpClient with the bearer Auth plugin — used for authenticated endpoints. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AuthenticatedClient
