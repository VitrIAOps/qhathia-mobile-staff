package com.qhathia.mobile.staff.core.domain.usecase

import com.qhathia.mobile.staff.core.domain.model.PairDeviceError
import com.qhathia.mobile.staff.core.domain.model.PairDeviceException
import com.qhathia.mobile.staff.core.testing.FakeDeviceRepository
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class PairDeviceUseCaseTest {

    private lateinit var repository: FakeDeviceRepository
    private lateinit var useCase: PairDeviceUseCase

    @BeforeEach
    fun setUp() {
        repository = FakeDeviceRepository()
        useCase = PairDeviceUseCase(repository)
    }

    @Test
    fun `given valid code when invoked then pairs and forwards the code and name`() = runTest {
        val result = useCase("AB3CD", "Mostrador")

        assertTrue(result.isSuccess)
        assertEquals("AB3CD", repository.lastPairedCode)
        assertEquals("Mostrador", repository.lastPairName)
        assertEquals(1, repository.pairCallCount)
    }

    @Test
    fun `given pairing failure when invoked then propagates the error`() = runTest {
        repository.pairResult = Result.failure(PairDeviceException(PairDeviceError.InvalidCode))

        val result = useCase("BADXX", null)

        assertTrue(result.isFailure)
        val error = (result.exceptionOrNull() as PairDeviceException).error
        assertInstanceOf(PairDeviceError.InvalidCode::class.java, error)
    }
}
