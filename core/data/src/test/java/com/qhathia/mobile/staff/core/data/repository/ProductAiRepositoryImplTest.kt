package com.qhathia.mobile.staff.core.data.repository

import com.qhathia.mobile.staff.core.data.local.CompressedImage
import com.qhathia.mobile.staff.core.data.local.ImageCompressor
import com.qhathia.mobile.staff.core.domain.model.RegisterProductError
import com.qhathia.mobile.staff.core.domain.model.RegisterProductException
import com.qhathia.mobile.staff.core.network.NetworkResult
import com.qhathia.mobile.staff.core.network.dto.DraftFromImageResponseDto
import com.qhathia.mobile.staff.core.network.dto.MultipartImage
import com.qhathia.mobile.staff.core.network.dto.RegenerateDescriptionResponseDto
import com.qhathia.mobile.staff.core.network.service.ProductApiService
import com.qhathia.mobile.staff.core.testing.TestFixtures
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.IOException

class ProductAiRepositoryImplTest {

    private val api = mockk<ProductApiService>()
    private val imageCompressor = mockk<ImageCompressor>()
    private val repository = ProductAiRepositoryImpl(api, imageCompressor)

    private fun compressionSucceeds() {
        every { imageCompressor.compress(any()) } returns
            Result.success(CompressedImage(byteArrayOf(1, 2, 3), "image/jpeg"))
    }

    @Test
    fun `given success when draftFromImage then compresses image and maps to domain`() = runTest {
        compressionSucceeds()
        val imageSlot = slot<MultipartImage>()
        coEvery { api.draftFromImage(capture(imageSlot)) } returns NetworkResult.Success(
            DraftFromImageResponseDto(
                description = "Foco LED",
                categoryId = "c1",
                tags = listOf("foco-led", "e27"),
                source = "groq",
            ),
        )

        val draft = repository.draftFromImage(TestFixtures.selectedImage()).getOrThrow()

        assertEquals("Foco LED", draft.description)
        assertEquals("c1", draft.categoryId)
        assertEquals(listOf("foco-led", "e27"), draft.tags)
        assertEquals("image/jpeg", imageSlot.captured.contentType)
        coVerify(exactly = 1) { imageCompressor.compress(any()) }
    }

    @Test
    fun `given compression failure when draftFromImage then fails without calling the api`() =
        runTest {
            every { imageCompressor.compress(any()) } returns
                Result.failure(RegisterProductException(RegisterProductError.ImageTooLargeOrInvalid))

            val result = repository.draftFromImage(TestFixtures.selectedImage())

            assertTrue(result.isFailure)
            coVerify(exactly = 0) { api.draftFromImage(any()) }
        }

    @Test
    fun `given http error when draftFromImage then fails`() = runTest {
        compressionSucceeds()
        coEvery { api.draftFromImage(any()) } returns NetworkResult.HttpError(403, "Forbidden")

        assertTrue(repository.draftFromImage(TestFixtures.selectedImage()).isFailure)
    }

    @Test
    fun `given network error when draftFromImage then fails with cause`() = runTest {
        compressionSucceeds()
        val cause = IOException("offline")
        coEvery { api.draftFromImage(any()) } returns NetworkResult.NetworkError(cause)

        assertEquals(cause, repository.draftFromImage(TestFixtures.selectedImage()).exceptionOrNull())
    }

    @Test
    fun `given success when optimizeDescription then returns paraphrase`() = runTest {
        coEvery { api.regenerateDescription(any()) } returns
            NetworkResult.Success(RegenerateDescriptionResponseDto("Bombilla LED de 9W"))

        val result = repository.optimizeDescription("Foco LED 9W")

        assertEquals("Bombilla LED de 9W", result.getOrThrow())
    }

    @Test
    fun `given http error when optimizeDescription then fails`() = runTest {
        coEvery { api.regenerateDescription(any()) } returns NetworkResult.HttpError(422, "Invalid")

        assertTrue(repository.optimizeDescription("x").isFailure)
    }

    @Test
    fun `given network error when optimizeDescription then fails with cause`() = runTest {
        val cause = IOException("offline")
        coEvery { api.regenerateDescription(any()) } returns NetworkResult.NetworkError(cause)

        assertEquals(cause, repository.optimizeDescription("x").exceptionOrNull())
    }
}
