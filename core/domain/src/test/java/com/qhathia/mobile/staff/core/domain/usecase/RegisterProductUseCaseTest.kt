package com.qhathia.mobile.staff.core.domain.usecase

import com.qhathia.mobile.staff.core.domain.model.RegisterProductError
import com.qhathia.mobile.staff.core.domain.model.RegisterProductException
import com.qhathia.mobile.staff.core.testing.FakeProductRepository
import com.qhathia.mobile.staff.core.testing.TestFixtures
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class RegisterProductUseCaseTest {

    private lateinit var productRepository: FakeProductRepository
    private lateinit var useCase: RegisterProductUseCase

    @BeforeEach
    fun setUp() {
        productRepository = FakeProductRepository()
        useCase = RegisterProductUseCase(productRepository)
    }

    @Test
    fun `delegates to the repository and returns the created product id`() = runTest {
        productRepository.registerResult = Result.success("p1")
        val product = TestFixtures.newProduct()

        val result = useCase(product)

        assertTrue(result.isSuccess)
        assertEquals("p1", result.getOrNull())
        assertEquals(1, productRepository.registerCallCount)
        assertEquals(product, productRepository.registeredProduct)
    }

    @Test
    fun `propagates a RegisterProductException from the repository`() = runTest {
        productRepository.registerResult =
            Result.failure(RegisterProductException(RegisterProductError.PlanLimitReached))

        val result = useCase(TestFixtures.newProduct())

        assertTrue(result.isFailure)
        val error = (result.exceptionOrNull() as RegisterProductException).error
        assertEquals(RegisterProductError.PlanLimitReached, error)
    }

    @Test
    fun `normalizes a non-RegisterProductException failure to Unknown`() = runTest {
        productRepository.registerResult = Result.failure(IllegalStateException("boom"))

        val result = useCase(TestFixtures.newProduct())

        assertTrue(result.isFailure)
        val error = (result.exceptionOrNull() as RegisterProductException).error
        assertTrue(error is RegisterProductError.Unknown)
    }
}
