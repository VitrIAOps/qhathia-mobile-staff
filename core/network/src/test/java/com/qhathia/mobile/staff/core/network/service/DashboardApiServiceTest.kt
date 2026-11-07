package com.qhathia.mobile.staff.core.network.service

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.url
import io.ktor.http.ContentType
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DashboardApiServiceTest {

    private val json = Json { ignoreUnknownKeys = true }
    private val requests = mutableListOf<HttpRequestData>()

    private val service = DashboardApiService(
        HttpClient(
            MockEngine { request ->
                requests += request
                respond(
                    content = "{}",
                    headers = headersOf("Content-Type", ContentType.Application.Json.toString()),
                )
            },
        ) {
            install(ContentNegotiation) { json(json) }
            defaultRequest { url("http://localhost/") }
        },
    )

    @Test
    fun `the ranking asks for five top sellers by default`() = runTest {
        service.getRanking(granularity = "DAILY", categoryId = null)

        val url = requests.single().url
        assertEquals("/api/v1/dashboard/ranking", url.encodedPath)
        assertEquals("5", url.parameters["limit"])
        assertEquals("DAILY", url.parameters["granularity"])
    }
}
