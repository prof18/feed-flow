package com.prof18.feedflow.feedsync.test

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.resources.Resources
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

internal fun createMockHttpClient(mockEngine: MockEngine, baseURL: String): HttpClient {
    return HttpClient(mockEngine) {
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                },
            )
        }
        install(Resources)
        // Set baseURL for Resources plugin - required for URL construction
        defaultRequest {
            url(baseURL)
        }
    }
}
