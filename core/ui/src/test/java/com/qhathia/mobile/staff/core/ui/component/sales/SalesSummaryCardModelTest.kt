package com.qhathia.mobile.staff.core.ui.component.sales

import kotlinx.collections.immutable.persistentListOf
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class SalesSummaryCardModelTest {

    private fun model(highlighted: Int, vararg amounts: Int) = SalesSummaryCardModel(
        period = SalesPeriodUi.entries.first(),
        total = BigDecimal(amounts.last()),
        deltaPercent = null,
        buckets = amounts.mapIndexed { i, a -> SalesBucketModel("Sem ${42 + i}", BigDecimal(a)) }
            .let { persistentListOf<SalesBucketModel>().addAll(it) },
        highlightedBucketIndex = highlighted,
        highlightedDeltaPercent = null,
    )

    @Test
    fun `the emphasised bar is the current one even when another week was best`() {
        val m = model(highlighted = 2, 400, 900, 2855, 986)
        assertEquals(3, m.currentBucketIndex)
        assertEquals("Sem 44", m.highlightedBucket?.label)
    }

    @Test
    fun `when the current week is the best both point at it`() {
        val m = model(highlighted = 3, 400, 900, 500, 986)
        assertEquals(m.highlightedBucketIndex, m.currentBucketIndex)
    }
}
