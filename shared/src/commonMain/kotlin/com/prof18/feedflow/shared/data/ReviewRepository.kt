package com.prof18.feedflow.shared.data

import com.prof18.feedflow.core.utils.AppConfig
import com.prof18.feedflow.database.DatabaseHelper
import kotlinx.datetime.Clock
import kotlin.time.Duration.Companion.days

internal class ReviewRepository(
    private val settingsRepository: SettingsRepository,
    private val databaseHelper: DatabaseHelper,
    private val appConfig: AppConfig,
) {
    @Suppress("ReturnCount", "MagicNumber")
    suspend fun shouldShowReview(): Boolean {
        val reviewCount = settingsRepository.getReviewRequestCount()
        if (reviewCount > MAX_REVIEW_REQUESTS) {
            return false
        }

        val lastReviewVersion = settingsRepository.getLastReviewVersion()
        if (lastReviewVersion == appConfig.appVersion) {
            return false
        }

        val feeds = databaseHelper.getFeedSources()
        if (feeds.isEmpty()) {
            return false
        }

        val firstInstallDate = settingsRepository.getFirstInstallationDate()
        val now = Clock.System.now().toEpochMilliseconds()
        val millisSinceInstall = now - firstInstallDate

        if (millisSinceInstall < DAYS_BEFORE_FIRST_REVIEW.days.inWholeMilliseconds) {
            return true
        }

        val lastReviewDate = settingsRepository.getLastReviewRequestDate()
        if (lastReviewDate == 0L) {
            return true
        }

        val millisSinceLastReview = now - lastReviewDate
        return when (reviewCount) {
            1 -> millisSinceLastReview >= 1.days.inWholeMilliseconds * 30
            2 -> millisSinceLastReview >= 2.days.inWholeMilliseconds * 30
            else -> false
        }
    }

    fun onReviewShown() {
        settingsRepository.incrementReviewRequestCount()
        settingsRepository.setLastReviewRequestDate(Clock.System.now().toEpochMilliseconds())
        settingsRepository.setLastReviewVersion(appConfig.appVersion)
    }

    private companion object {
        const val MAX_REVIEW_REQUESTS = 3
        const val DAYS_BEFORE_FIRST_REVIEW = 2L
    }
}
