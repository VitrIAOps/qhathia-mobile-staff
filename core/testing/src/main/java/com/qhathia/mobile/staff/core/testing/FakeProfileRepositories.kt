package com.qhathia.mobile.staff.core.testing

import com.qhathia.mobile.staff.core.domain.model.ActiveSession
import com.qhathia.mobile.staff.core.domain.model.AppLanguage
import com.qhathia.mobile.staff.core.domain.model.NotificationPreference
import com.qhathia.mobile.staff.core.domain.model.NotificationPreferences
import com.qhathia.mobile.staff.core.domain.model.StoreSettings
import com.qhathia.mobile.staff.core.domain.model.Subscription
import com.qhathia.mobile.staff.core.domain.model.UserProfile
import com.qhathia.mobile.staff.core.domain.repository.AccountSecurityRepository
import com.qhathia.mobile.staff.core.domain.repository.AppPreferencesRepository
import com.qhathia.mobile.staff.core.domain.repository.NotificationPreferencesRepository
import com.qhathia.mobile.staff.core.domain.repository.StoreSettingsRepository
import com.qhathia.mobile.staff.core.domain.repository.SubscriptionRepository
import com.qhathia.mobile.staff.core.domain.repository.UserProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Controllable [UserProfileRepository]. */
class FakeUserProfileRepository : UserProfileRepository {

    var profileResult: Result<UserProfile> = Result.success(TestFixtures.userProfile())

    var callCount: Int = 0
        private set

    override suspend fun getProfile(): Result<UserProfile> {
        callCount++
        return profileResult
    }
}

/** Controllable [SubscriptionRepository]. A successful `null` stands for "no subscription". */
class FakeSubscriptionRepository : SubscriptionRepository {

    var subscriptionResult: Result<Subscription?> = Result.success(TestFixtures.subscription())

    var callCount: Int = 0
        private set

    override suspend fun getCurrent(): Result<Subscription?> {
        callCount++
        return subscriptionResult
    }
}

/**
 * Stateful [NotificationPreferencesRepository]: a successful [set] updates [current] and recounts
 * the active toggles, like the backend. Set [setFailure] to make saves fail.
 */
class FakeNotificationPreferencesRepository : NotificationPreferencesRepository {

    var current: NotificationPreferences = TestFixtures.notificationPreferences()
    var getFailure: Throwable? = null
    var setFailure: Throwable? = null

    var getCallCount: Int = 0
        private set

    /** Every `(preference, enabled)` save requested, in order. */
    val saves: MutableList<Pair<NotificationPreference, Boolean>> = mutableListOf()

    override suspend fun get(): Result<NotificationPreferences> {
        getCallCount++
        return getFailure?.let { Result.failure(it) } ?: Result.success(current)
    }

    override suspend fun set(
        preference: NotificationPreference,
        enabled: Boolean,
    ): Result<NotificationPreferences> {
        saves += preference to enabled
        setFailure?.let { return Result.failure(it) }
        val values = current.values + (preference to enabled)
        current = current.copy(values = values, activeCount = values.count { it.value })
        return Result.success(current)
    }
}

/** Controllable [AccountSecurityRepository] recording every call. */
class FakeAccountSecurityRepository : AccountSecurityRepository {

    var sessionsResult: Result<List<ActiveSession>> = Result.success(
        listOf(
            TestFixtures.activeSession(sessionId = "session-1", isCurrent = true),
            TestFixtures.activeSession(sessionId = "session-2", deviceLabel = "Tablet vitrina"),
        ),
    )
    var revokeResult: Result<Unit> = Result.success(Unit)
    var revokeOthersResult: Result<Unit> = Result.success(Unit)
    var changePasswordResult: Result<Unit> = Result.success(Unit)

    var getSessionsCallCount: Int = 0
        private set
    var revokeOthersCallCount: Int = 0
        private set
    val revokedSessionIds: MutableList<String> = mutableListOf()
    val passwordChanges: MutableList<Pair<String, String>> = mutableListOf()

    override suspend fun getSessions(): Result<List<ActiveSession>> {
        getSessionsCallCount++
        return sessionsResult
    }

    override suspend fun revokeSession(sessionId: String): Result<Unit> {
        revokedSessionIds += sessionId
        return revokeResult
    }

    override suspend fun revokeOtherSessions(): Result<Unit> {
        revokeOthersCallCount++
        return revokeOthersResult
    }

    override suspend fun changePassword(currentPassword: String, newPassword: String): Result<Unit> {
        passwordChanges += currentPassword to newPassword
        return changePasswordResult
    }
}

/** Controllable [StoreSettingsRepository] recording the updates sent. */
class FakeStoreSettingsRepository : StoreSettingsRepository {

    var settingsResult: Result<StoreSettings> = Result.success(TestFixtures.storeSettings())
    var updateFailure: Throwable? = null

    var getCallCount: Int = 0
        private set
    val languageUpdates: MutableList<AppLanguage> = mutableListOf()
    val igvUpdates: MutableList<Boolean> = mutableListOf()

    override suspend fun get(): Result<StoreSettings> {
        getCallCount++
        return settingsResult
    }

    override suspend fun updateLanguage(language: AppLanguage): Result<StoreSettings> {
        languageUpdates += language
        return update { it.copy(language = language) }
    }

    override suspend fun updatePricesIncludeIgv(pricesIncludeIgv: Boolean): Result<StoreSettings> {
        igvUpdates += pricesIncludeIgv
        return update { it.copy(pricesIncludeIgv = pricesIncludeIgv) }
    }

    private fun update(change: (StoreSettings) -> StoreSettings): Result<StoreSettings> {
        updateFailure?.let { return Result.failure(it) }
        val updated = change(settingsResult.getOrNull() ?: TestFixtures.storeSettings())
        settingsResult = Result.success(updated)
        return settingsResult
    }
}

/** In-memory [AppPreferencesRepository] backed by state flows. */
class FakeAppPreferencesRepository(
    language: AppLanguage? = null,
    biometricEnabled: Boolean = false,
) : AppPreferencesRepository {

    private val languageState = MutableStateFlow(language)
    private val biometricState = MutableStateFlow(biometricEnabled)

    /** Set to make [setLanguage] throw, as a failing disk write would. */
    var setLanguageFailure: Throwable? = null

    val language: AppLanguage? get() = languageState.value
    val biometricEnabled: Boolean get() = biometricState.value

    override fun observeLanguage(): Flow<AppLanguage?> = languageState.asStateFlow()

    override suspend fun setLanguage(language: AppLanguage) {
        setLanguageFailure?.let { throw it }
        languageState.value = language
    }

    override fun observeBiometricEnabled(): Flow<Boolean> = biometricState.asStateFlow()

    override suspend fun setBiometricEnabled(enabled: Boolean) {
        biometricState.value = enabled
    }
}
