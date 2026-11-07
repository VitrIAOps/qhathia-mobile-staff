package com.qhathia.mobile.staff.feature.profile.personal

import app.cash.turbine.test
import com.qhathia.mobile.staff.core.domain.model.UpdateProfileError
import com.qhathia.mobile.staff.core.domain.model.UpdateProfileException
import com.qhathia.mobile.staff.core.domain.usecase.GetUserProfileUseCase
import com.qhathia.mobile.staff.core.domain.usecase.UpdateUserProfileUseCase
import com.qhathia.mobile.staff.core.testing.FakeSessionRepository
import com.qhathia.mobile.staff.core.testing.FakeUserProfileRepository
import com.qhathia.mobile.staff.core.testing.MainDispatcherExtension
import com.qhathia.mobile.staff.core.testing.TestFixtures
import com.qhathia.mobile.staff.feature.profile.R
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class PersonalDataViewModelTest {

    @JvmField
    @RegisterExtension
    val mainDispatcher = MainDispatcherExtension()

    private val profiles = FakeUserProfileRepository().apply {
        profileResult = Result.success(TestFixtures.userProfile(fullName = "Carla Mendoza", phone = "999888777"))
    }
    private val sessions = FakeSessionRepository(TestFixtures.session())

    private fun createViewModel() = PersonalDataViewModel(
        getUserProfileUseCase = GetUserProfileUseCase(profiles),
        updateUserProfileUseCase = UpdateUserProfileUseCase(profiles, sessions),
    )

    @Test
    fun `the loaded profile fills the fields and nothing can be saved yet`() = runTest {
        val state = createViewModel().uiState.value

        assertEquals("Carla Mendoza", state.fullName)
        assertEquals("999888777", state.phone)
        assertFalse(state.isDirty)
        assertFalse(state.canSave)
        assertNull(state.nameErrorRes)
        assertNull(state.phoneErrorRes)
    }

    @Test
    fun `a trailing space alone is not a change`() = runTest {
        val viewModel = createViewModel()

        viewModel.onEvent(PersonalDataUiEvent.NameChanged("Carla Mendoza "))

        assertFalse(viewModel.uiState.value.canSave)
    }

    @Test
    fun `saving a new name sends only the trimmed name`() = runTest {
        val viewModel = createViewModel()

        viewModel.onEvent(PersonalDataUiEvent.NameChanged("  Carla Mendoza Ruiz "))
        viewModel.onEvent(PersonalDataUiEvent.Save)

        assertEquals(FakeUserProfileRepository.Update(fullName = "Carla Mendoza Ruiz", phone = null), profiles.updates.single())
    }

    @Test
    fun `saving a new phone sends only the phone`() = runTest {
        val viewModel = createViewModel()

        viewModel.onEvent(PersonalDataUiEvent.PhoneChanged("987 654 321"))
        viewModel.onEvent(PersonalDataUiEvent.Save)

        assertEquals(FakeUserProfileRepository.Update(fullName = null, phone = "987654321"), profiles.updates.single())
    }

    @Test
    fun `a successful save confirms it and resets the form to the saved values`() = runTest {
        val viewModel = createViewModel()

        viewModel.uiEffect.test {
            viewModel.onEvent(PersonalDataUiEvent.NameChanged("Carla Ruiz"))
            viewModel.onEvent(PersonalDataUiEvent.Save)
            assertEquals(PersonalDataUiEffect.Saved, awaitItem())
        }
        val state = viewModel.uiState.value
        assertEquals("Carla Ruiz", state.fullName)
        assertFalse(state.isDirty)
        assertFalse(state.isSaving)
    }

    @Test
    fun `an invalid phone shows an error and blocks saving`() = runTest {
        val viewModel = createViewModel()

        viewModel.onEvent(PersonalDataUiEvent.PhoneChanged("88765432"))

        assertEquals(R.string.personal_error_phone, viewModel.uiState.value.phoneErrorRes)
        assertFalse(viewModel.uiState.value.canSave)
        viewModel.onEvent(PersonalDataUiEvent.Save)
        assertTrue(profiles.updates.isEmpty())
    }

    @Test
    fun `a too short name shows an error and blocks saving`() = runTest {
        val viewModel = createViewModel()

        viewModel.onEvent(PersonalDataUiEvent.NameChanged("C"))

        assertEquals(R.string.personal_error_name, viewModel.uiState.value.nameErrorRes)
        assertFalse(viewModel.uiState.value.canSave)
    }

    @Test
    fun `clearing a stored phone enables save and sends an empty string`() = runTest {
        val viewModel = createViewModel()

        viewModel.onEvent(PersonalDataUiEvent.PhoneChanged(""))

        val state = viewModel.uiState.value
        assertNull(state.phoneErrorRes)
        assertTrue(state.isRemovingPhone)
        assertTrue(state.canSave)
        viewModel.onEvent(PersonalDataUiEvent.Save)
        assertEquals(FakeUserProfileRepository.Update(fullName = null, phone = ""), profiles.updates.single())
        // The backend answers phone = null: the field stays empty and nothing is left to save.
        assertEquals("", viewModel.uiState.value.phone)
        assertFalse(viewModel.uiState.value.isRemovingPhone)
        assertFalse(viewModel.uiState.value.isDirty)
    }

    @Test
    fun `back with unsaved changes asks before leaving`() = runTest {
        val viewModel = createViewModel()
        viewModel.onEvent(PersonalDataUiEvent.PhoneChanged("987654321"))

        viewModel.uiEffect.test {
            viewModel.onEvent(PersonalDataUiEvent.BackRequested)
            assertTrue(viewModel.uiState.value.isDiscardDialogVisible)
            expectNoEvents()
        }
    }

    @Test
    fun `keep editing closes the dialog and keeps the typed values`() = runTest {
        val viewModel = createViewModel()
        viewModel.onEvent(PersonalDataUiEvent.PhoneChanged("987654321"))
        viewModel.onEvent(PersonalDataUiEvent.BackRequested)

        viewModel.onEvent(PersonalDataUiEvent.DiscardDismissed)

        assertFalse(viewModel.uiState.value.isDiscardDialogVisible)
        assertEquals("987654321", viewModel.uiState.value.phone)
    }

    @Test
    fun `discard leaves`() = runTest {
        val viewModel = createViewModel()
        viewModel.onEvent(PersonalDataUiEvent.PhoneChanged("987654321"))
        viewModel.onEvent(PersonalDataUiEvent.BackRequested)

        viewModel.uiEffect.test {
            viewModel.onEvent(PersonalDataUiEvent.DiscardConfirmed)
            assertEquals(PersonalDataUiEffect.NavigateBack, awaitItem())
        }
        assertFalse(viewModel.uiState.value.isDiscardDialogVisible)
        assertTrue(profiles.updates.isEmpty())
    }

    @Test
    fun `back with no changes leaves at once`() = runTest {
        val viewModel = createViewModel()

        viewModel.uiEffect.test {
            viewModel.onEvent(PersonalDataUiEvent.BackRequested)
            assertEquals(PersonalDataUiEffect.NavigateBack, awaitItem())
        }
        assertFalse(viewModel.uiState.value.isDiscardDialogVisible)
    }

    @Test
    fun `back while saving does nothing`() = runTest {
        profiles.updateGate = CompletableDeferred()
        val viewModel = createViewModel()
        viewModel.onEvent(PersonalDataUiEvent.PhoneChanged("987654321"))
        viewModel.onEvent(PersonalDataUiEvent.Save)
        assertTrue(viewModel.uiState.value.isSaving)

        viewModel.uiEffect.test {
            viewModel.onEvent(PersonalDataUiEvent.BackRequested)
            expectNoEvents()
        }
        assertFalse(viewModel.uiState.value.isDiscardDialogVisible)
        profiles.updateGate?.complete(Unit)
    }

    @Test
    fun `without a stored phone an empty field is fine`() = runTest {
        profiles.profileResult = Result.success(TestFixtures.userProfile(fullName = "Carla Mendoza", phone = null))
        val viewModel = createViewModel()

        viewModel.onEvent(PersonalDataUiEvent.NameChanged("Carla Ruiz"))

        assertNull(viewModel.uiState.value.phoneErrorRes)
        viewModel.onEvent(PersonalDataUiEvent.Save)
        assertEquals(FakeUserProfileRepository.Update(fullName = "Carla Ruiz", phone = null), profiles.updates.single())
    }

    @Test
    fun `a rejected save keeps the typed values and asks to review them`() = runTest {
        profiles.updateResult = Result.failure(UpdateProfileException(UpdateProfileError.Validation))
        val viewModel = createViewModel()

        viewModel.onEvent(PersonalDataUiEvent.NameChanged("Carla Ruiz"))
        viewModel.onEvent(PersonalDataUiEvent.Save)

        val state = viewModel.uiState.value
        assertEquals("Carla Ruiz", state.fullName)
        assertEquals(R.string.personal_error_save_validation, state.bannerErrorRes)
        assertFalse(state.isSaving)
        assertTrue(state.canSave)
    }

    @Test
    fun `an offline save shows the connectivity message`() = runTest {
        profiles.updateResult = Result.failure(UpdateProfileException(UpdateProfileError.Network))
        val viewModel = createViewModel()

        viewModel.onEvent(PersonalDataUiEvent.NameChanged("Carla Ruiz"))
        viewModel.onEvent(PersonalDataUiEvent.Save)

        assertEquals(R.string.personal_error_save_network, viewModel.uiState.value.bannerErrorRes)
    }

    @Test
    fun `any other failure shows the generic message, cleared on the next edit`() = runTest {
        profiles.updateResult = Result.failure(IllegalStateException("boom"))
        val viewModel = createViewModel()

        viewModel.onEvent(PersonalDataUiEvent.NameChanged("Carla Ruiz"))
        viewModel.onEvent(PersonalDataUiEvent.Save)
        assertEquals(R.string.personal_error_save_unknown, viewModel.uiState.value.bannerErrorRes)

        viewModel.onEvent(PersonalDataUiEvent.NameChanged("Carla Ruiz M"))
        assertNull(viewModel.uiState.value.bannerErrorRes)
    }
}
