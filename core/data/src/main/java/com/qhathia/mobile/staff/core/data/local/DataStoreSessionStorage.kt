package com.qhathia.mobile.staff.core.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.qhathia.mobile.staff.core.domain.model.Session
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import java.io.IOException
import javax.inject.Inject

/**
 * [SessionStorage] over DataStore Preferences. The session is serialized to JSON, encrypted by
 * [SessionCipher], and stored as a single Base64 blob so tokens are never persisted in clear text.
 */
class DataStoreSessionStorage @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val cipher: SessionCipher,
    private val json: Json,
) : SessionStorage {

    private val blobKey = stringPreferencesKey(SESSION_BLOB_KEY)

    override fun sessionFlow(): Flow<Session?> = dataStore.data
        .catch { throwable ->
            if (throwable is IOException) emit(emptyPreferences()) else throw throwable
        }
        .map { prefs -> prefs[blobKey]?.let(::decode) }

    override suspend fun save(session: Session) {
        val blob = cipher.encrypt(json.encodeToString(session.toPersisted()))
        dataStore.edit { prefs -> prefs[blobKey] = blob }
    }

    override suspend fun clear() {
        dataStore.edit { prefs -> prefs.remove(blobKey) }
    }

    override suspend fun current(): Session? = sessionFlow().first()

    private fun decode(stored: String): Session? = runCatching {
        json.decodeFromString<PersistedSession>(cipher.decrypt(stored)).toDomain()
    }.getOrNull()

    private companion object {
        const val SESSION_BLOB_KEY = "session_blob"
    }
}
