package com.qhathia.mobile.staff.core.domain.usecase

import com.qhathia.mobile.staff.core.domain.model.DiscountError
import com.qhathia.mobile.staff.core.domain.model.DiscountException
import com.qhathia.mobile.staff.core.domain.model.DiscountType
import com.qhathia.mobile.staff.core.testing.FakeProductDiscountRepository
import com.qhathia.mobile.staff.core.testing.MutableClock
import com.qhathia.mobile.staff.core.testing.TestFixtures
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Duration

class ProductDiscountUseCasesTest {

    private val repository = FakeProductDiscountRepository()
    private val clock = MutableClock()
    private val getDiscount = GetProductDiscountUseCase(repository, clock)
    private val saveDiscount = SaveProductDiscountUseCase(repository, clock)
    private val removeDiscount = RemoveProductDiscountUseCase(repository)

    private val networkFailure = DiscountException(DiscountError.Network)

    @Test
    fun `current discount is the active one that has not ended`() = runTest {
        repository.discounts += TestFixtures.productDiscount(id = "running")

        assertEquals("running", getDiscount("product-1").getOrThrow()?.id)
    }

    @Test
    fun `inactive or ended discounts do not count as current`() = runTest {
        repository.discounts += TestFixtures.productDiscount(id = "inactive", active = false)
        repository.discounts += TestFixtures.productDiscount(
            id = "ended",
            endsAt = TestFixtures.NOW.minusSeconds(1),
        )

        assertNull(getDiscount("product-1").getOrThrow())
    }

    @Test
    fun `no discounts means no current discount`() = runTest {
        assertNull(getDiscount("product-1").getOrThrow())
    }

    @Test
    fun `with several running discounts the one ending last wins`() = runTest {
        repository.discounts += TestFixtures.productDiscount(id = "short")
        repository.discounts += TestFixtures.productDiscount(
            id = "long",
            endsAt = TestFixtures.NOW.plus(Duration.ofDays(20)),
        )

        assertEquals("long", getDiscount("product-1").getOrThrow()?.id)
    }

    @Test
    fun `saving for a product without discounts only creates`() = runTest {
        saveDiscount("product-1", TestFixtures.discount())

        assertEquals(listOf("get:product-1", "create:product-1"), repository.calls)
    }

    @Test
    fun `saving deletes every existing discount before creating the new one`() = runTest {
        repository.discounts += TestFixtures.productDiscount(id = "old-1")
        repository.discounts += TestFixtures.productDiscount(id = "old-2", active = false)
        val discount = TestFixtures.discount(DiscountType.FIXED_AMOUNT, 15.0)

        val saved = saveDiscount("product-1", discount).getOrThrow()

        assertEquals(
            listOf("get:product-1", "delete:old-1", "delete:old-2", "create:product-1"),
            repository.calls,
        )
        assertEquals(discount, saved.discount)
        assertEquals(listOf(saved), repository.discounts)
    }

    @Test
    fun `a new discount starts now and ends one week later`() = runTest {
        val saved = saveDiscount("product-1", TestFixtures.discount()).getOrThrow()

        assertEquals(TestFixtures.NOW, saved.startsAt)
        assertEquals(TestFixtures.NOW.plus(Duration.ofDays(7)), saved.endsAt)
    }

    @Test
    fun `a failed delete stops the replace before creating`() = runTest {
        repository.discounts += TestFixtures.productDiscount(id = "old")
        repository.deleteFailure = networkFailure

        val result = saveDiscount("product-1", TestFixtures.discount())

        assertTrue(result.isFailure)
        assertEquals(listOf("get:product-1", "delete:old"), repository.calls)
    }

    @Test
    fun `a failed create after the delete leaves the product without a discount`() = runTest {
        repository.discounts += TestFixtures.productDiscount(id = "old")
        repository.createFailure = networkFailure

        val result = saveDiscount("product-1", TestFixtures.discount())

        assertEquals(networkFailure, result.exceptionOrNull())
        assertTrue(repository.discounts.isEmpty())
    }

    @Test
    fun `a failed listing fails the save without touching anything`() = runTest {
        repository.getFailure = networkFailure

        assertTrue(saveDiscount("product-1", TestFixtures.discount()).isFailure)
        assertEquals(listOf("get:product-1"), repository.calls)
    }

    @Test
    fun `removing deletes every discount of the product`() = runTest {
        repository.discounts += TestFixtures.productDiscount(id = "a")
        repository.discounts += TestFixtures.productDiscount(id = "b")

        assertTrue(removeDiscount("product-1").isSuccess)
        assertEquals(listOf("get:product-1", "delete:a", "delete:b"), repository.calls)
        assertTrue(repository.discounts.isEmpty())
    }

    @Test
    fun `a failed delete fails the removal`() = runTest {
        repository.discounts += TestFixtures.productDiscount(id = "a")
        repository.deleteFailure = networkFailure

        assertEquals(networkFailure, removeDiscount("product-1").exceptionOrNull())
        assertEquals(1, repository.discounts.size)
    }
}
