package com.qhathia.mobile.staff.core.domain.usecase

import com.qhathia.mobile.staff.core.testing.FakeDeviceRepository
import com.qhathia.mobile.staff.core.testing.TestFixtures
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.IOException

class GetDevicesUseCaseTest {

    private lateinit var repository: FakeDeviceRepository
    private lateinit var useCase: GetDevicesUseCase

    @BeforeEach
    fun setUp() {
        repository = FakeDeviceRepository()
        useCase = GetDevicesUseCase(repository)
    }

    @Test
    fun `given devices when invoked then returns the list`() = runTest {
        val devices = listOf(TestFixtures.device(id = "d1"), TestFixtures.device(id = "d2"))
        repository.devicesResult = Result.success(devices)

        val result = useCase()

        assertTrue(result.isSuccess)
        assertEquals(devices, result.getOrNull())
        assertEquals(1, repository.getDevicesCallCount)
    }

    @Test
    fun `given failure when invoked then propagates it`() = runTest {
        repository.devicesResult = Result.failure(IOException("offline"))

        val result = useCase()

        assertTrue(result.isFailure)
    }
}
