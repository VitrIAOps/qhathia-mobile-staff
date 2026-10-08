package com.qhathia.mobile.staff.core.network.dto

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
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
}
