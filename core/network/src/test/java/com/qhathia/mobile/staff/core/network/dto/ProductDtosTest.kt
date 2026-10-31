package com.qhathia.mobile.staff.core.network.dto

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class ProductDtosTest {

    // Same defaults as the app's Json: fields left at their default value are not written.
    private val json = Json

    private fun encode(dto: CreateProductRequestDto) =
        json.encodeToJsonElement(CreateProductRequestDto.serializer(), dto).jsonObject

    @Test
    fun `a product without tags is sent with an explicit empty tag list`() {
        val body = encode(CreateProductRequestDto(name = "Taladro", price = 189.0, stock = 3))

        assertEquals(JsonArray(emptyList()), body["tags"])
        // Other defaulted fields keep being omitted.
        assertFalse(body.containsKey("position"))
    }

    @Test
    fun `entered tags are sent as they are`() {
        val body = encode(
            CreateProductRequestDto(name = "Taladro", price = 189.0, stock = 3, tags = listOf("bosch", "650w")),
        )

        assertEquals("""["bosch","650w"]""", body["tags"].toString())
    }

    private val lenientJson = Json { ignoreUnknownKeys = true }

    @Test
    fun `a products row carries stock and the server-computed discount`() {
        val row = lenientJson.decodeFromString(
            ProductDto.serializer(),
            """{"id":"p1","name":"Foco LED","price":35.90,"stock":1,""" +
                """"discountedPrice":30.90,"discountEndsAt":"2026-10-25T05:00:00Z","discounts":[]}""",
        )

        assertEquals(1, row.stock)
        assertEquals(30.90, row.discountedPrice)
        assertEquals("2026-10-25T05:00:00Z", row.discountEndsAt)
    }

    @Test
    fun `a products row from an older backend leaves stock and discount null`() {
        val row = lenientJson.decodeFromString(
            ProductDto.serializer(),
            """{"id":"p1","name":"Foco LED","price":35.90}""",
        )

        assertNull(row.stock)
        assertNull(row.discountedPrice)
        assertNull(row.discountEndsAt)
    }

    @Test
    fun `a product detail carries the server-computed discount`() {
        val detail = lenientJson.decodeFromString(
            ProductDetailDto.serializer(),
            """{"id":"p1","name":"Foco LED","price":35.90,"stock":1,""" +
                """"discountedPrice":30.90,"discountEndsAt":"2026-10-25T05:00:00Z",""" +
                """"discounts":[{"id":"d1","discountType":"FIXED_AMOUNT","value":5.00}]}""",
        )

        assertEquals(30.90, detail.discountedPrice)
        assertEquals("2026-10-25T05:00:00Z", detail.discountEndsAt)
    }

    @Test
    fun `a product detail without a discount in force leaves it null`() {
        val detail = lenientJson.decodeFromString(
            ProductDetailDto.serializer(),
            """{"id":"p1","name":"Foco LED","price":35.90,"discountedPrice":null}""",
        )

        assertNull(detail.discountedPrice)
        assertNull(detail.discountEndsAt)
    }
}
