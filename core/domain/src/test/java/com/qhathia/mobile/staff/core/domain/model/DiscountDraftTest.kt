package com.qhathia.mobile.staff.core.domain.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DiscountDraftTest {

    private val price = 189.0

    @Test
    fun `percentage discount is taken off the price`() {
        assertEquals(151.2, Discount(DiscountType.PERCENTAGE, 20.0).applyTo(price))
    }

    @Test
    fun `fixed amount is subtracted from the price`() {
        assertEquals(174.0, Discount(DiscountType.FIXED_AMOUNT, 15.0).applyTo(price))
    }

    @Test
    fun `resulting price is rounded to cents and never negative`() {
        assertEquals(66.63, Discount(DiscountType.PERCENTAGE, 33.0).applyTo(99.45))
        assertEquals(0.0, Discount(DiscountType.FIXED_AMOUNT, 500.0).applyTo(price))
    }

    @Test
    fun `value text matches how the merchant types it`() {
        assertEquals("20", Discount(DiscountType.PERCENTAGE, 20.0).valueText())
        assertEquals("15", Discount(DiscountType.FIXED_AMOUNT, 15.0).valueText())
        assertEquals("15.50", Discount(DiscountType.FIXED_AMOUNT, 15.5).valueText())
    }

    @Test
    fun `a fixed amount stops fitting once the price drops below it`() {
        val discount = Discount(DiscountType.FIXED_AMOUNT, 50.0)
        assertTrue(discount.fits(50.0))
        assertFalse(discount.fits(49.99))
        assertTrue(Discount(DiscountType.PERCENTAGE, 100.0).fits(1.0))
    }

    @Test
    fun `an empty draft is in percentage mode, not valid and shows the original price`() {
        val draft = DiscountDraft(price)

        assertEquals(DiscountType.PERCENTAGE, draft.type)
        assertFalse(draft.isValid)
        assertNull(draft.error)
        assertEquals(price, draft.resultingPrice)
        assertNull(draft.toDiscount())
    }

    @Test
    fun `a custom percentage is valid and previews the resulting price`() {
        val draft = DiscountDraft(price).withInput("35")

        assertTrue(draft.isValid)
        assertEquals(122.85, draft.resultingPrice)
        assertEquals(Discount(DiscountType.PERCENTAGE, 35.0), draft.toDiscount())
    }

    @Test
    fun `one hundred percent is the limit and brings the price to zero`() {
        val full = DiscountDraft(price).withInput("100")
        val over = DiscountDraft(price).withInput("101")

        assertTrue(full.isValid)
        assertEquals(0.0, full.resultingPrice)
        assertFalse(over.isValid)
        assertEquals(DiscountInputError.InvalidPercentage, over.error)
        assertEquals(price, over.resultingPrice)
    }

    @Test
    fun `zero or a decimal percentage cannot be applied`() {
        val zero = DiscountDraft(price).withInput("0")
        val decimal = DiscountDraft(price).withInput("12.5")

        assertFalse(zero.isValid)
        assertNull(zero.error)
        assertFalse(decimal.isValid)
        assertEquals(DiscountInputError.InvalidPercentage, decimal.error)
    }

    @Test
    fun `a quick option fills the percentage input`() {
        val draft = DiscountDraft(price, DiscountType.FIXED_AMOUNT, "5").withQuickPercentage(50)

        assertEquals(DiscountType.PERCENTAGE, draft.type)
        assertEquals("50", draft.input)
        assertEquals(94.5, draft.resultingPrice)
        assertEquals(listOf(10, 20, 50), DiscountDraft.QUICK_PERCENTAGES)
    }

    @Test
    fun `an amount up to the price is valid and the price itself is the limit`() {
        val partial = DiscountDraft(price, DiscountType.FIXED_AMOUNT, "15,50")
        val whole = DiscountDraft(price, DiscountType.FIXED_AMOUNT, "189")
        val over = DiscountDraft(price, DiscountType.FIXED_AMOUNT, "189.01")

        assertEquals(173.5, partial.resultingPrice)
        assertTrue(whole.isValid)
        assertEquals(0.0, whole.resultingPrice)
        assertFalse(over.isValid)
        assertEquals(DiscountInputError.AmountAbovePrice, over.error)
    }

    @Test
    fun `an amount with more than two decimals or letters is rejected`() {
        assertEquals(
            DiscountInputError.InvalidAmount,
            DiscountDraft(price, DiscountType.FIXED_AMOUNT, "1.234").error,
        )
        assertEquals(
            DiscountInputError.InvalidAmount,
            DiscountDraft(price, DiscountType.FIXED_AMOUNT, "abc").error,
        )
    }

    @Test
    fun `switching mode clears the input and keeping the mode does not`() {
        val draft = DiscountDraft(price).withInput("20")

        val switched = draft.withType(DiscountType.FIXED_AMOUNT)
        assertEquals("", switched.input)
        assertEquals(price, switched.resultingPrice)
        assertEquals(draft, draft.withType(DiscountType.PERCENTAGE))
    }

    @Test
    fun `from prefills the editor with an existing discount`() {
        val draft = DiscountDraft.from(price, Discount(DiscountType.FIXED_AMOUNT, 15.5))

        assertEquals(DiscountType.FIXED_AMOUNT, draft.type)
        assertEquals("15.50", draft.input)
        assertTrue(draft.isValid)
        assertEquals(DiscountDraft(price), DiscountDraft.from(price, null))
    }
}
