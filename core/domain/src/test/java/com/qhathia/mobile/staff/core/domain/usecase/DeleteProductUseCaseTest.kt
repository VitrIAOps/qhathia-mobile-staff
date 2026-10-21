package com.qhathia.mobile.staff.core.domain.usecase

import com.qhathia.mobile.staff.core.domain.model.UpdateProductError
import com.qhathia.mobile.staff.core.domain.model.UpdateProductException
import com.qhathia.mobile.staff.core.testing.FakeProductRepository
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class DeleteProductUseCaseTest {

    private lateinit var productRepository: FakeProductRepository
    private lateinit var useCase: DeleteProductUseCase

    @BeforeEach
    fun setUp() {
        productRepository = FakeProductRepository()
        useCase = DeleteProductUseCase(productRepository)
    }

    @Test
    fun `deletes the product through the repository`() = runTest {
        val result = useCase("p1")

        assertTrue(result.isSuccess)
        assertEquals(listOf("p1"), productRepository.deletedIds)
    }

    @Test
    fun `keeps a classified failure as it is`() = runTest {
        productRepository.deleteResult =
            Result.failure(UpdateProductException(UpdateProductError.NotFound))

        val error = useCase("p1").exceptionOrNull()

        assertEquals(UpdateProductError.NotFound, (error as UpdateProductException).error)
    }

    @Test
    fun `wraps any other failure as Unknown`() = runTest {
        productRepository.deleteResult = Result.failure(IllegalStateException("boom"))

        val error = useCase("p1").exceptionOrNull()

        assertEquals(UpdateProductError.Unknown("boom"), (error as UpdateProductException).error)
    }
}
