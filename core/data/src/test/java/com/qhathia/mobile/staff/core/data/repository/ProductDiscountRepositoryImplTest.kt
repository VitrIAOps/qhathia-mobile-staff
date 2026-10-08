package com.qhathia.mobile.staff.core.data.repository

import com.qhathia.mobile.staff.core.domain.model.Discount
import com.qhathia.mobile.staff.core.domain.model.DiscountError
import com.qhathia.mobile.staff.core.domain.model.DiscountException
import com.qhathia.mobile.staff.core.domain.model.DiscountType
import com.qhathia.mobile.staff.core.network.NetworkResult
import com.qhathia.mobile.staff.core.network.dto.CreateDiscountRequestDto
import com.qhathia.mobile.staff.core.network.dto.DiscountDto
import com.qhathia.mobile.staff.core.network.service.ProductApiService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.IOException
import java.time.Instant

class ProductDiscountRepositoryImplTest {

    private val api = mockk<ProductApiService>()
    private val repository = ProductDiscountRepositoryImpl(api)

    private val startsAt = Instant.parse("2026-06-29T20:00:00Z")
    private val endsAt = Instant.parse("2026-07-06T20:00:00Z")

    private fun dto(
        id: String = "d1",
        discountType: String = "PERCENTAGE",
        value: Double = 15.0,
        endsAt: String = "2026-07-06T20:00:00Z",
    ) = DiscountDto(
        id = id,
        discountType = discountType,
        value = value,
        startsAt = "2026-06-29T20:00:00Z",
        endsAt = endsAt,
        active = true,
    )

    private fun Result<*>.discountError(): DiscountError? =
        (exceptionOrNull() as? DiscountException)?.error

    @Test
    fun `given success when getDiscounts then maps dtos to domain`() = runTest {
        coEvery { api.getDiscounts("p1") } returns NetworkResult.Success(
            listOf(dto(), dto(id = "d2", discountType = "FIXED_AMOUNT", value = 10.5)),
        )

        val discounts = repository.getDiscounts("p1").getOrThrow()

        assertEquals(2, discounts.size)
        assertEquals(Discount(DiscountType.PERCENTAGE, 15.0), discounts[0].discount)
        assertEquals(startsAt, discounts[0].startsAt)
        assertEquals(endsAt, discounts[0].endsAt)
        assertEquals(Discount(DiscountType.FIXED_AMOUNT, 10.5), discounts[1].discount)
    }

    @Test
    fun `given rows it cannot understand when getDiscounts then they are skipped`() = runTest {
        coEvery { api.getDiscounts("p1") } returns NetworkResult.Success(
            listOf(dto(id = "odd", discountType = "BUY_ONE_GET_ONE"), dto(id = "bad", endsAt = "soon"), dto()),
        )

        assertEquals(listOf("d1"), repository.getDiscounts("p1").getOrThrow().map { it.id })
    }

    @Test
    fun `given http errors when getDiscounts then maps the status to a discount error`() = runTest {
        coEvery { api.getDiscounts("p1") } returns NetworkResult.HttpError(404, "Not Found")
        assertEquals(DiscountError.NotFound, repository.getDiscounts("p1").discountError())

        coEvery { api.getDiscounts("p1") } returns NetworkResult.HttpError(500, "Server Error")
        assertTrue(repository.getDiscounts("p1").discountError() is DiscountError.Unknown)
    }

    @Test
    fun `given a transport failure when getDiscounts then fails with network`() = runTest {
        coEvery { api.getDiscounts("p1") } returns NetworkResult.NetworkError(IOException("offline"))

        assertEquals(DiscountError.Network, repository.getDiscounts("p1").discountError())
    }

    @Test
    fun `when createDiscount then sends type, value and the window as ISO instants`() = runTest {
        val body = slot<CreateDiscountRequestDto>()
        coEvery { api.createDiscount("p1", capture(body)) } returns
            NetworkResult.Success(dto(id = "new", discountType = "FIXED_AMOUNT", value = 20.0))

        val created = repository
            .createDiscount("p1", Discount(DiscountType.FIXED_AMOUNT, 20.0), startsAt, endsAt)
            .getOrThrow()

        assertEquals(
            CreateDiscountRequestDto("FIXED_AMOUNT", 20.0, "2026-06-29T20:00:00Z", "2026-07-06T20:00:00Z"),
            body.captured,
        )
        assertEquals("new", created.id)
    }

    @Test
    fun `given a rejected body when createDiscount then fails with invalid input`() = runTest {
        coEvery { api.createDiscount(any(), any()) } returns NetworkResult.HttpError(422, "Unprocessable")

        val result = repository.createDiscount("p1", Discount(DiscountType.PERCENTAGE, 20.0), startsAt, endsAt)

        assertEquals(DiscountError.InvalidInput, result.discountError())
    }

    @Test
    fun `when deleteDiscount then calls the api and maps failures`() = runTest {
        coEvery { api.deleteDiscount("p1", "d1") } returns NetworkResult.Success(Unit)
        assertTrue(repository.deleteDiscount("p1", "d1").isSuccess)
        coVerify(exactly = 1) { api.deleteDiscount("p1", "d1") }

        coEvery { api.deleteDiscount("p1", "d1") } returns NetworkResult.NetworkError(IOException("offline"))
        assertEquals(DiscountError.Network, repository.deleteDiscount("p1", "d1").discountError())
    }
}
