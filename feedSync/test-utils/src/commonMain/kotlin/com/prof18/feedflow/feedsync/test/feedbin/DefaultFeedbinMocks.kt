package com.prof18.feedflow.feedsync.test.feedbin

import io.ktor.http.HttpStatusCode

/**
 * Configures default mock responses for Feedbin API.
 * These cover common endpoints with safe default responses (empty lists, success statuses).
 */
internal fun FeedbinMockEngineBuilder.configureDefaultFeedbinMocks() {
    // Authentication success
    addMockResponse(
        urlPattern = "/v2/authentication.json",
        method = "GET",
        responseContent = """{}""",
        statusCode = HttpStatusCode.OK,
    )

    // Empty subscriptions list by default
    addMockResponse(
        urlPattern = "/v2/subscriptions.json",
        method = "GET",
        responseContent = """[]""",
    )

    // Empty icons list
    addMockResponse(
        urlPattern = "/v2/icons.json",
        method = "GET",
        responseContent = """[]""",
    )

    // Empty entries list
    addMockResponse(
        urlPattern = "/v2/entries.json",
        method = "GET",
        responseContent = """[]""",
    )

    // Empty unread entries
    addMockResponse(
        urlPattern = "/v2/unread_entries.json",
        method = "GET",
        responseContent = """[]""",
    )

    // Empty starred entries
    addMockResponse(
        urlPattern = "/v2/starred_entries.json",
        method = "GET",
        responseContent = """[]""",
    )

    // Empty taggings
    addMockResponse(
        urlPattern = "/v2/taggings.json",
        method = "GET",
        responseContent = """[]""",
    )

    // Create subscription success
    addMockResponse(
        urlPattern = "/v2/subscriptions.json",
        method = "POST",
        responseContent = """{"id":1,"feed_id":1,"title":"Test Feed",""" +
            """"feed_url":"http://example.com/feed","site_url":"http://example.com"}""",
    )

    // Delete subscription success (returns empty)
    addMockResponse(
        urlPattern = "/v2/subscriptions/",
        method = "DELETE",
        responseContent = "",
        statusCode = HttpStatusCode.NoContent,
    )

    // Update subscription success
    addMockResponse(
        urlPattern = "/v2/subscriptions/",
        method = "PATCH",
        responseContent = """{"id":1,"title":"Updated Feed"}""",
    )

    // Mark as read success (returns empty)
    addMockResponse(
        urlPattern = "/v2/unread_entries.json",
        method = "DELETE",
        responseContent = "",
        statusCode = HttpStatusCode.OK,
    )

    // Star entries success
    addMockResponse(
        urlPattern = "/v2/starred_entries.json",
        method = "POST",
        responseContent = """[]""",
    )

    // Unstar entries success
    addMockResponse(
        urlPattern = "/v2/starred_entries.json",
        method = "DELETE",
        responseContent = "",
        statusCode = HttpStatusCode.OK,
    )
}
