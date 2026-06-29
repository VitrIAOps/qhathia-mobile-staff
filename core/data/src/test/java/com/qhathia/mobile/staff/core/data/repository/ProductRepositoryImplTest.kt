package com.qhathia.mobile.staff.core.data.repository

import com.qhathia.mobile.staff.core.domain.model.Availability
import com.qhathia.mobile.staff.core.domain.model.RegisterProductError
import com.qhathia.mobile.staff.core.domain.model.RegisterProductException
import com.qhathia.mobile.staff.core.network.NetworkResult
import com.qhathia.mobile.staff.core.network.dto.MediaResponseDto
import com.qhathia.mobile.staff.core.network.dto.ProductDto
import com.qhathia.mobile.staff.core.network.dto.ProductResponseDto
import com.qhathia.mobile.staff.core.network.service.ProductApiService
import com.qhathia.mobile.staff.core.testing.TestFixtures
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.IOException

class ProductRepositoryImplTest {

    private val api = mockk<ProductApiService>()
    private val repository = ProductRepositoryImpl(api)

    private fun productDto() = ProductDto(
        id = "p1",
        categoryId = "c1",
        categoryName = "Electrónica",
        name = "Taladro",
        price = 189.0,
        availability = "LOW_STOCK",
        primaryImageUrl = "http://img/p1.jpg",
    )

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
    fun `given success when createProduct then returns product id`() = runTest {
        coEvery { api.createProduct(any()) } returns NetworkResult.Success(ProductResponseDto("p1"))

        val result = repository.createProduct(TestFixtures.newProduct())

        assertEquals("p1", result.getOrNull())
    }

    @Test
    fun `given http 403 when createProduct then fails with PlanLimitReached`() = runTest {
        coEvery { api.createProduct(any()) } returns NetworkResult.HttpError(403, "Forbidden")

        val result = repository.createProduct(TestFixtures.newProduct())

        val error = (result.exceptionOrNull() as RegisterProductException).error
        assertEquals(RegisterProductError.PlanLimitReached, error)
    }

    @Test
    fun `given network error when createProduct then fails with Network`() = runTest {
        coEvery { api.createProduct(any()) } returns NetworkResult.NetworkError(IOException("offline"))

        val result = repository.createProduct(TestFixtures.newProduct())

        val error = (result.exceptionOrNull() as RegisterProductException).error
        assertEquals(RegisterProductError.Network, error)
    }

    @Test
    fun `given success when attachMedia then succeeds`() = runTest {
        coEvery { api.attachMedia(any(), any()) } returns NetworkResult.Success(MediaResponseDto("m1"))

        val result = repository.attachMedia("p1", "https://cdn/x.jpg", position = 0, primary = true)

        assertTrue(result.isSuccess)
    }

    @Test
    fun `given success when deactivateProduct then succeeds`() = runTest {
        coEvery { api.deactivateProduct(any()) } returns NetworkResult.Success(Unit)

        val result = repository.deactivateProduct("p1")

        assertTrue(result.isSuccess)
    }
}
