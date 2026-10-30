package com.prof18.feedflow.shared.domain

import co.touchlab.kermit.Logger
import com.prof18.feedflow.core.utils.HttpHostValidator
import com.prof18.feedflow.shared.domain.recovery.FeedChallenge
import com.prof18.feedflow.shared.domain.recovery.RecoveryPolicy
import com.prof18.feedflow.shared.domain.recovery.ResponseTextDecoder
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.request
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import io.ktor.utils.io.readBuffer
import kotlinx.io.readByteArray

class HtmlRetriever(
    private val logger: Logger,
    private val client: HttpClient,
    private val forbiddenFallbackClient: HttpClient,
) {
    suspend fun retrieveHtml(url: String): String? =
        (retrieveResult(url) as? HtmlRetrievalResult.Content)?.html

    suspend fun retrieveResult(url: String): HtmlRetrievalResult {
        // Platform engines can reject unsupported hosts outside the normal suspend call path.
        if (!HttpHostValidator.isUrlSafeForHttpClient(url)) {
            logger.d { "Skipping URL with unsupported host for HTTP client: $url" }
            return HtmlRetrievalResult.Failed
        }
        return try {
            val firstAttempt = retrieveAttempt(url)
            if (firstAttempt.status == HttpStatusCode.Forbidden ||
                firstAttempt.result is HtmlRetrievalResult.Challenge
            ) {
                logger.d { "Retrying HTML retrieval with fallback user agent after HTTP 403" }
                retrieveAttempt(url, forbiddenFallbackClient).result
            } else {
                firstAttempt.result
            }
        } catch (e: Throwable) {
            logger.d(e) { "Unable to retrieve HTML, skipping" }
            HtmlRetrievalResult.Failed
        }
    }

    /** Validates and decodes HTML captured by a browser or a website session. */
    fun decodeCaptured(bytes: ByteArray, contentType: String?, effectiveUrl: String): HtmlRetrievalResult {
        if (!HttpHostValidator.isUrlSafeForHttpClient(effectiveUrl)) {
            logger.d { "Skipping captured HTML with unsupported host" }
            return HtmlRetrievalResult.Failed
        }
        if (!isReadableContentType(contentType)) {
            logger.d { "Skipping captured non-readable content type: $contentType" }
            return HtmlRetrievalResult.Failed
        }
        if (bytes.size > RecoveryPolicy.MAX_BODY_BYTES) {
            return HtmlRetrievalResult.Failed
        }
        return HtmlRetrievalResult.Content(ResponseTextDecoder.decode(bytes, contentType), effectiveUrl)
    }

    private suspend fun retrieveAttempt(url: String, requestClient: HttpClient = client): RetrievalAttempt =
        requestClient.prepareGet(url) {
            header(HttpHeaders.Accept, READABLE_CONTENT_ACCEPT_HEADER)
        }.execute { response ->
            if (RecoveryPolicy.isChallenge(response.headers[RecoveryPolicy.CHALLENGE_HEADER])) {
                logger.d { "Website verification required, HTTP status: ${response.status}" }
                val challenge = FeedChallenge(
                    requestedUrl = url,
                    effectiveUrl = response.request.url.toString(),
                    statusCode = response.status.value,
                    retryAfter = response.headers[HttpHeaders.RetryAfter],
                )
                return@execute RetrievalAttempt(response.status, HtmlRetrievalResult.Challenge(challenge))
            }
            if (!response.status.isSuccess()) {
                logger.d { "Unable to retrieve HTML, HTTP status: ${response.status}" }
                return@execute RetrievalAttempt(response.status, HtmlRetrievalResult.Failed)
            }
            val contentTypeHeader = response.headers[HttpHeaders.ContentType]
            if (!isReadableContentType(contentTypeHeader)) {
                logger.d { "Skipping non-readable content type: $contentTypeHeader for $url" }
                return@execute RetrievalAttempt(response.status, HtmlRetrievalResult.Failed)
            }
            val declaredLength = response.headers[HttpHeaders.ContentLength]?.toLongOrNull()
            if (declaredLength != null && declaredLength > MAX_RESPONSE_BYTES) {
                return@execute RetrievalAttempt(response.status, HtmlRetrievalResult.Failed)
            }
            val buffer = response.bodyAsChannel().readBuffer(MAX_RESPONSE_BYTES + 1)
            if (buffer.size > MAX_RESPONSE_BYTES) {
                return@execute RetrievalAttempt(response.status, HtmlRetrievalResult.Failed)
            }
            val bytes = buffer.readByteArray()
            RetrievalAttempt(
                status = response.status,
                result = HtmlRetrievalResult.Content(
                    html = ResponseTextDecoder.decode(bytes, contentTypeHeader),
                    effectiveUrl = response.request.url.toString(),
                ),
            )
        }

    private fun isReadableContentType(contentTypeHeader: String?): Boolean {
        if (contentTypeHeader.isNullOrBlank()) return true
        val mediaType = contentTypeHeader.substringBefore(';').trim().lowercase()
        return mediaType in readableContentTypes
    }

    private companion object {
        private const val MAX_RESPONSE_BYTES = 5 * 1024 * 1024 // 5 MB
        private const val READABLE_CONTENT_ACCEPT_HEADER =
            "text/html, application/xhtml+xml, application/xml;q=0.9, text/xml;q=0.9, " +
                "application/rss+xml;q=0.8, application/atom+xml;q=0.8, */*;q=0.1"
        private val readableContentTypes = setOf(
            "text/html",
            "application/xhtml+xml",
            "application/xml",
            "text/xml",
            "application/rss+xml",
            "application/atom+xml",
        )
    }
}

sealed interface HtmlRetrievalResult {
    data class Content(val html: String, val effectiveUrl: String) : HtmlRetrievalResult
    data class Challenge(val challenge: FeedChallenge) : HtmlRetrievalResult
    data object Failed : HtmlRetrievalResult
}

private data class RetrievalAttempt(val status: HttpStatusCode, val result: HtmlRetrievalResult)
