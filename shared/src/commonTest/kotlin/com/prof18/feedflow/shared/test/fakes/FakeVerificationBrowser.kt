package com.prof18.feedflow.shared.test.fakes

import com.prof18.feedflow.shared.domain.recovery.BrowserCapture
import com.prof18.feedflow.shared.domain.recovery.FeedRecoveryCoordinator
import com.prof18.feedflow.shared.domain.recovery.FeedRecoveryRepository
import com.prof18.feedflow.shared.domain.recovery.RecoveryOrigin
import com.prof18.feedflow.shared.domain.recovery.VerificationFailure
import com.prof18.feedflow.shared.domain.recovery.WebsiteVerificationRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** Plays the platform browser: answers every visible verification request as configured. */
class FakeVerificationBrowser(
    private val coordinator: FeedRecoveryCoordinator,
    private val recoveryRepository: FeedRecoveryRepository,
) {
    val shownRequests = mutableListOf<WebsiteVerificationRequest>()
    var failure: VerificationFailure? = null
    var respond: Boolean = true
    var bytes: ByteArray = "<rss/>".encodeToByteArray()

    fun start(scope: CoroutineScope): Job = scope.launch {
        coordinator.visibleState.collect { request ->
            if (request == null || request.validating || request.failure != null) return@collect
            shownRequests += request
            if (!respond) return@collect
            val reason = failure
            if (reason != null) {
                coordinator.failed(request.id, reason)
                return@collect
            }
            if (!coordinator.startRead(request.id)) return@collect
            val profile = recoveryRepository.acquireProfile(
                epoch = request.accountEpoch,
                origin = requireNotNull(RecoveryOrigin.of(request.url)),
                userAgent = "fake-browser",
                engineVersion = "1",
            )
            coordinator.captured(
                id = request.id,
                response = BrowserCapture(
                    effectiveUrl = request.url,
                    statusCode = 200,
                    contentType = "application/rss+xml",
                    bytes = bytes,
                ),
                profile = profile,
            )
        }
    }
}
