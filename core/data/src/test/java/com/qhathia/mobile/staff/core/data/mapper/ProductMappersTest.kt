package com.qhathia.mobile.staff.core.data.mapper

import com.qhathia.mobile.staff.core.testing.TestFixtures
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ProductMappersTest {

    @Test
    fun `toCreateRequestDto maps quantity to stock and sets no availability`() {
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
        // quantity is sent as stock; availability is not a field on the DTO (backend derives it).
        assertEquals(25, dto.stock)
    }
}
