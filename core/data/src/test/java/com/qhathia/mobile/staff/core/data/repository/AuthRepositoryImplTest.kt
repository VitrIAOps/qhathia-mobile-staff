package com.qhathia.mobile.staff.core.data.repository

import com.qhathia.mobile.staff.core.domain.model.LoginError
import com.qhathia.mobile.staff.core.domain.model.LoginException
import com.qhathia.mobile.staff.core.network.NetworkResult
import com.qhathia.mobile.staff.core.network.dto.AuthResponseDto
import com.qhathia.mobile.staff.core.network.dto.StoreDto
import com.qhathia.mobile.staff.core.network.dto.UserDto
import com.qhathia.mobile.staff.core.network.service.AuthApiService
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.IOException

class AuthRepositoryImplTest {

    private val api = mockk<AuthApiService>()
    private val repository = AuthRepositoryImpl(api)

    private fun loginResponse() = AuthResponseDto(
        accessToken = "access",
        refreshToken = "refresh",
        tokenType = "Bearer",
        expiresIn = 86_400L,
        user = UserDto("u1", "owner@tienda.com", "Juan", "OWNER"),
        store = StoreDto("s1", "sc1", "Tienda", "TEXTILE"),
        stores = emptyList(),
    )

    @Test
    fun `given success when login then maps to domain session`() = runTest {
        coEvery { api.login(any()) } returns NetworkResult.Success(loginResponse())

        val result = repository.login("owner@tienda.com", "secret123")

        assertTrue(result.isSuccess)
        val session = result.getOrThrow()
        assertEquals("access", session.tokens.accessToken)
        assertEquals("owner@tienda.com", session.user.email)
        assertEquals("Tienda", session.activeStore?.name)
    }

    @Test
    fun `given 401 when login then fails with InvalidCredentials`() = runTest {
        coEvery { api.login(any()) } returns NetworkResult.HttpError(401, "Unauthorized")

        val error = repository.login("owner@tienda.com", "bad").error()

        assertInstanceOf(LoginError.InvalidCredentials::class.java, error)
    }

    @Test
    fun `given 400 when login then fails with Validation`() = runTest {
        coEvery { api.login(any()) } returns NetworkResult.HttpError(400, "Bad Request")

        val error = repository.login("owner@tienda.com", "bad").error()

        assertInstanceOf(LoginError.Validation::class.java, error)
    }

    @Test
    fun `given transport failure when login then fails with Network`() = runTest {
        coEvery { api.login(any()) } returns NetworkResult.NetworkError(IOException())

        val error = repository.login("owner@tienda.com", "bad").error()

        assertInstanceOf(LoginError.Network::class.java, error)
    }

    @Test
    fun `given success without user when login then fails with Unknown`() = runTest {
        coEvery { api.login(any()) } returns NetworkResult.Success(loginResponse().copy(user = null))

        val error = repository.login("owner@tienda.com", "secret123").error()

        assertInstanceOf(LoginError.Unknown::class.java, error)
    }

    @Test
    fun `given success when logout then returns success`() = runTest {
        coEvery { api.logout(any()) } returns NetworkResult.Success(Unit)

        val result = repository.logout("refresh")

        assertTrue(result.isSuccess)
    }

    @Test
    fun `given transport failure when logout then fails with Network`() = runTest {
        coEvery { api.logout(any()) } returns NetworkResult.NetworkError(IOException())

        val error = repository.logout("refresh").error()

        assertInstanceOf(LoginError.Network::class.java, error)
    }

    private fun <T> Result<T>.error(): LoginError = (exceptionOrNull() as LoginException).error
}
