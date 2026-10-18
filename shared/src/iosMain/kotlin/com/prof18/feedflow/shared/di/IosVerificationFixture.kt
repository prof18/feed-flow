package com.prof18.feedflow.shared.di

import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.shared.domain.recovery.FeedChallenge
import com.prof18.feedflow.shared.domain.recovery.FeedRecoveryRepository
import com.prof18.feedflow.shared.domain.recovery.RecoveryUpdate

/** Points a seeded feed at an unreachable HTTPS URL and records it as waiting for verification. */
internal suspend fun applyIosVerificationFixture(
    fixture: String,
    databaseHelper: DatabaseHelper,
    feedRecoveryRepository: FeedRecoveryRepository,
) {
    check(fixture == "pending") { "Unknown iOS verification fixture: $fixture" }

    val seededSource = databaseHelper.getFeedSources().first { it.id == FIXTURE_SOURCE_ID }
    val source = seededSource.copy(url = VERIFICATION_FIXTURE_URL, websiteUrl = null)
    databaseHelper.updateFeedSource(source)
    val challenge = FeedChallenge(
        requestedUrl = source.url,
        effectiveUrl = source.url,
        statusCode = 403,
        retryAfter = null,
    )
    feedRecoveryRepository.apply(
        update = RecoveryUpdate.RecordChallenge(source = source, challenge = challenge, previous = null),
        ticket = feedRecoveryRepository.issueTicket(),
    )
}

private const val FIXTURE_SOURCE_ID = "e2e-feed-android-weekly"
private const val VERIFICATION_FIXTURE_URL = "https://feed-verification.invalid/rss"
