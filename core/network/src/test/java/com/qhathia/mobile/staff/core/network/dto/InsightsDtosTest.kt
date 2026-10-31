package com.qhathia.mobile.staff.core.network.dto

import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class InsightsDtosTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `unmet searches carry the requests outside the line of business`() {
        val dto = json.decodeFromString(
            UnmetSearchesDto.serializer(),
            """{"totalPeople":5,""" +
                """"groups":[{"term":"lampara solar","personCount":5,"searchCount":7,""" +
                """"examples":[{"rawText":"una lámpara solar","count":4}],"storeFit":0.6}],""" +
                """"otherRequests":[{"term":"pintura","personCount":3,"searchCount":3,""" +
                """"examples":[{"rawText":"pintura","count":3}],"storeFit":0.1}]}""",
        )

        assertEquals(listOf("lampara solar"), dto.groups.map { it.term })
        assertEquals(listOf("pintura"), dto.otherRequests.map { it.term })
        assertEquals(3, dto.otherRequests.single().personCount)
    }

    @Test
    fun `an older response without other requests decodes them as empty`() {
        val dto = json.decodeFromString(
            UnmetSearchesDto.serializer(),
            """{"totalPeople":1,"groups":[{"term":"taladro","personCount":1}]}""",
        )

        assertTrue(dto.otherRequests.isEmpty())
    }
}
