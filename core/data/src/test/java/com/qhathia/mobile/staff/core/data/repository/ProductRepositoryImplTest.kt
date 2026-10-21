package com.qhathia.mobile.staff.core.data.repository

import com.qhathia.mobile.staff.core.data.local.CompressedImage
import com.qhathia.mobile.staff.core.data.local.ImageCompressor
import com.qhathia.mobile.staff.core.domain.model.Availability
import com.qhathia.mobile.staff.core.domain.model.RegisterProductError
import com.qhathia.mobile.staff.core.domain.model.RegisterProductException
import com.qhathia.mobile.staff.core.domain.model.UpdateProductError
import com.qhathia.mobile.staff.core.domain.model.UpdateProductException
import com.qhathia.mobile.staff.core.network.NetworkResult
import com.qhathia.mobile.staff.core.network.dto.CreateProductRequestDto
import com.qhathia.mobile.staff.core.network.dto.MultipartImage
import com.qhathia.mobile.staff.core.network.dto.ProductDetailDto
import com.qhathia.mobile.staff.core.network.dto.ProductDto
import com.qhathia.mobile.staff.core.network.dto.ProductMediaDto
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

    // ---- Detail / update ----

    private fun productDetailDto() = ProductDetailDto(
        id = "p1",
        categoryId = "c1",
        categoryName = "Electrónica",
        name = "Taladro",
        description = "Percutor 650W",
        price = 189.0,
        stock = 5,
        tags = listOf("bosch"),
        media = listOf(ProductMediaDto(id = "m1", url = "http://img/p1.jpg", primary = true)),
    )

    @Test
    fun `given success when getProduct then maps the dto to domain`() = runTest {
        coEvery { api.getProduct("p1") } returns NetworkResult.Success(productDetailDto())

        val result = repository.getProduct("p1")

        assertTrue(result.isSuccess)
        val detail = result.getOrThrow()
        assertEquals("p1", detail.id)
        assertEquals("Percutor 650W", detail.description)
        assertEquals(5, detail.stock)
        assertEquals(listOf("http://img/p1.jpg"), detail.images.map { it.url })
    }

    @Test
    fun `given http 404 when getProduct then fails with NotFound`() = runTest {
        coEvery { api.getProduct("p1") } returns NetworkResult.HttpError(404, "not found")

        val error = (repository.getProduct("p1").exceptionOrNull() as UpdateProductException).error

        assertEquals(UpdateProductError.NotFound, error)
    }

    @Test
    fun `given network error when getProduct then fails with Network`() = runTest {
        coEvery { api.getProduct("p1") } returns NetworkResult.NetworkError(IOException("offline"))

        val error = (repository.getProduct("p1").exceptionOrNull() as UpdateProductException).error

        assertEquals(UpdateProductError.Network, error)
    }

    @Test
    fun `given success when updateProduct then sends the full replace body`() = runTest {
        val body = slot<CreateProductRequestDto>()
        coEvery { api.updateProduct("p1", capture(body)) } returns
            NetworkResult.Success(productDetailDto())

        val result = repository.updateProduct(
            "p1",
            TestFixtures.productUpdate(name = "Taladro Pro", stock = 9, position = 3),
        )

        assertTrue(result.isSuccess)
        assertEquals("Taladro Pro", body.captured.name)
        assertEquals(9, body.captured.stock)
        assertEquals(3, body.captured.position)
        coVerify(exactly = 1) { api.updateProduct("p1", any()) }
    }

    @Test
    fun `given http 422 when updateProduct then fails with InvalidInput`() = runTest {
        coEvery { api.updateProduct(any(), any()) } returns NetworkResult.HttpError(422, "invalid")

        val result = repository.updateProduct("p1", TestFixtures.productUpdate())

        val error = (result.exceptionOrNull() as UpdateProductException).error
        assertEquals(UpdateProductError.InvalidInput, error)
    }

    @Test
    fun `given an unmapped http code when updateProduct then fails with Unknown`() = runTest {
        coEvery { api.updateProduct(any(), any()) } returns NetworkResult.HttpError(500, "boom")

        val result = repository.updateProduct("p1", TestFixtures.productUpdate())

        val error = (result.exceptionOrNull() as UpdateProductException).error
        assertEquals(UpdateProductError.Unknown("HTTP 500"), error)
    }

    // ---- Photos ----

    @Test
    fun `given success when addProductImages then compresses and uploads every image in one request`() =
        runTest {
            compressionSucceeds()
            val parts = slot<List<MultipartImage>>()
            coEvery { api.addProductImages("p1", capture(parts)) } returns NetworkResult.Success(Unit)

            val result = repository.addProductImages(
                "p1",
                listOf(TestFixtures.selectedImage(), TestFixtures.selectedImage(uri = "content://2")),
            )

            assertTrue(result.isSuccess)
            assertEquals(2, parts.captured.size)
        }

    @Test
    fun `given compression failure when addProductImages then fails without calling the api`() =
        runTest {
            every { imageCompressor.compress(any()) } returns Result.failure(IllegalStateException("bad"))

            val result = repository.addProductImages("p1", listOf(TestFixtures.selectedImage()))

            assertTrue(result.exceptionOrNull() is UpdateProductException)
            coVerify(exactly = 0) { api.addProductImages(any(), any()) }
        }

    @Test
    fun `given http 400 when addProductImages then fails with InvalidInput`() = runTest {
        compressionSucceeds()
        coEvery { api.addProductImages(any(), any()) } returns NetworkResult.HttpError(400, "limit")

        val result = repository.addProductImages("p1", listOf(TestFixtures.selectedImage()))

        assertEquals(UpdateProductError.InvalidInput, (result.exceptionOrNull() as UpdateProductException).error)
    }

    @Test
    fun `given success when deleteProductImage then succeeds`() = runTest {
        coEvery { api.deleteProductMedia("p1", "m1") } returns NetworkResult.Success(Unit)

        assertTrue(repository.deleteProductImage("p1", "m1").isSuccess)
    }

    @Test
    fun `given network error when deleteProductImage then fails with Network`() = runTest {
        coEvery { api.deleteProductMedia(any(), any()) } returns NetworkResult.NetworkError(IOException("off"))

        val result = repository.deleteProductImage("p1", "m1")

        assertEquals(UpdateProductError.Network, (result.exceptionOrNull() as UpdateProductException).error)
    }
}
