package com.qhathia.mobile.staff.feature.salesstatistics.ui

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class NeedsAttentionCaptionTest {

    @Test
    fun `without a total count the caption covers the listed products`() {
        assertEquals(NeedsAttentionCaption.All(3), needsAttentionCaption(listed = 3, totalCount = null))
    }

    @Test
    fun `a total equal to the list covers the listed products`() {
        assertEquals(NeedsAttentionCaption.All(3), needsAttentionCaption(listed = 3, totalCount = 3))
    }

    @Test
    fun `a larger total says how many of the total are listed`() {
        assertEquals(NeedsAttentionCaption.Partial(3, 17), needsAttentionCaption(listed = 3, totalCount = 17))
    }

    @Test
    fun `an inconsistent smaller total never claims more than is listed`() {
        assertEquals(NeedsAttentionCaption.All(3), needsAttentionCaption(listed = 3, totalCount = 2))
    }
}
