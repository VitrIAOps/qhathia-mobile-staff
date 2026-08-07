package com.qhathia.mobile.staff.core.data.repository

import com.qhathia.mobile.staff.core.network.NetworkResult
import com.qhathia.mobile.staff.core.network.dto.RegisterTokenRequestDto
import com.qhathia.mobile.staff.core.network.service.NotificationApiService
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.IOException

class NotificationTokenRepositoryImplTest {

    private val api = mockk<NotificationApiService>()
    private val repository = NotificationTokenRepositoryImpl(api)

    @Test
    fun `given success when registerToken then returns success with ANDROID platform`() = runTest {
        val bodySlot = slot<RegisterTokenRequestDto>()
        coEvery { api.registerToken(capture(bodySlot)) } returns NetworkResult.Success(Unit)

        val result = repository.registerToken("fcm-abc", "Pixel 7")

        assertTrue(result.isSuccess)
        assertEquals("fcm-abc", bodySlot.captured.token)
        assertEquals("ANDROID", bodySlot.captured.platform)
        assertEquals("Pixel 7", bodySlot.captured.deviceLabel)
    }

    @Test
    fun `given success when unregisterToken then returns success`() = runTest {
        coEvery { api.unregisterToken(any()) } returns NetworkResult.Success(Unit)

        val result = repository.unregisterToken("fcm-abc")

        assertTrue(result.isSuccess)
    }

    @Test
    fun `given http error when registerToken then returns failure`() = runTest {
        coEvery { api.registerToken(any()) } returns NetworkResult.HttpError(403, "Forbidden")

        val result = repository.registerToken("fcm-abc", null)

        assertTrue(result.isFailure)
    }

    @Test
    fun `given network error when unregisterToken then returns failure`() = runTest {
        coEvery { api.unregisterToken(any()) } returns NetworkResult.NetworkError(IOException())

        val result = repository.unregisterToken("fcm-abc")

        assertTrue(result.isFailure)
    }
}
