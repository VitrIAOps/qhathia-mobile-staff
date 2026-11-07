package com.qhathia.mobile.staff.core.network.dto

import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class OrderDtosTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `an item decodes its list price, active flag and current name`() {
        val item = json.decodeFromString(
            OrderItemDto.serializer(),
            """{"id":"i1","productId":"p1","productName":"Taladro","unitPriceSnapshot":80.0,""" +
                """"listPriceSnapshot":100.0,"productActive":false,"currentProductName":"Taladro Pro"}""",
        )

        assertEquals(100.0, item.listPriceSnapshot)
        assertEquals(false, item.productActive)
        assertEquals("Taladro Pro", item.currentProductName)
    }

    @Test
    fun `an item from an older backend defaults the new fields`() {
        val item = json.decodeFromString(OrderItemDto.serializer(), """{"id":"i1","productId":"p1"}""")

        assertNull(item.listPriceSnapshot)
        assertEquals(true, item.productActive)
        assertNull(item.currentProductName)
    }

    @Test
    fun `a problem decodes the not-sellable reason`() {
        val problem = json.decodeFromString(
            OrderProblemDto.serializer(),
            """{"errorCode":"PRODUCT_NOT_SELLABLE","productName":"Taladro","reason":"INACTIVE"}""",
        )

        assertEquals("INACTIVE", problem.reason)
    }
}
