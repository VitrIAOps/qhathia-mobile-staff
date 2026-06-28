package com.qhathia.mobile.staff.core.domain.usecase

import com.qhathia.mobile.staff.core.domain.model.LoginError
import com.qhathia.mobile.staff.core.domain.model.LoginException
import com.qhathia.mobile.staff.core.testing.FakeAuthRepository
import com.qhathia.mobile.staff.core.testing.FakeSessionRepository
import com.qhathia.mobile.staff.core.testing.TestFixtures
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class LoginUseCaseTest {

    private lateinit var authRepository: FakeAuthRepository
    private lateinit var sessionRepository: FakeSessionRepository
    private lateinit var useCase: LoginUseCase

    @BeforeEach
    fun setUp() {
        authRepository = FakeAuthRepository()
        sessionRepository = FakeSessionRepository()
        useCase = LoginUseCase(authRepository, sessionRepository)
    }

    @Test
    fun `given valid credentials when invoked then session is returned and persisted`() = runTest {
        val expected = TestFixtures.session()
        authRepository.loginResult = Result.success(expected)

        val result = useCase("owner@tienda.com", "secret123")

        assertTrue(result.isSuccess)
        assertEquals(expected, result.getOrNull())
        assertEquals(1, sessionRepository.saveCallCount)
        assertEquals(expected.tokens.accessToken, sessionRepository.currentAccessToken())
    }

    @Test
    fun `given email with surrounding spaces when invoked then it is trimmed before login`() =
        runTest {
            useCase("  owner@tienda.com  ", "secret123")

            assertEquals("owner@tienda.com", authRepository.lastLoginEmail)
        }

    @Test
    fun `given invalid credentials when invoked then failure is propagated and nothing persisted`() =
        runTest {
            authRepository.loginResult =
                Result.failure(LoginException(LoginError.InvalidCredentials))

            val result = useCase("owner@tienda.com", "wrong")

            assertTrue(result.isFailure)
            val error = (result.exceptionOrNull() as LoginException).error
            assertInstanceOf(LoginError.InvalidCredentials::class.java, error)
            assertEquals(0, sessionRepository.saveCallCount)
            assertNull(sessionRepository.currentAccessToken())
        }
}
