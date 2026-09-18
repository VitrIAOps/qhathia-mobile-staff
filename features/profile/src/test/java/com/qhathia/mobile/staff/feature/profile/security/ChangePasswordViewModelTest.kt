package com.qhathia.mobile.staff.feature.profile.security

import app.cash.turbine.test
import com.qhathia.mobile.staff.core.domain.model.ChangePasswordError
import com.qhathia.mobile.staff.core.domain.model.ChangePasswordException
import com.qhathia.mobile.staff.core.domain.usecase.ChangePasswordUseCase
import com.qhathia.mobile.staff.core.testing.FakeAccountSecurityRepository
import com.qhathia.mobile.staff.core.testing.MainDispatcherExtension
import com.qhathia.mobile.staff.feature.profile.R
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class ChangePasswordViewModelTest {

    @JvmField
    @RegisterExtension
    val mainDispatcher = MainDispatcherExtension()

    private val security = FakeAccountSecurityRepository()

    private fun createViewModel() = ChangePasswordViewModel(ChangePasswordUseCase(security))

    private fun ChangePasswordViewModel.fill(current: String, new: String, confirmation: String = new) {
        onEvent(ChangePasswordUiEvent.CurrentPasswordChanged(current))
        onEvent(ChangePasswordUiEvent.NewPasswordChanged(new))
        onEvent(ChangePasswordUiEvent.ConfirmationChanged(confirmation))
    }

    @Test
    fun `submit is enabled only with a current password, a valid new one and a matching confirmation`() {
        val viewModel = createViewModel()

        viewModel.fill(current = "", new = "Nueva1234")
        assertFalse(viewModel.uiState.value.canSubmit)

        viewModel.fill(current = "Vieja123", new = "Corta1")
        assertFalse(viewModel.uiState.value.canSubmit)

        viewModel.fill(current = "Vieja123", new = "Nueva1234", confirmation = "Nueva123")
        assertFalse(viewModel.uiState.value.canSubmit)
        assertTrue(viewModel.uiState.value.isConfirmationMismatch)

        viewModel.fill(current = "Vieja123", new = "Nueva1234")
        assertTrue(viewModel.uiState.value.canSubmit)
    }

    @Test
    fun `an invalid form never reaches the backend`() {
        val viewModel = createViewModel()
        viewModel.fill(current = "Vieja123", new = "Corta1")

        viewModel.onEvent(ChangePasswordUiEvent.Submit)

        assertTrue(security.passwordChanges.isEmpty())
    }

    @Test
    fun `success sends both passwords and reports back`() = runTest {
        val viewModel = createViewModel()
        viewModel.fill(current = "Vieja123", new = "Nueva1234")

        viewModel.uiEffect.test {
            viewModel.onEvent(ChangePasswordUiEvent.Submit)
            assertEquals(ChangePasswordUiEffect.PasswordChanged, awaitItem())
        }
        assertEquals(listOf("Vieja123" to "Nueva1234"), security.passwordChanges)
        assertFalse(viewModel.uiState.value.isSubmitting)
    }

    @Test
    fun `each failure lands on its field or the banner`() = runTest {
        val expectations = listOf(
            ChangePasswordError.WrongCurrent to { s: ChangePasswordUiState -> s.currentPasswordErrorRes to R.string.change_password_error_wrong_current },
            ChangePasswordError.SameAsCurrent to { s: ChangePasswordUiState -> s.newPasswordErrorRes to R.string.change_password_error_same },
            ChangePasswordError.Validation to { s: ChangePasswordUiState -> s.newPasswordErrorRes to R.string.change_password_error_length },
            ChangePasswordError.Network to { s: ChangePasswordUiState -> s.bannerErrorRes to R.string.change_password_error_network },
            ChangePasswordError.Unknown("HTTP 500") to { s: ChangePasswordUiState -> s.bannerErrorRes to R.string.change_password_error_unknown },
        )
        expectations.forEach { (error, pick) ->
            security.changePasswordResult = Result.failure(ChangePasswordException(error))
            val viewModel = createViewModel()
            viewModel.fill(current = "Vieja123", new = "Nueva1234")

            viewModel.onEvent(ChangePasswordUiEvent.Submit)

            val (actual, expected) = pick(viewModel.uiState.value)
            assertEquals(expected, actual, "for $error")
            assertFalse(viewModel.uiState.value.isSubmitting)
        }
    }

    @Test
    fun `editing a field clears its error`() = runTest {
        security.changePasswordResult = Result.failure(ChangePasswordException(ChangePasswordError.WrongCurrent))
        val viewModel = createViewModel()
        viewModel.fill(current = "Vieja123", new = "Nueva1234")
        viewModel.onEvent(ChangePasswordUiEvent.Submit)

        viewModel.onEvent(ChangePasswordUiEvent.CurrentPasswordChanged("Vieja1234"))

        assertNull(viewModel.uiState.value.currentPasswordErrorRes)
    }
}
