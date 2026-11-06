package com.qhathia.mobile.staff.core.data.mapper

import com.qhathia.mobile.staff.core.network.dto.ProductDetailDto
import com.qhathia.mobile.staff.core.network.dto.ProductDto
import com.qhathia.mobile.staff.core.network.dto.ProductMediaDto
import com.qhathia.mobile.staff.core.testing.TestFixtures
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.Instant

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
    fun `toUpdateRequestDto maps the partial update`() {
        val product = TestFixtures.productUpdate(
            name = "Lámpara",
            price = 89.9,
            stock = 4,
            description = "",
            version = 7,
        )

        val dto = product.toUpdateRequestDto()

        assertEquals("Lámpara", dto.name)
        assertEquals(89.9, dto.price)
        assertEquals(4, dto.stock)
        assertEquals("", dto.description)
        assertEquals(7L, dto.version)
    }

    @Test
    fun `toUpdateRequestDto leaves an untouched stock out`() {
        val dto = TestFixtures.productUpdate(stock = null, version = null).toUpdateRequestDto()

        assertEquals(null, dto.stock)
        assertEquals(null, dto.version)
    }

    @Test
    fun `ProductDetailDto toDomain carries the version`() {
        val detail = ProductDetailDto(id = "p1", name = "Taladro", version = 8).toDomain()

        assertEquals(8L, detail.version)
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

        assertEquals(listOf("primary.jpg", "second.jpg", "third.jpg"), detail.images.map { it.url })
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

        assertEquals(listOf("photo.jpg"), dto.toDomain().images.map { it.url })
    }

    @Test
    fun `ProductDto toDomain carries stock and the server-computed discount`() {
        val product = ProductDto(
            id = "p1",
            name = "Foco LED",
            price = 35.9,
            stock = 1,
            discountedPrice = 30.9,
            discountEndsAt = "2026-10-25T05:00:00Z",
        ).toDomain()

        assertEquals(1, product.stock)
        assertEquals(30.9, product.discountedPrice)
        assertEquals(Instant.parse("2026-10-25T05:00:00Z"), product.discountEndsAt)
    }

    @Test
    fun `ProductDto toDomain keeps unknown stock and no discount as null`() {
        val product = ProductDto(id = "p1", name = "Foco LED", price = 35.9).toDomain()

        // Null stock means "unknown", never zero: the row must not invent a number.
        assertNull(product.stock)
        assertNull(product.discountedPrice)
        assertNull(product.discountEndsAt)
    }

    @Test
    fun `ProductDto toDomain maps an unparsable discount end to null`() {
        val product = ProductDto(
            id = "p1",
            name = "Foco LED",
            price = 35.9,
            discountedPrice = 30.9,
            discountEndsAt = "next friday",
        ).toDomain()

        assertEquals(30.9, product.discountedPrice)
        assertNull(product.discountEndsAt)
    }

    @Test
    fun `ProductDetailDto toDomain carries the server-computed discount`() {
        val detail = ProductDetailDto(
            id = "p1",
            name = "Foco LED",
            price = 35.9,
            discountedPrice = 30.9,
            discountEndsAt = "2026-10-25T05:00:00Z",
        ).toDomain()

        assertEquals(30.9, detail.discountedPrice)
        assertEquals(Instant.parse("2026-10-25T05:00:00Z"), detail.discountEndsAt)
    }

    @Test
    fun `ProductDetailDto toDomain keeps no discount as null`() {
        val detail = ProductDetailDto(id = "p1", name = "Foco LED", price = 35.9).toDomain()

        assertNull(detail.discountedPrice)
        assertNull(detail.discountEndsAt)
    }

    @Test
    fun `ProductDetailDto toDomain maps an unparsable discount end to null`() {
        val detail = ProductDetailDto(
            id = "p1",
            name = "Foco LED",
            price = 35.9,
            discountedPrice = 30.9,
            discountEndsAt = "next friday",
        ).toDomain()

        assertEquals(30.9, detail.discountedPrice)
        assertNull(detail.discountEndsAt)
    }
}
