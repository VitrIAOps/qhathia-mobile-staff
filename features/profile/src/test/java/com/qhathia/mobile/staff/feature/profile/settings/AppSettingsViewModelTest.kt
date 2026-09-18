package com.qhathia.mobile.staff.feature.profile.settings

import app.cash.turbine.test
import com.qhathia.mobile.staff.core.domain.model.AppLanguage
import com.qhathia.mobile.staff.core.domain.model.UserRole
import com.qhathia.mobile.staff.core.domain.usecase.GetStoreSettingsUseCase
import com.qhathia.mobile.staff.core.domain.usecase.ObserveAppLanguageUseCase
import com.qhathia.mobile.staff.core.domain.usecase.ObserveSessionUseCase
import com.qhathia.mobile.staff.core.domain.usecase.SetAppLanguageUseCase
import com.qhathia.mobile.staff.core.domain.usecase.SetPricesIncludeIgvUseCase
import com.qhathia.mobile.staff.core.testing.FakeAppPreferencesRepository
import com.qhathia.mobile.staff.core.testing.FakeSessionRepository
import com.qhathia.mobile.staff.core.testing.FakeStoreSettingsRepository
import com.qhathia.mobile.staff.core.testing.MainDispatcherExtension
import com.qhathia.mobile.staff.core.testing.TestFixtures
import com.qhathia.mobile.staff.feature.profile.common.LoadState
import com.qhathia.mobile.staff.feature.profile.common.valueOrNull
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
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
            setPricesIncludeIgvUseCase = SetPricesIncludeIgvUseCase(store),
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
    fun `a seller changes the language only on the device and cannot touch IGV`() = runTest {
        val viewModel = createViewModel(role = UserRole.SELLER)

        viewModel.onEvent(AppSettingsUiEvent.SelectLanguage(AppLanguage.ENGLISH))
        viewModel.onEvent(AppSettingsUiEvent.ToggleIgv(false))

        assertFalse(viewModel.uiState.value.canEditStore)
        assertEquals(AppLanguage.ENGLISH, prefs.language)
        assertTrue(store.languageUpdates.isEmpty())
        assertTrue(store.igvUpdates.isEmpty())
    }

    @Test
    fun `IGV toggle saves the new declaration`() = runTest {
        val viewModel = createViewModel()

        viewModel.onEvent(AppSettingsUiEvent.ToggleIgv(false))

        assertEquals(listOf(false), store.igvUpdates)
        assertFalse(viewModel.uiState.value.store.valueOrNull!!.pricesIncludeIgv)
        assertFalse(viewModel.uiState.value.isIgvSaving)
    }

    @Test
    fun `a failed IGV save springs back and reports it`() = runTest {
        store.updateFailure = IOException("offline")
        val viewModel = createViewModel()

        viewModel.uiEffect.test {
            viewModel.onEvent(AppSettingsUiEvent.ToggleIgv(false))
            assertEquals(AppSettingsUiEffect.ShowIgvSaveError, awaitItem())
        }
        assertTrue(viewModel.uiState.value.store.valueOrNull!!.pricesIncludeIgv)
    }

    @Test
    fun `a store that fails to load leaves the language usable`() = runTest {
        store.settingsResult = Result.failure(IOException("offline"))
        val viewModel = createViewModel()

        assertEquals(LoadState.Error, viewModel.uiState.value.store)

        viewModel.onEvent(AppSettingsUiEvent.SelectLanguage(AppLanguage.ENGLISH))
        assertEquals(AppLanguage.ENGLISH, prefs.language)
    }
}
