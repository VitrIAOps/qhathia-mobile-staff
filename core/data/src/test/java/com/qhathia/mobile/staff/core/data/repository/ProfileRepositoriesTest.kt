package com.qhathia.mobile.staff.core.data.repository

import com.qhathia.mobile.staff.core.network.dto.ProblemDto
import com.qhathia.mobile.staff.core.domain.model.AppLanguage
import com.qhathia.mobile.staff.core.domain.model.ChangePasswordError
import com.qhathia.mobile.staff.core.domain.model.ChangePasswordException
import com.qhathia.mobile.staff.core.domain.model.NotificationPreference
import com.qhathia.mobile.staff.core.network.NetworkResult
import com.qhathia.mobile.staff.core.network.dto.ChangePasswordRequestDto
import com.qhathia.mobile.staff.core.network.dto.LoginRequestDto
import com.qhathia.mobile.staff.core.network.dto.NotificationPreferencesDto
import com.qhathia.mobile.staff.core.network.dto.StoreSettingsDto
import com.qhathia.mobile.staff.core.network.dto.SubscriptionDto
import com.qhathia.mobile.staff.core.network.service.AuthApiService
import com.qhathia.mobile.staff.core.network.service.NotificationApiService
import com.qhathia.mobile.staff.core.network.service.StoreApiService
import com.qhathia.mobile.staff.core.network.service.SubscriptionApiService
import com.qhathia.mobile.staff.core.testing.FakeSessionRepository
import com.qhathia.mobile.staff.core.testing.TestFixtures
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.IOException

class ProfileRepositoriesTest {

    @Test
    fun `a store without subscription is a successful null, other errors fail`() = runTest {
        val api = mockk<SubscriptionApiService>()
        val repository = SubscriptionRepositoryImpl(api)

        coEvery { api.getCurrent() } returns NetworkResult.HttpError(404, "Not Found")
        assertNull(repository.getCurrent().getOrThrow())

        coEvery { api.getCurrent() } returns NetworkResult.HttpError(500, "Server Error")
        assertTrue(repository.getCurrent().isFailure)

        coEvery { api.getCurrent() } returns NetworkResult.Success(SubscriptionDto(planType = "PRO"))
        assertEquals("PRO", repository.getCurrent().getOrThrow()?.planType)
    }

    @Test
    fun `saving a toggle sends only its field`() = runTest {
        val api = mockk<NotificationApiService>()
        coEvery { api.updatePreference("lowBattery", false) } returns
            NetworkResult.Success(NotificationPreferencesDto(lowBattery = false, activeCount = 6))

        val result = NotificationPreferencesRepositoryImpl(api).set(NotificationPreference.LOW_BATTERY, false)

        assertEquals(6, result.getOrThrow().activeCount)
        coVerify(exactly = 1) { api.updatePreference("lowBattery", false) }
    }

    @Test
    fun `a language update targets the active store`() = runTest {
        val api = mockk<StoreApiService>()
        coEvery { api.updateStore("store-1", language = "en") } returns
            NetworkResult.Success(StoreSettingsDto(id = "store-1", language = "en"))
        val repository = StoreSettingsRepositoryImpl(api, FakeSessionRepository(TestFixtures.session()))

        assertEquals(AppLanguage.ENGLISH, repository.updateLanguage(AppLanguage.ENGLISH).getOrThrow().language)
    }

    @Test
    fun `store calls fail without an active store and send nothing`() = runTest {
        val api = mockk<StoreApiService>()
        val repository = StoreSettingsRepositoryImpl(api, FakeSessionRepository(TestFixtures.session(activeStore = null)))

        assertTrue(repository.get().isFailure)
        coVerify(exactly = 0) { api.getStore(any()) }
    }

    @Test
    fun `sessions are requested with this device's refresh token`() = runTest {
        val api = mockk<AuthApiService>()
        coEvery { api.getSessions("refresh-token") } returns NetworkResult.Success(emptyList())
        val repository = AccountSecurityRepositoryImpl(api, FakeSessionRepository(TestFixtures.session()))

        assertTrue(repository.getSessions().isSuccess)
        coVerify { api.getSessions("refresh-token") }
    }

    @Test
    fun `closing a session that is already gone counts as closed`() = runTest {
        val api = mockk<AuthApiService>()
        val repository = AccountSecurityRepositoryImpl(api, FakeSessionRepository(TestFixtures.session()))

        coEvery { api.revokeSession("s2") } returns NetworkResult.HttpError(404, "Not Found")
        assertTrue(repository.revokeSession("s2").isSuccess)

        coEvery { api.revokeSession("s2") } returns NetworkResult.HttpError(500, "Server Error")
        assertTrue(repository.revokeSession("s2").isFailure)
    }

    @Test
    fun `revoking others without a session never calls the backend`() = runTest {
        val api = mockk<AuthApiService>()
        val repository = AccountSecurityRepositoryImpl(api, FakeSessionRepository(initial = null))

        assertTrue(repository.revokeOtherSessions().isFailure)
        coVerify(exactly = 0) { api.revokeOthers(any()) }
    }

    @Test
    fun `password change keeps this device signed in and maps each failure`() = runTest {
        val api = mockk<AuthApiService>()
        val body = slot<ChangePasswordRequestDto>()
        val repository = AccountSecurityRepositoryImpl(api, FakeSessionRepository(TestFixtures.session()))

        coEvery { api.changePassword(capture(body)) } returns NetworkResult.Success(Unit)
        assertTrue(repository.changePassword("Old12345", "New12345").isSuccess)
        assertEquals("refresh-token", body.captured.currentRefreshToken)

        val cases = mapOf(
            NetworkResult.HttpError(401, "", ProblemDto(errorCode = "CURRENT_PASSWORD_INCORRECT")) to
                ChangePasswordError.WrongCurrent,
            // A 401 about the session, not the password: the auth plugin already dealt with it.
            NetworkResult.HttpError(401, "", ProblemDto(errorCode = "UNAUTHORIZED")) to
                ChangePasswordError.Unknown("HTTP 401"),
            NetworkResult.HttpError(400, "") to ChangePasswordError.SameAsCurrent,
            NetworkResult.HttpError(422, "") to ChangePasswordError.Validation,
            NetworkResult.NetworkError(IOException("offline")) to ChangePasswordError.Network,
        )
        cases.forEach { (response, expected) ->
            coEvery { api.changePassword(any()) } returns response
            val error = (repository.changePassword("Old12345", "New12345").exceptionOrNull() as ChangePasswordException).error
            assertEquals(expected, error)
        }
    }

    @Test
    fun `login names this phone`() = runTest {
        val api = mockk<AuthApiService>()
        val request = slot<LoginRequestDto>()
        coEvery { api.login(capture(request)) } returns NetworkResult.HttpError(401, "")

        AuthRepositoryImpl(api) { "Xiaomi Redmi Note 12" }.login("owner@tienda.com", "secret123")

        assertEquals("Xiaomi Redmi Note 12", request.captured.deviceLabel)
    }
}
