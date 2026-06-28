package com.qhathia.mobile.staff.core.data.repository

import com.qhathia.mobile.staff.core.network.NetworkResult
import com.qhathia.mobile.staff.core.network.dto.CategoryDto
import com.qhathia.mobile.staff.core.network.service.CategoryApiService
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.IOException

class CategoryRepositoryImplTest {

    private val api = mockk<CategoryApiService>()
    private val repository = CategoryRepositoryImpl(api)

    @Test
    fun `given success when getCategories then maps dtos to domain`() = runTest {
        coEvery { api.getCategories() } returns
            NetworkResult.Success(listOf(CategoryDto(id = "c1", name = "Electrónica")))

        val category = repository.getCategories().getOrThrow().single()

        assertEquals("c1", category.id)
        assertEquals("Electrónica", category.name)
    }

    @Test
    fun `given http error when getCategories then fails`() = runTest {
        coEvery { api.getCategories() } returns NetworkResult.HttpError(500, "Server Error")

        assertTrue(repository.getCategories().isFailure)
    }

    @Test
    fun `given network error when getCategories then fails with cause`() = runTest {
        val cause = IOException("offline")
        coEvery { api.getCategories() } returns NetworkResult.NetworkError(cause)

        assertEquals(cause, repository.getCategories().exceptionOrNull())
    }
}
