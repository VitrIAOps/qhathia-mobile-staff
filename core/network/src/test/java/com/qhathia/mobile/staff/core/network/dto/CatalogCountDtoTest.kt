package com.qhathia.mobile.staff.core.network.dto

import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class CatalogCountDtoTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `a Fase 42 count carries the visible products and why the rest are hidden`() {
        val dto = json.decodeFromString<CatalogCountDto>(
            """
            { "activeProducts": 58, "visibleProducts": 52,
              "hidden": { "outOfStock": 3, "lowStock": 1, "consult": 2, "noActiveCategory": 0 } }
            """.trimIndent(),
        )

        assertEquals(58, dto.activeProducts)
        assertEquals(52, dto.visibleProducts)
        assertEquals(CatalogHiddenDto(outOfStock = 3, lowStock = 1, consult = 2, noActiveCategory = 0), dto.hidden)
    }

    @Test
    fun `an older count has only the active products`() {
        val dto = json.decodeFromString<CatalogCountDto>("""{ "activeProducts": 50 }""")

        assertEquals(50, dto.activeProducts)
        assertNull(dto.visibleProducts)
        assertNull(dto.hidden)
    }
}
