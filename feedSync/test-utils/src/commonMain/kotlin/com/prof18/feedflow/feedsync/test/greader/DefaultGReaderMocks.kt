package com.prof18.feedflow.feedsync.test.greader

import io.ktor.http.HttpStatusCode

/**
 * Configures default mock responses for GReader API.
 * These cover common endpoints with safe default responses (empty lists, success statuses).
 */
internal fun GReaderMockEngineBuilder.configureDefaultGReaderMocks() {
    // Login success
    addMockResponse(
        urlPattern = "/accounts/ClientLogin",
        method = "POST",
        responseContent = "SID=test-sid\nLSID=test-lsid\nAuth=test-auth-token",
        statusCode = HttpStatusCode.OK,
    )

    // Token
    addMockResponse(
        urlPattern = "/reader/api/0/token",
        method = "GET",
        responseContent = "test-token-12345",
    )

    // Empty subscription list by default
    addMockResponse(
        urlPattern = "/reader/api/0/subscription/list",
        method = "GET",
        responseContent = """{"subscriptions":[]}""",
    )

    // Empty stream IDs by default
    addMockResponse(
        urlPattern = "/reader/api/0/stream/items/ids",
        method = "GET",
        responseContent = """{"itemRefs":[],"continuation":null}""",
    )

    // Empty stream contents by default
    addMockResponse(
        urlPattern = "/reader/api/0/stream/contents",
        method = "GET",
        responseContent = """{"items":[],"continuation":null}""",
    )

    // Edit tag success
    addMockResponse(
        urlPattern = "/reader/api/0/edit-tag",
        method = "POST",
        responseContent = "OK",
    )

    // Subscription edit success
    addMockResponse(
        urlPattern = "/reader/api/0/subscription/edit",
        method = "POST",
        responseContent = "OK",
    )

    // Subscription quickadd success
    addMockResponse(
        urlPattern = "/reader/api/0/subscription/quickadd",
        method = "POST",
        responseContent = """{"numResults":1,"streamId":"feed/http://example.com/feed"}""",
    )
}
