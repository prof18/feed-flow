package com.prof18.feedflow.feedsync.test.feedbin

import com.prof18.feedflow.feedsync.test.createMockHttpClient
import com.prof18.feedflow.feedsync.test.loadFixture
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf

/**
 * Creates a pre-configured HttpClient with Feedbin mock responses.
 *
 * This is a convenience function that sets up common Feedbin API mocks.
 * Override or extend as needed in your tests.
 */
fun createMockFeedbinHttpClient(
    baseURL: String = "https://api.feedbin.com/",
    configure: FeedbinMockEngineBuilder.() -> Unit = {},
): HttpClient {
    val mockEngine = createFeedbinMockEngine(configure)
    return createMockHttpClient(mockEngine, baseURL)
}

/**
 * Creates a MockEngine for Feedbin API testing.
 *
 *
 * Example usage:
 * ```
 * val mockEngine = createFeedbinMockEngine {
 *     addMockResponse(
 *         urlPattern = "/v2/authentication.json",
 *         responseFile = "auth_success.json"
 *     )
 *     addMockResponse(
 *         urlPattern = "/v2/subscriptions.json",
 *         responseFile = "subscriptions.json"
 *     )
 * }
 * ```
 */
internal fun createFeedbinMockEngine(
    configure: FeedbinMockEngineBuilder.() -> Unit = {},
): MockEngine {
    val builder = FeedbinMockEngineBuilder()
    builder.configure()
    return builder.build()
}

class FeedbinMockEngineBuilder {
    private val mockResponses = mutableListOf<FeedbinMockResponseConfig>()

    fun addMockResponse(
        urlPattern: String,
        method: String = "GET",
        responseFile: String? = null,
        statusCode: HttpStatusCode = HttpStatusCode.OK,
        headers: Map<String, String> = emptyMap(),
        responseContent: String? = null,
    ) {
        mockResponses.add(
            FeedbinMockResponseConfig(
                urlPattern = urlPattern,
                method = method,
                responseFile = responseFile,
                statusCode = statusCode,
                headers = headers,
                responseContent = responseContent,
            ),
        )
    }

    internal fun build(): MockEngine {
        return MockEngine { request ->
            val config = findMatchingConfig(request)

            if (config == null) {
                respond(
                    content = """{"error": "No mock configured for ${request.method.value} ${request.url}"}""",
                    status = HttpStatusCode.NotFound,
                    headers = headersOf(HttpHeaders.ContentType, "application/json"),
                )
            } else {
                createResponse(config)
            }
        }
    }

    private fun findMatchingConfig(request: HttpRequestData): FeedbinMockResponseConfig? {
        val url = request.url.toString()
        val method = request.method.value

        // Search in reverse order so that more recently added mocks (like error mocks) take precedence
        return mockResponses.lastOrNull { config ->
            url.contains(config.urlPattern) && method.equals(config.method, ignoreCase = true)
        }
    }

    private fun MockRequestHandleScope.createResponse(
        config: FeedbinMockResponseConfig,
    ): HttpResponseData {
        val content = when {
            config.responseContent != null -> config.responseContent
            config.responseFile != null -> loadFixture("feedbin/${config.responseFile}")
            else -> "[]"
        }

        val headers = headersOf(
            HttpHeaders.ContentType to listOf("application/json"),
            *config.headers.map { (key, value) -> key to listOf(value) }.toTypedArray(),
        )

        return respond(
            content = content,
            status = config.statusCode,
            headers = headers,
        )
    }
}

internal data class FeedbinMockResponseConfig(
    val urlPattern: String,
    val method: String,
    val responseFile: String?,
    val statusCode: HttpStatusCode,
    val headers: Map<String, String>,
    val responseContent: String?,
)
