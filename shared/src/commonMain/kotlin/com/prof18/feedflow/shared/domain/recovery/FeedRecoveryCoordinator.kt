package com.prof18.feedflow.shared.domain.recovery

import com.prof18.feedflow.core.model.FeedRecoveryProfile
import io.ktor.http.URLBuilder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.job
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.uuid.Uuid

class FeedRecoveryCoordinator(val available: Boolean) {
    private val mutex = Mutex()
    private val waiters = mutableListOf<Waiter>()
    private val visibleMutableState = MutableStateFlow<WebsiteVerificationRequest?>(null)
    val visibleState = visibleMutableState.asStateFlow()

    private val failureMessages = MutableSharedFlow<VerificationFailure>(extraBufferCapacity = 1)
    val failures = failureMessages.asSharedFlow()

    fun reportFailure(reason: VerificationFailure) {
        failureMessages.tryEmit(reason)
    }

    @Suppress("ThrowsCount")
    internal suspend fun <T> verify(
        url: String,
        purpose: VerificationPurpose,
        ticket: RecoveryTicket,
        consume: suspend (BrowserCapture, FeedRecoveryProfile) -> T,
    ): T {
        if (!available) throw WebsiteVerificationFailure(VerificationFailure.Unsupported)
        val approvedUrl = URLBuilder(url).apply { fragment = "" }.buildString()
        val origin = RecoveryOrigin.of(approvedUrl)?.value
            ?: throw WebsiteVerificationFailure(VerificationFailure.Unsupported)
        val epoch = ticket.accountEpoch
        val owner = currentCoroutineContext().job
        val waiter = mutex.withLock {
            val current = visibleMutableState.value
            if (current != null && (current.origin != origin || current.accountEpoch != epoch)) {
                throw WebsiteVerificationFailure(VerificationFailure.Busy)
            }
            val request = WebsiteVerificationRequest(
                id = Uuid.random().toString(),
                sessionId = current?.sessionId ?: Uuid.random().toString(),
                url = approvedUrl,
                origin = origin,
                accountEpoch = epoch,
                purpose = purpose,
            )
            Waiter(request, owner, ticket).also {
                waiters.add(it)
                publish()
            }
        }
        var consumed = false
        try {
            val capture = waiter.capture.await()
            return consume(capture.response, capture.profile).also { consumed = true }
        } finally {
            withContext(NonCancellable) {
                mutex.withLock {
                    // A successful consume has already committed; revoking it would cancel the caller's guard
                    if (!consumed) waiter.ticket.revoke()
                    waiters.remove(waiter)
                    publish()
                }
            }
        }
    }

    suspend fun startRead(id: String): Boolean = mutex.withLock {
        val current = waiters.firstOrNull() ?: return@withLock false
        if (current.request.id != id || current.request.validating || current.request.failure != null) {
            return@withLock false
        }
        current.request = current.request.copy(validating = true)
        publish()
        true
    }

    suspend fun captured(id: String, response: BrowserCapture, profile: FeedRecoveryProfile) = mutex.withLock {
        val current = waiters.firstOrNull() ?: return@withLock
        if (current.request.id != id || !current.request.validating || current.capture.isCompleted) return@withLock
        check(response.effectiveUrl == current.request.url && response.statusCode in SUCCESS_CODES)
        check(response.bytes.size <= RecoveryPolicy.MAX_BODY_BYTES)
        check(profile.accountEpoch == current.request.accountEpoch && profile.origin == current.request.origin)
        current.profileId = profile.id
        current.capture.complete(Captured(response, profile))
    }

    suspend fun failed(id: String, reason: VerificationFailure) = mutex.withLock {
        val current = waiters.firstOrNull() ?: return@withLock
        if (current.request.id != id) return@withLock
        if (current.capture.isCompleted) {
            if (reason == VerificationFailure.BrowserStopped) {
                current.ticket.revoke()
                current.owner.cancel()
            }
            return@withLock
        }
        current.request = current.request.copy(validating = false, failure = reason)
        publish()
    }

    suspend fun cancelAll() = mutex.withLock {
        waiters.forEach {
            it.ticket.revoke()
            it.owner.cancel(CancellationException("Website verification dismissed"))
        }
        waiters.clear()
        publish()
    }

    suspend fun cancelSession(sessionId: String) = mutex.withLock {
        val cancelled = waiters.filter { it.request.sessionId == sessionId }
        cancelled.forEach {
            it.ticket.revoke()
            it.owner.cancel()
        }
        waiters.removeAll(cancelled.toSet())
        publish()
    }

    suspend fun attachProfile(sessionId: String, profile: FeedRecoveryProfile) = mutex.withLock {
        waiters.filter { it.request.sessionId == sessionId }.forEach {
            check(it.request.accountEpoch == profile.accountEpoch && it.request.origin == profile.origin)
            it.profileId = profile.id
        }
    }

    internal suspend fun invalidateProfile(profileId: String) = mutex.withLock {
        waiters.filter { it.profileId == profileId }.forEach {
            it.ticket.revoke()
            it.owner.cancel()
        }
    }

    private fun publish() {
        visibleMutableState.value = waiters.firstOrNull()?.request
    }

    private class Waiter(var request: WebsiteVerificationRequest, val owner: Job, val ticket: RecoveryTicket) {
        val capture = CompletableDeferred<Captured>()
        var profileId: String? = null
    }

    private class Captured(val response: BrowserCapture, val profile: FeedRecoveryProfile)

    private companion object {
        val SUCCESS_CODES = 200..299
    }
}

data class WebsiteVerificationRequest(
    val id: String,
    val sessionId: String,
    val url: String,
    val origin: String,
    val accountEpoch: String,
    val purpose: VerificationPurpose,
    val validating: Boolean = false,
    val failure: VerificationFailure? = null,
)

data class BrowserCapture(
    val effectiveUrl: String,
    val statusCode: Int,
    val contentType: String?,
    val bytes: ByteArray,
)

enum class VerificationPurpose { Feed, ReaderArticle }

enum class VerificationFailure { AccessDenied, Unsupported, BrowserStopped, Timeout, SaveFailed, InvalidContent, Busy }

class WebsiteVerificationFailure(val reason: VerificationFailure) : Exception("Website verification failed")
