package com.qhathia.mobile.staff.core.data.repository

import com.qhathia.mobile.staff.core.domain.model.Availability
import com.qhathia.mobile.staff.core.network.NetworkResult
import com.qhathia.mobile.staff.core.network.dto.ProductDto
import com.qhathia.mobile.staff.core.network.service.ProductApiService
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
}
