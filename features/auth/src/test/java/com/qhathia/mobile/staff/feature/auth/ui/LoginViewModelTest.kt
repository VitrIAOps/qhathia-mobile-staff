package com.qhathia.mobile.staff.feature.auth.ui

import app.cash.turbine.test
import com.qhathia.mobile.staff.core.domain.model.LoginError
import com.qhathia.mobile.staff.core.domain.model.LoginException
import com.qhathia.mobile.staff.core.domain.usecase.LoginUseCase
import com.qhathia.mobile.staff.core.testing.FakeAuthRepository
import com.qhathia.mobile.staff.core.testing.FakeSessionRepository
import com.qhathia.mobile.staff.core.testing.MainDispatcherExtension
import com.qhathia.mobile.staff.feature.auth.R
import com.qhathia.mobile.staff.feature.auth.model.LoginUiEffect
import com.qhathia.mobile.staff.feature.auth.model.LoginUiEvent
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class LoginViewModelTest {

    @JvmField
    @RegisterExtension
    val mainDispatcher = MainDispatcherExtension()

    private lateinit var authRepository: FakeAuthRepository
    private lateinit var sessionRepository: FakeSessionRepository
    private lateinit var viewModel: LoginViewModel

    @BeforeEach
    fun setUp() {
        authRepository = FakeAuthRepository()
        sessionRepository = FakeSessionRepository()
        viewModel = LoginViewModel(LoginUseCase(authRepository, sessionRepository))
    }

    private fun enterValidCredentials() {
        viewModel.onEvent(LoginUiEvent.EmailChanged("owner@tienda.com"))
        viewModel.onEvent(LoginUiEvent.PasswordChanged("secret123"))
    }

    @Test
    fun `given invalid input when submit then field errors set and no login call`() = runTest {
        viewModel.onEvent(LoginUiEvent.EmailChanged("owner"))
        viewModel.onEvent(LoginUiEvent.PasswordChanged(""))

        viewModel.onEvent(LoginUiEvent.Submit)

        val state = viewModel.uiState.value
        assertEquals(R.string.login_error_invalid_email, state.emailError)
        assertEquals(R.string.login_error_empty_password, state.passwordError)
        assertEquals(0, authRepository.loginCallCount)
    }

    @Test
    fun `given valid credentials when submit succeeds then NavigateToHome is emitted`() = runTest {
        enterValidCredentials()

        viewModel.uiEffect.test {
            viewModel.onEvent(LoginUiEvent.Submit)
            assertEquals(LoginUiEffect.NavigateToHome, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        assertFalse(viewModel.uiState.value.isSubmitting)
    }

    @Test
    fun `given invalid credentials when submit then ShowError with credentials message`() = runTest {
        authRepository.loginResult = Result.failure(LoginException(LoginError.InvalidCredentials))
        enterValidCredentials()

        viewModel.uiEffect.test {
            viewModel.onEvent(LoginUiEvent.Submit)
            val effect = awaitItem()
            assertTrue(effect is LoginUiEffect.ShowError)
            assertEquals(
                R.string.login_error_invalid_credentials,
                (effect as LoginUiEffect.ShowError).messageRes,
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `given login in flight then isSubmitting is true until it completes`() = runTest {
        val gate = CompletableDeferred<Unit>()
        authRepository.loginGate = gate
        enterValidCredentials()

        viewModel.onEvent(LoginUiEvent.Submit)
        runCurrent()
        assertTrue(viewModel.uiState.value.isSubmitting)

        gate.complete(Unit)
        runCurrent()
        assertFalse(viewModel.uiState.value.isSubmitting)
    }
}
