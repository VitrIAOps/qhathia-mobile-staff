package com.qhathia.mobile.staff.core.data.di

import javax.inject.Qualifier

/**
 * The device-preferences DataStore (language, fingerprint sign-in). Qualified to keep it apart from
 * the session DataStore, which is cleared on sign out.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AppPreferencesDataStore
