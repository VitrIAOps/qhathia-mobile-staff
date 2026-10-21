package com.qhathia.mobile.staff.feature.profile.settings

import com.qhathia.mobile.staff.core.domain.model.AppLanguage
import com.qhathia.mobile.staff.core.domain.model.UserRole
import com.qhathia.mobile.staff.core.domain.usecase.GetStoreSettingsUseCase
import com.qhathia.mobile.staff.core.domain.usecase.ObserveAppLanguageUseCase
import com.qhathia.mobile.staff.core.domain.usecase.ObserveSessionUseCase
import com.qhathia.mobile.staff.core.domain.usecase.SetAppLanguageUseCase
import com.qhathia.mobile.staff.core.testing.FakeAppPreferencesRepository
import com.qhathia.mobile.staff.core.testing.FakeSessionRepository
import com.qhathia.mobile.staff.core.testing.FakeStoreSettingsRepository
import com.qhathia.mobile.staff.core.testing.MainDispatcherExtension
import com.qhathia.mobile.staff.core.testing.TestFixtures
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import java.io.IOException

class AppSettingsViewModelTest {

    @JvmField
    @RegisterExtension
    val mainDispatcher = MainDispatcherExtension()

    private val prefs = FakeAppPreferencesRepository(language = AppLanguage.SPANISH)
    private val store = FakeStoreSettingsRepository()

    private fun createViewModel(role: UserRole = UserRole.OWNER): AppSettingsViewModel {
        val sessions = FakeSessionRepository(TestFixtures.session(user = TestFixtures.user(role = role)))
        return AppSettingsViewModel(
            observeAppLanguageUseCase = ObserveAppLanguageUseCase(prefs),
            observeSessionUseCase = ObserveSessionUseCase(sessions),
            setAppLanguageUseCase = SetAppLanguageUseCase(prefs, store, sessions),
            getStoreSettingsUseCase = GetStoreSettingsUseCase(store),
        )
    }

    @Test
    fun `the device language wins over the store and is re-sent when they disagree`() = runTest {
        prefs.setLanguage(AppLanguage.ENGLISH)
        store.settingsResult = Result.success(TestFixtures.storeSettings(language = AppLanguage.SPANISH))

        val viewModel = createViewModel()

        assertEquals(AppLanguage.ENGLISH, viewModel.uiState.value.language)
        assertEquals(listOf(AppLanguage.ENGLISH), store.languageUpdates)
    }

    @Test
    fun `nothing is re-sent when the store already agrees`() = runTest {
        createViewModel()

        assertTrue(store.languageUpdates.isEmpty())
    }

    @Test
    fun `selecting a language stores it and mirrors it for an owner`() = runTest {
        val viewModel = createViewModel()

        viewModel.onEvent(AppSettingsUiEvent.SelectLanguage(AppLanguage.ENGLISH))

        assertEquals(AppLanguage.ENGLISH, prefs.language)
        assertEquals(AppLanguage.ENGLISH, viewModel.uiState.value.language)
        assertEquals(listOf(AppLanguage.ENGLISH), store.languageUpdates)
    }

    @Test
    fun `a seller changes the language only on the device`() = runTest {
        prefs.setLanguage(AppLanguage.ENGLISH)
        store.settingsResult = Result.success(TestFixtures.storeSettings(language = AppLanguage.SPANISH))
        val viewModel = createViewModel(role = UserRole.SELLER)

        viewModel.onEvent(AppSettingsUiEvent.SelectLanguage(AppLanguage.SPANISH))

        assertEquals(AppLanguage.SPANISH, prefs.language)
        assertTrue(store.languageUpdates.isEmpty())
    }

    @Test
    fun `a store that fails to load is silent and leaves the language usable`() = runTest {
        store.settingsResult = Result.failure(IOException("offline"))
        val viewModel = createViewModel()

        assertEquals(AppSettingsUiState(language = AppLanguage.SPANISH), viewModel.uiState.value)

        viewModel.onEvent(AppSettingsUiEvent.SelectLanguage(AppLanguage.ENGLISH))
        assertEquals(AppLanguage.ENGLISH, prefs.language)
    }
}
