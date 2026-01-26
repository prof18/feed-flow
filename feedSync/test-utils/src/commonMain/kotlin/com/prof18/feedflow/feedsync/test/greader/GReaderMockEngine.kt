package com.prof18.feedflow.feedsync.test.greader

import com.prof18.feedflow.core.model.SyncAccounts
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
 * Creates a pre-configured HttpClient with GReader mock responses.
 *
 * This is a convenience function that sets up common GReader API mocks.
 * Override or extend as needed in your tests.
 *
 * @param provider Optional provider type (e.g., SyncAccounts.FRESH_RSS) to use provider-specific fixtures
 */
fun createMockGReaderHttpClient(
    provider: SyncAccounts? = null,
    baseURL: String,
    configure: GReaderMockEngineBuilder.() -> Unit = {},
): HttpClient {
    val mockEngine = createGReaderMockEngine(provider, configure)
    return createMockHttpClient(mockEngine, baseURL)
}

/**
 * Creates a MockEngine for GReader API testing.
 *
 * ```
 * val mockEngine = createGReaderMockEngine(SyncAccounts.FRESH_RSS) {
 *     addMockResponse(
 *         urlPattern = "/accounts/ClientLogin",
 *         method = "POST",
 *         responseFile = "login_success.txt"
 *     )
 *     addMockResponse(
 *         urlPattern = "/reader/api/0/subscription/list",
 *         responseFile = "subscriptions.json"
 *     )
 * }
 * ```
 *
 * @param provider Optional provider type to use provider-specific fixtures from subdirectories
 */
fun createGReaderMockEngine(
    provider: SyncAccounts? = null,
    configure: GReaderMockEngineBuilder.() -> Unit = {},
): MockEngine {
    val builder = GReaderMockEngineBuilder(provider)
    builder.configure()
    return builder.build()
}

class GReaderMockEngineBuilder(
    private val provider: SyncAccounts? = null,
) {
    private val mockResponses = mutableListOf<GReaderMockResponseConfig>()

    fun addMockResponse(
        urlPattern: String,
        method: String = "GET",
        responseFile: String? = null,
        statusCode: HttpStatusCode = HttpStatusCode.OK,
        headers: Map<String, String> = emptyMap(),
        responseContent: String? = null,
    ) {
        mockResponses.add(
            GReaderMockResponseConfig(
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

    private fun findMatchingConfig(request: HttpRequestData): GReaderMockResponseConfig? {
        val url = request.url.toString()
        val method = request.method.value

        val match = mockResponses.firstOrNull { config ->
            val urlMatches = url.contains(config.urlPattern)
            val methodMatches = method.equals(config.method, ignoreCase = true)
            urlMatches && methodMatches
        }

        return match
    }

    private fun MockRequestHandleScope.createResponse(
        config: GReaderMockResponseConfig,
    ): HttpResponseData {
        val content = when {
            config.responseContent != null -> config.responseContent
            config.responseFile != null -> {
                val fixturePath = when {
                    provider != null -> {
                        val providerDir = provider.name.lowercase().replace("_", "")
                        "greader/$providerDir/${config.responseFile}"
                    }
                    else -> "greader/${config.responseFile}"
                }
                loadFixture(fixturePath)
            }
            else -> "{}"
        }

        val defaultContentType = if (config.responseFile?.endsWith(".txt") == true) {
            "text/plain"
        } else {
            "application/json"
        }
        val contentType = config.headers[HttpHeaders.ContentType] ?: defaultContentType
        val headers = headersOf(
            HttpHeaders.ContentType to listOf(contentType),
            *config.headers.filterKeys { it != HttpHeaders.ContentType }
                .map { (key, value) -> key to listOf(value) }.toTypedArray(),
        )

        return respond(
            content = content,
            status = config.statusCode,
            headers = headers,
        )
    }
}

internal data class GReaderMockResponseConfig(
    val urlPattern: String,
    val method: String,
    val responseFile: String?,
    val statusCode: HttpStatusCode,
    val headers: Map<String, String>,
    val responseContent: String?,
)
