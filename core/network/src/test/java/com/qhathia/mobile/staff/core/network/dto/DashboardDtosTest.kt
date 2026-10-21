package com.qhathia.mobile.staff.core.network.dto

import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class DashboardDtosTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `an activity response carries confirmed orders and their variation`() {
        val dto = json.decodeFromString<StoreActivityDto>(
            """
            {
              "granularity": "WEEKLY",
              "uniqueSessions": 72, "uniqueSessionsDeltaPercent": -12.5,
              "productViews": 264, "productViewsDeltaPercent": 8.1,
              "confirmedOrders": 9, "confirmedOrdersDeltaPercent": 12.5
            }
            """.trimIndent(),
        )

        assertEquals(9, dto.confirmedOrders)
        assertEquals(12.5, dto.confirmedOrdersDeltaPercent)
    }

    @Test
    fun `an activity response from an older backend has no confirmed orders`() {
        val dto = json.decodeFromString<StoreActivityDto>("""{ "uniqueSessions": 72, "productViews": 264 }""")

        assertNull(dto.confirmedOrders)
        assertNull(dto.confirmedOrdersDeltaPercent)
    }

    @Test
    fun `a ranking response carries the total count of products without sales`() {
        val dto = json.decodeFromString<SalesRankingDto>(
            """
            {
              "needsAttention": {
                "totalImmobilized": 2990.00,
                "totalCount": 17,
                "items": [
                  { "productId": "p-1", "name": "Foco", "stock": 34, "price": 85.00, "immobilizedAmount": 2890.00 }
                ]
              }
            }
            """.trimIndent(),
        )

        assertEquals(17, dto.needsAttention.totalCount)
        assertEquals(1, dto.needsAttention.items.size)
    }

    @Test
    fun `a ranking response from an older backend has no total count`() {
        val dto = json.decodeFromString<SalesRankingDto>("""{ "needsAttention": { "totalImmobilized": 0.0 } }""")

        assertNull(dto.needsAttention.totalCount)
    }
}
