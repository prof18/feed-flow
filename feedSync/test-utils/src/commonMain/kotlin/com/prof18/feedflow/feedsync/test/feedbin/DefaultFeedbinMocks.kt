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

    // Update subscription success (PATCH /v2/subscriptions/{id}.json)
    addMockResponse(
        urlPattern = "/v2/subscriptions/",
        method = "PATCH",
        responseContent = """{"id":1,"created_at":"2026-01-26T17:25:57.701911Z",""" +
            """"feed_id":1,"title":"Updated Feed",""" +
            """"feed_url":"http://example.com/feed","site_url":"http://example.com"}""",
    )

    // Mark as unread success (POST /v2/unread_entries.json)
    // Returns array of entry IDs that were marked as unread
    addMockResponse(
        urlPattern = "/v2/unread_entries.json",
        method = "POST",
        responseContent = """[1]""",
    )

    // Mark as read success (DELETE /v2/unread_entries.json)
    // Returns array of entry IDs that were marked as read
    addMockResponse(
        urlPattern = "/v2/unread_entries.json",
        method = "DELETE",
        responseContent = """[1]""",
        statusCode = HttpStatusCode.OK,
    )

    // Star entries success (POST /v2/starred_entries.json)
    // Returns array of entry IDs that were starred
    addMockResponse(
        urlPattern = "/v2/starred_entries.json",
        method = "POST",
        responseContent = """[1]""",
    )

    // Unstar entries success (DELETE /v2/starred_entries.json)
    // Returns array of entry IDs that were unstarred
    addMockResponse(
        urlPattern = "/v2/starred_entries.json",
        method = "DELETE",
        responseContent = """[1]""",
        statusCode = HttpStatusCode.OK,
    )
}
