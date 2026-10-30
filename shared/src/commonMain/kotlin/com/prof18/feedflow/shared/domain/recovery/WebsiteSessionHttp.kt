package com.prof18.feedflow.shared.domain.recovery

import com.prof18.feedflow.core.model.FeedRecoveryProfile
import com.prof18.rssparser.model.RssChannel

interface WebsiteSessionHttp {
    /** Sends exactly one request with the profile's saved identity; never follows a redirect. */
    suspend fun fetch(request: WebsiteSessionRequest): WebsiteSessionResponse
}

/** Parses captured or session feed bytes without a network request. */
class CapturedFeedParser(private val parseText: suspend (String) -> RssChannel) {
    suspend fun parse(bytes: ByteArray, contentType: String?): RssChannel =
        parseText(ResponseTextDecoder.decode(bytes, contentType))
}

data class WebsiteSessionRequest(
    val profile: FeedRecoveryProfile,
    val url: String,
    val kind: WebsiteSessionKind,
    // Subscription URL for feeds: validators are read and the response is recorded under it
    val cacheKey: String?,
)

enum class WebsiteSessionKind { Feed, Html }

sealed interface WebsiteSessionResponse {
    class Body(val bytes: ByteArray, val contentType: String?, val effectiveUrl: String) : WebsiteSessionResponse
    data object NotModified : WebsiteSessionResponse
    data class Challenge(val statusCode: Int, val retryAfter: String?) : WebsiteSessionResponse
    data class Failure(val reason: WebsiteSessionFailure) : WebsiteSessionResponse
}

enum class WebsiteSessionFailure { NoIdentity, Redirect, Http, TooLarge, Transport, Timeout }
