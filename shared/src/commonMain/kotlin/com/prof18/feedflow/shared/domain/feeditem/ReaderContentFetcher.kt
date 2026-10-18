package com.prof18.feedflow.shared.domain.feeditem

import com.prof18.feedflow.core.domain.DateFormatter
import com.prof18.feedflow.shared.domain.HtmlRetrievalResult
import com.prof18.feedflow.shared.domain.HtmlRetriever
import com.prof18.feedflow.shared.domain.feed.httpcache.FeedRefreshScheduler
import com.prof18.feedflow.shared.domain.recovery.FeedChallenge
import com.prof18.feedflow.shared.domain.recovery.FeedRecoveryCoordinator
import com.prof18.feedflow.shared.domain.recovery.FeedRecoveryRepository
import com.prof18.feedflow.shared.domain.recovery.ReaderFetchResult
import com.prof18.feedflow.shared.domain.recovery.RecoveryOrigin
import com.prof18.feedflow.shared.domain.recovery.RecoveryTicket
import com.prof18.feedflow.shared.domain.recovery.VerificationFailure
import com.prof18.feedflow.shared.domain.recovery.VerificationPurpose
import com.prof18.feedflow.shared.domain.recovery.WebsiteSessionHttp
import com.prof18.feedflow.shared.domain.recovery.WebsiteSessionKind
import com.prof18.feedflow.shared.domain.recovery.WebsiteSessionRequest
import com.prof18.feedflow.shared.domain.recovery.WebsiteSessionResponse
import com.prof18.feedflow.shared.domain.recovery.WebsiteVerificationFailure
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

@Suppress("LongParameterList")
internal class ReaderContentFetcher(
    private val articleContentParser: ArticleContentParser,
    private val feedItemContentFileHandler: FeedItemContentFileHandler,
    private val htmlRetriever: HtmlRetriever,
    private val recoveryRepository: FeedRecoveryRepository,
    private val coordinator: FeedRecoveryCoordinator,
    private val sessionHttp: WebsiteSessionHttp?,
    private val dateFormatter: DateFormatter,
) {
    suspend fun fetch(feedItemId: String, url: String, save: Boolean = false): String? {
        val content = articleContentParser.parse(url)?.takeIf { it.isNotBlank() } ?: return null
        if (save) {
            feedItemContentFileHandler.saveFeedItemContentToFile(feedItemId, content)
        }
        return content
    }

    suspend fun fetchForReader(
        articleId: String,
        url: String,
        save: Boolean,
        ticket: RecoveryTicket,
    ): ReaderFetchResult = when (val result = articleContentParser.parseResult(url)) {
        is ArticleParseResult.Content -> {
            if (result.content.isBlank()) {
                ReaderFetchResult.Unavailable
            } else {
                if (save) feedItemContentFileHandler.saveFeedItemContentToFile(articleId, result.content)
                ReaderFetchResult.Content(result.content, result.baseUrl)
            }
        }
        is ArticleParseResult.Challenge -> challenged(articleId, result.challenge, save, ticket)
        ArticleParseResult.Failed -> ReaderFetchResult.Unavailable
    }

    suspend fun verifyForReader(
        articleId: String,
        verificationUrl: String,
        save: Boolean,
        ticket: RecoveryTicket,
    ): ReaderFetchResult = ticket.guard {
        coordinator.verify(verificationUrl, VerificationPurpose.ReaderArticle, ticket) { capture, profile ->
            val html = htmlRetriever.decodeCaptured(capture.bytes, capture.contentType, capture.effectiveUrl)
                as? HtmlRetrievalResult.Content
                ?: throw WebsiteVerificationFailure(VerificationFailure.InvalidContent)
            val content = articleContentParser.parseHtml(html.html, capture.effectiveUrl)
                ?.takeIf { it.isNotBlank() }
                ?: throw WebsiteVerificationFailure(VerificationFailure.InvalidContent)
            recoveryRepository.retainForReader(profile, ticket)
            ReaderFetchResult.Content(
                content = content,
                baseUrl = capture.effectiveUrl,
                saveFailed = save && !saveRecovered(articleId, content, ticket) && ticket.isValid,
            )
        }
    }

    private suspend fun challenged(
        articleId: String,
        challenge: FeedChallenge,
        save: Boolean,
        ticket: RecoveryTicket,
    ): ReaderFetchResult {
        if (!recoveryRepository.supported) return ReaderFetchResult.Unavailable
        return sessionContent(articleId, challenge, save, ticket)
            ?: ReaderFetchResult.NeedsVerification(
                verificationUrl = challenge.effectiveUrl,
                blockedUntil = FeedRefreshScheduler.computeBackoffTimestamp(
                    now = dateFormatter.currentTimeMillis(),
                    retryAfter = challenge.retryAfter,
                ),
            )
    }

    /** Returns null when the saved session cannot produce the article, so the anonymous result is kept. */
    private suspend fun sessionContent(
        articleId: String,
        challenge: FeedChallenge,
        save: Boolean,
        ticket: RecoveryTicket,
    ): ReaderFetchResult? {
        val http = sessionHttp ?: return null
        val origin = RecoveryOrigin.of(challenge.effectiveUrl) ?: return null
        val profile = recoveryRepository.activeProfile(ticket.accountEpoch, origin) ?: return null
        val request = WebsiteSessionRequest(profile, challenge.effectiveUrl, WebsiteSessionKind.Html, cacheKey = null)
        val response = try {
            http.fetch(request)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Throwable) {
            return null
        }
        val body = response as? WebsiteSessionResponse.Body ?: return null
        val html = htmlRetriever.decodeCaptured(body.bytes, body.contentType, body.effectiveUrl)
            as? HtmlRetrievalResult.Content
            ?: return null
        val content = articleContentParser.parseHtml(html.html, html.effectiveUrl)
            ?.takeIf { it.isNotBlank() }
            ?: return null
        return ReaderFetchResult.Content(
            content = content,
            baseUrl = html.effectiveUrl,
            saveFailed = save && !saveRecovered(articleId, content, ticket) && ticket.isValid,
        )
    }

    private suspend fun saveRecovered(articleId: String, content: String, ticket: RecoveryTicket): Boolean {
        val staged = feedItemContentFileHandler.stageContent(articleId, content) ?: return false
        return try {
            feedItemContentFileHandler.promoteStaged(staged, ticket::commit)
        } finally {
            withContext(NonCancellable) { feedItemContentFileHandler.discardStaged(staged) }
        }
    }
}
