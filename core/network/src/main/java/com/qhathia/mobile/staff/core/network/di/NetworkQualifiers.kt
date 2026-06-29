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

/** HttpClient for Cloudflare R2 uploads — AWS SigV4 signed per request, no bearer plugin. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class R2Client
