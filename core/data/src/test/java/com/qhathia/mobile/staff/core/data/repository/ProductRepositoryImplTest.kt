package com.qhathia.mobile.staff.core.data.repository

import com.qhathia.mobile.staff.core.data.local.CompressedImage
import com.qhathia.mobile.staff.core.data.local.ImageCompressor
import com.qhathia.mobile.staff.core.domain.model.Availability
import com.qhathia.mobile.staff.core.domain.model.RegisterProductError
import com.qhathia.mobile.staff.core.domain.model.RegisterProductException
import com.qhathia.mobile.staff.core.network.NetworkResult
import com.qhathia.mobile.staff.core.network.dto.CreateProductRequestDto
import com.qhathia.mobile.staff.core.network.dto.MultipartImage
import com.qhathia.mobile.staff.core.network.dto.ProductDto
import com.qhathia.mobile.staff.core.network.dto.ProductResponseDto
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

class ProductRepositoryImplTest {

    private val api = mockk<ProductApiService>()
    private val imageCompressor = mockk<ImageCompressor>()
    private val repository = ProductRepositoryImpl(api, imageCompressor)

    private fun productDto() = ProductDto(
        id = "p1",
        categoryId = "c1",
        categoryName = "Electrónica",
        name = "Taladro",
        price = 189.0,
        availability = "LOW_STOCK",
        primaryImageUrl = "http://img/p1.jpg",
    )

    private fun compressionSucceeds() {
        every { imageCompressor.compress(any()) } returns
            Result.success(CompressedImage(byteArrayOf(1, 2, 3), "image/jpeg"))
    }

    @Test
    fun `given success when getProducts then maps dtos to domain`() = runTest {
        coEvery { api.getProducts(any(), any()) } returns NetworkResult.Success(listOf(productDto()))

        val result = repository.getProducts(categoryId = null, availability = null)

        assertTrue(result.isSuccess)
        val product = result.getOrThrow().single()
        assertEquals("p1", product.id)
        assertEquals(Availability.LOW_STOCK, product.availability)
    }

    @Test
    fun `given filters when getProducts then forwards categoryId and availability name`() = runTest {
        coEvery { api.getProducts(any(), any()) } returns NetworkResult.Success(emptyList())

        repository.getProducts(categoryId = "c1", availability = Availability.AVAILABLE)

        coVerify { api.getProducts("c1", "AVAILABLE") }
    }

    @Test
    fun `given unknown availability when getProducts then defaults to CONSULT`() = runTest {
        coEvery { api.getProducts(any(), any()) } returns
            NetworkResult.Success(listOf(productDto().copy(availability = null)))

        val product = repository.getProducts(null, null).getOrThrow().single()

        assertEquals(Availability.CONSULT, product.availability)
    }

    @Test
    fun `given http error when getProducts then fails`() = runTest {
        coEvery { api.getProducts(any(), any()) } returns NetworkResult.HttpError(401, "Unauthorized")

        val result = repository.getProducts(null, null)

        assertTrue(result.isFailure)
    }

    @Test
    fun `given network error when getProducts then fails with cause`() = runTest {
        val cause = IOException("offline")
        coEvery { api.getProducts(any(), any()) } returns NetworkResult.NetworkError(cause)

        val result = repository.getProducts(null, null)

        assertEquals(cause, result.exceptionOrNull())
    }

    @Test
    fun `given success when registerProduct then sends stock, multipart images and returns id`() =
        runTest {
            compressionSucceeds()
            val productSlot = slot<CreateProductRequestDto>()
            val imagesSlot = slot<List<MultipartImage>>()
            coEvery {
                api.createProductWithImages(capture(productSlot), capture(imagesSlot))
            } returns NetworkResult.Success(ProductResponseDto("p1"))

            val result = repository.registerProduct(
                TestFixtures.newProduct(
                    quantity = 7,
                    images = listOf(TestFixtures.selectedImage(), TestFixtures.selectedImage()),
                ),
            )

            assertEquals("p1", result.getOrNull())
            assertEquals(7, productSlot.captured.stock)
            assertEquals(2, imagesSlot.captured.size)
            assertEquals("image_0.jpg", imagesSlot.captured[0].filename)
            assertEquals("image_1.jpg", imagesSlot.captured[1].filename)
        }

    @Test
    fun `given compression failure when registerProduct then fails without calling the api`() =
        runTest {
            every { imageCompressor.compress(any()) } returns
                Result.failure(RegisterProductException(RegisterProductError.ImageTooLargeOrInvalid))

            val result = repository.registerProduct(TestFixtures.newProduct())

            val error = (result.exceptionOrNull() as RegisterProductException).error
            assertEquals(RegisterProductError.ImageTooLargeOrInvalid, error)
            coVerify(exactly = 0) { api.createProductWithImages(any(), any()) }
        }

    @Test
    fun `given http 402 when registerProduct then fails with PlanLimitReached`() = runTest {
        assertRegisterHttpError(402, RegisterProductError.PlanLimitReached)
    }

    @Test
    fun `given http 403 when registerProduct then fails with PlanLimitReached`() = runTest {
        assertRegisterHttpError(403, RegisterProductError.PlanLimitReached)
    }

    @Test
    fun `given http 422 when registerProduct then fails with InvalidInput`() = runTest {
        assertRegisterHttpError(422, RegisterProductError.InvalidInput)
    }

    @Test
    fun `given http 400 when registerProduct then fails with InvalidInput`() = runTest {
        assertRegisterHttpError(400, RegisterProductError.InvalidInput)
    }

    @Test
    fun `given http 503 when registerProduct then fails with Network`() = runTest {
        assertRegisterHttpError(503, RegisterProductError.Network)
    }

    @Test
    fun `given network error when registerProduct then fails with Network`() = runTest {
        compressionSucceeds()
        coEvery { api.createProductWithImages(any(), any()) } returns
            NetworkResult.NetworkError(IOException("offline"))

        val result = repository.registerProduct(TestFixtures.newProduct())

        val error = (result.exceptionOrNull() as RegisterProductException).error
        assertEquals(RegisterProductError.Network, error)
    }

    private suspend fun assertRegisterHttpError(code: Int, expected: RegisterProductError) {
        compressionSucceeds()
        coEvery { api.createProductWithImages(any(), any()) } returns
            NetworkResult.HttpError(code, "error")

        val result = repository.registerProduct(TestFixtures.newProduct())

        val error = (result.exceptionOrNull() as RegisterProductException).error
        assertEquals(expected, error)
    }
}
