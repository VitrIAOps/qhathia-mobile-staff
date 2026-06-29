package com.qhathia.mobile.staff.core.domain.usecase

import com.qhathia.mobile.staff.core.domain.model.RegisterProductError
import com.qhathia.mobile.staff.core.domain.model.RegisterProductException
import com.qhathia.mobile.staff.core.domain.model.SelectedImage
import com.qhathia.mobile.staff.core.testing.FakeMediaStorageRepository
import com.qhathia.mobile.staff.core.testing.FakeProductRepository
import com.qhathia.mobile.staff.core.testing.TestFixtures
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class RegisterProductUseCaseTest {

    private lateinit var productRepository: FakeProductRepository
    private lateinit var mediaRepository: FakeMediaStorageRepository
    private lateinit var useCase: RegisterProductUseCase

    @BeforeEach
    fun setUp() {
        productRepository = FakeProductRepository()
        mediaRepository = FakeMediaStorageRepository()
        useCase = RegisterProductUseCase(productRepository, mediaRepository)
    }

    @Test
    fun `happy path creates product, uploads images and attaches first as primary`() = runTest {
        productRepository.createResult = Result.success("p1")
        mediaRepository.uploadResult = Result.success("https://cdn/x.jpg")
        val product = TestFixtures.newProduct(
            images = listOf(
                SelectedImage("content://1", "image/jpeg"),
                SelectedImage("content://2", "image/png"),
            ),
        )

        val result = useCase(product)

        assertTrue(result.isSuccess)
        assertEquals("p1", result.getOrNull())
        assertEquals(2, mediaRepository.uploadCount)
        assertEquals(2, productRepository.attachedMedia.size)
        assertEquals(true, productRepository.attachedMedia[0].primary)
        assertEquals(0, productRepository.attachedMedia[0].position)
        assertEquals(false, productRepository.attachedMedia[1].primary)
        assertNull(productRepository.deactivatedProductId)
    }

    @Test
    fun `plan limit on create returns PlanLimitReached without uploading`() = runTest {
        productRepository.createResult =
            Result.failure(RegisterProductException(RegisterProductError.PlanLimitReached))

        val result = useCase(TestFixtures.newProduct())

        assertTrue(result.isFailure)
        val error = (result.exceptionOrNull() as RegisterProductException).error
        assertEquals(RegisterProductError.PlanLimitReached, error)
        assertEquals(0, mediaRepository.uploadCount)
    }

    @Test
    fun `media failure after create rolls back the product`() = runTest {
        productRepository.createResult = Result.success("p1")
        mediaRepository.uploadResult =
            Result.failure(RegisterProductException(RegisterProductError.Network))

        val result = useCase(TestFixtures.newProduct())

        assertTrue(result.isFailure)
        assertEquals("p1", productRepository.deactivatedProductId)
    }

    @Test
    fun `attach failure after create rolls back the product`() = runTest {
        productRepository.createResult = Result.success("p1")
        mediaRepository.uploadResult = Result.success("https://cdn/x.jpg")
        productRepository.attachResult =
            Result.failure(RegisterProductException(RegisterProductError.Network))

        val result = useCase(TestFixtures.newProduct())

        assertTrue(result.isFailure)
        assertEquals("p1", productRepository.deactivatedProductId)
    }
}
