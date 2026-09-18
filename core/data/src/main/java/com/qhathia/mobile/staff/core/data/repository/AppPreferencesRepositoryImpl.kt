package com.qhathia.mobile.staff.core.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.qhathia.mobile.staff.core.data.di.AppPreferencesDataStore
import com.qhathia.mobile.staff.core.domain.model.AppLanguage
import com.qhathia.mobile.staff.core.domain.repository.AppPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject

/**
 * [AppPreferencesRepository] over its own Preferences DataStore file, separate from the session's,
 * so clearing the session on sign out never touches the device's language or fingerprint choice.
 */
class AppPreferencesRepositoryImpl @Inject constructor(
    @param:AppPreferencesDataStore private val dataStore: DataStore<Preferences>,
) : AppPreferencesRepository {

    private val languageKey = stringPreferencesKey(LANGUAGE_KEY)
    private val biometricKey = booleanPreferencesKey(BIOMETRIC_KEY)

    private val preferences: Flow<Preferences> = dataStore.data.catch { throwable ->
        if (throwable is IOException) emit(emptyPreferences()) else throw throwable
    }

    override fun observeLanguage(): Flow<AppLanguage?> = preferences
        .map { prefs -> AppLanguage.fromTag(prefs[languageKey]) }
        .distinctUntilChanged()

    override suspend fun setLanguage(language: AppLanguage) {
        dataStore.edit { prefs -> prefs[languageKey] = language.tag }
    }

    override fun observeBiometricEnabled(): Flow<Boolean> = preferences
        .map { prefs -> prefs[biometricKey] ?: false }
        .distinctUntilChanged()

    override suspend fun setBiometricEnabled(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[biometricKey] = enabled }
    }

    private companion object {
        const val LANGUAGE_KEY = "app_language"
        const val BIOMETRIC_KEY = "biometric_enabled"
    }
}
