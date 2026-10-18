package com.prof18.feedflow.shared.domain.recovery

import com.prof18.feedflow.core.model.FeedRecoveryRecord
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.RecoveryMode
import com.prof18.feedflow.shared.domain.feed.RssParserWrapper
import com.prof18.rssparser.exception.HttpException
import com.prof18.rssparser.model.RssChannel
import kotlinx.coroutines.CancellationException

internal class FeedAttempt(
    private val rssParserWrapper: RssParserWrapper,
    private val recoveryRepository: FeedRecoveryRepository,
    private val sessionHttp: WebsiteSessionHttp?,
    private val capturedFeedParser: CapturedFeedParser?,
) {
    suspend fun run(source: FeedSource, record: FeedRecoveryRecord?): FeedOutcome {
        sessionOutcome(source, record)?.let { return it }
        return try {
            FeedOutcome.Fetched(rssParserWrapper.getRssChannel(source.url), viaSession = false)
        } catch (e: CancellationException) {
            throw e
        } catch (e: HttpException) {
            if (e.code == HTTP_NOT_MODIFIED) FeedOutcome.NotModified(viaSession = false) else FeedOutcome.Failed(e)
        } catch (e: FeedVerificationRequired) {
            FeedOutcome.Challenged(e.challenge)
        } catch (e: Throwable) {
            FeedOutcome.Failed(e)
        }
    }

    /** Returns null when the session cannot be used, so the normal tiers run. */
    @Suppress("SwallowedException")
    private suspend fun sessionOutcome(source: FeedSource, record: FeedRecoveryRecord?): FeedOutcome? {
        if (record?.mode != RecoveryMode.SessionApproved) return null
        val http = sessionHttp ?: return null
        val parser = capturedFeedParser ?: return null
        val profileId = record.profileId ?: return null
        val profile = recoveryRepository.profile(profileId)?.takeIf { recoveryRepository.isCurrent(it) }
            ?: return null
        val request = WebsiteSessionRequest(
            profile = profile,
            url = record.effectiveUrl,
            kind = WebsiteSessionKind.Feed,
            cacheKey = source.url,
        )
        val response = try {
            http.fetch(request)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            return null
        }
        return when (response) {
            is WebsiteSessionResponse.Body -> try {
                FeedOutcome.Fetched(parser.parse(response.bytes, response.contentType), viaSession = true)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                null
            }

            WebsiteSessionResponse.NotModified -> FeedOutcome.NotModified(viaSession = true)
            is WebsiteSessionResponse.Challenge -> FeedOutcome.Challenged(
                FeedChallenge(
                    requestedUrl = source.url,
                    effectiveUrl = record.effectiveUrl,
                    statusCode = response.statusCode,
                    retryAfter = response.retryAfter,
                ),
            )

            is WebsiteSessionResponse.Failure -> null
        }
    }

    private companion object {
        const val HTTP_NOT_MODIFIED = 304
    }
}

sealed interface FeedOutcome {
    data class Fetched(val channel: RssChannel, val viaSession: Boolean) : FeedOutcome
    data class NotModified(val viaSession: Boolean) : FeedOutcome
    data class Challenged(val challenge: FeedChallenge) : FeedOutcome
    data class Failed(val error: Throwable) : FeedOutcome
}
