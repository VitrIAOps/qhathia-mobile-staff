package com.qhathia.mobile.staff.core.data.mapper

import com.qhathia.mobile.staff.core.testing.TestFixtures
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ProductMappersTest {

    @Test
    fun `toCreateRequestDto maps fields but omits quantity`() {
        val product = TestFixtures.newProduct(
            name = "Lámpara",
            price = 89.9,
            quantity = 25,
            description = "desc",
            categoryId = "c1",
            tags = listOf("vintage", "oro"),
        )

        val dto = product.toCreateRequestDto()

        assertEquals("Lámpara", dto.name)
        assertEquals(89.9, dto.price)
        assertEquals("desc", dto.description)
        assertEquals("c1", dto.categoryId)
        assertEquals(listOf("vintage", "oro"), dto.tags)
        assertEquals("AVAILABLE", dto.availability)
        // quantity has no field on the create DTO yet — the value (25) is intentionally not sent.
    }
}
