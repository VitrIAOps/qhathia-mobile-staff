package com.qhathia.mobile.staff.core.domain.usecase

import com.qhathia.mobile.staff.core.domain.model.UpdateProductError
import com.qhathia.mobile.staff.core.domain.model.UpdateProductException
import com.qhathia.mobile.staff.core.testing.FakeProductRepository
import com.qhathia.mobile.staff.core.testing.TestFixtures
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class UpdateProductPhotosUseCaseTest {

    private lateinit var productRepository: FakeProductRepository
    private lateinit var useCase: UpdateProductPhotosUseCase

    @BeforeEach
    fun setUp() {
        productRepository = FakeProductRepository()
        useCase = UpdateProductPhotosUseCase(productRepository)
    }

    @Test
    fun `deletes removed photos first, then uploads the added ones in one request`() = runTest {
        val added = listOf(TestFixtures.selectedImage(), TestFixtures.selectedImage(uri = "content://2"))

        val result = useCase("p1", removedImageIds = listOf("m1", "m2"), addedImages = added)

        assertTrue(result.isSuccess)
        assertEquals(listOf("deleteImage:m1", "deleteImage:m2", "addImages:2"), productRepository.writeLog)
    }

    @Test
    fun `sends nothing when there is nothing to change`() = runTest {
        val result = useCase("p1", removedImageIds = emptyList(), addedImages = emptyList())

        assertTrue(result.isSuccess)
        assertTrue(productRepository.writeLog.isEmpty())
    }

    @Test
    fun `stops at a failed delete without uploading`() = runTest {
        productRepository.deleteImageResult = Result.failure(UpdateProductException(UpdateProductError.Network))

        val result = useCase("p1", listOf("m1", "m2"), listOf(TestFixtures.selectedImage()))

        assertEquals(UpdateProductError.Network, (result.exceptionOrNull() as UpdateProductException).error)
        assertEquals(listOf("deleteImage:m1"), productRepository.writeLog)
    }

    @Test
    fun `wraps an unclassified upload failure as Unknown`() = runTest {
        productRepository.addImagesResult = Result.failure(IllegalStateException("boom"))

        val result = useCase("p1", emptyList(), listOf(TestFixtures.selectedImage()))

        assertEquals(UpdateProductError.Unknown("boom"), (result.exceptionOrNull() as UpdateProductException).error)
    }

    @Test
    fun `a photo already gone counts as deleted`() = runTest {
        productRepository.deleteImageResult = Result.failure(UpdateProductException(UpdateProductError.NotFound))

        val result = useCase("p1", listOf("m1"), listOf(TestFixtures.selectedImage()))

        assertTrue(result.isSuccess)
        assertEquals(listOf("deleteImage:m1", "addImages:1"), productRepository.writeLog)
    }
}
