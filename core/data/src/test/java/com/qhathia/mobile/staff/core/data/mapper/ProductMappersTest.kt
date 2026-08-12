package com.qhathia.mobile.staff.core.data.mapper

import com.qhathia.mobile.staff.core.network.dto.ProductDetailDto
import com.qhathia.mobile.staff.core.network.dto.ProductMediaDto
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

    @Test
    fun `toUpdateRequestDto carries the non-editable fields through`() {
        val product = TestFixtures.productUpdate(
            name = "Lámpara",
            price = 89.9,
            stock = 4,
            allowDiscountInquiry = true,
            position = 7,
        )

        val dto = product.toUpdateRequestDto()

        assertEquals("Lámpara", dto.name)
        assertEquals(89.9, dto.price)
        assertEquals(4, dto.stock)
        // PUT is a full replace, so these must survive the round trip rather than reset to defaults.
        assertEquals(true, dto.allowDiscountInquiry)
        assertEquals(7, dto.position)
    }

    @Test
    fun `ProductDetailDto toDomain orders media with the primary photo first`() {
        val dto = ProductDetailDto(
            id = "p1",
            name = "Taladro",
            price = 189.0,
            stock = 3,
            tags = listOf("bosch"),
            media = listOf(
                ProductMediaDto(id = "m1", url = "second.jpg", position = 1),
                ProductMediaDto(id = "m2", url = "third.jpg", position = 2),
                ProductMediaDto(id = "m3", url = "primary.jpg", position = 9, primary = true),
            ),
        )

        val detail = dto.toDomain()

        assertEquals(listOf("primary.jpg", "second.jpg", "third.jpg"), detail.imageUrls)
        assertEquals(3, detail.stock)
        assertEquals(listOf("bosch"), detail.tags)
    }

    @Test
    fun `ProductDetailDto toDomain drops non-image media`() {
        val dto = ProductDetailDto(
            id = "p1",
            name = "Taladro",
            media = listOf(
                ProductMediaDto(id = "m1", url = "clip.mp4", mediaType = "VIDEO"),
                ProductMediaDto(id = "m2", url = "photo.jpg"),
            ),
        )

        assertEquals(listOf("photo.jpg"), dto.toDomain().imageUrls)
    }
}
