package com.qhathia.mobile.staff.core.data.repository

import com.qhathia.mobile.staff.core.domain.model.UpdateProfileError
import com.qhathia.mobile.staff.core.domain.model.UpdateProfileException
import com.qhathia.mobile.staff.core.network.NetworkResult
import com.qhathia.mobile.staff.core.network.dto.UserProfileDto
import com.qhathia.mobile.staff.core.network.service.UserApiService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.IOException

class UserProfileRepositoryImplTest {

    private val api = mockk<UserApiService>()
    private val repository = UserProfileRepositoryImpl(api)

    private fun dto(fullName: String = "Carla Mendoza", phone: String? = "999888777") = UserProfileDto(
        id = "user-1",
        email = "carla.mendoza@gmail.com",
        fullName = fullName,
        phone = phone,
    )

    private fun errorOf(result: Result<*>): UpdateProfileError =
        (result.exceptionOrNull() as UpdateProfileException).error

    @Test
    fun `a successful update returns the server's profile`() = runTest {
        coEvery { api.updateMe(fullName = "Carla Ruiz", phone = null) } returns
            NetworkResult.Success(dto(fullName = "Carla Ruiz"))

        val profile = repository.updateProfile(fullName = "Carla Ruiz", phone = null).getOrThrow()

        assertEquals("Carla Ruiz", profile.fullName)
        assertEquals("999888777", profile.phone)
    }

    @Test
    fun `only the given fields reach the service`() = runTest {
        coEvery { api.updateMe(any(), any()) } returns NetworkResult.Success(dto(phone = "987654321"))

        repository.updateProfile(fullName = null, phone = "987654321")

        coVerify(exactly = 1) { api.updateMe(fullName = null, phone = "987654321") }
    }

    @Test
    fun `a 422 is a validation error`() = runTest {
        coEvery { api.updateMe(any(), any()) } returns NetworkResult.HttpError(422, "Unprocessable")

        assertEquals(UpdateProfileError.Validation, errorOf(repository.updateProfile("C", null)))
    }

    @Test
    fun `no connectivity is a network error`() = runTest {
        coEvery { api.updateMe(any(), any()) } returns NetworkResult.NetworkError(IOException("offline"))

        assertEquals(UpdateProfileError.Network, errorOf(repository.updateProfile("Carla", null)))
    }

    @Test
    fun `any other status is unknown`() = runTest {
        coEvery { api.updateMe(any(), any()) } returns NetworkResult.HttpError(500, "boom")

        assertEquals(UpdateProfileError.Unknown("HTTP 500"), errorOf(repository.updateProfile("Carla", null)))
    }
}
