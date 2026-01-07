package com.prof18.feedflow.shared.data

import com.prof18.feedflow.core.utils.AppConfig
import com.prof18.feedflow.database.DatabaseHelper
import com.russhwolf.settings.Settings
import com.russhwolf.settings.set
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days

internal class ReviewRepository(
    private val settings: Settings,
    private val databaseHelper: DatabaseHelper,
    private val appConfig: AppConfig,
) {
    @Suppress("ReturnCount", "MagicNumber")
    suspend fun shouldShowReview(): Boolean {
        val reviewCount = getReviewRequestCount()
        if (reviewCount > MAX_REVIEW_REQUESTS) {
            return false
        }

        val lastReviewVersion = getLastReviewVersion()
        if (lastReviewVersion == appConfig.appVersion) {
            return false
        }

        val feeds = databaseHelper.getFeedSources()
        if (feeds.isEmpty()) {
            return false
        }

        val firstInstallDate = getFirstInstallationDate()
        val now = Clock.System.now().toEpochMilliseconds()
        val millisSinceInstall = now - firstInstallDate

        if (millisSinceInstall < DAYS_BEFORE_FIRST_REVIEW.days.inWholeMilliseconds) {
            return true
        }

        val lastReviewDate = getLastReviewRequestDate()
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
        incrementReviewRequestCount()
        setLastReviewRequestDate(Clock.System.now().toEpochMilliseconds())
        setLastReviewVersion(appConfig.appVersion)
    }

    private fun getFirstInstallationDate(): Long {
        val currentValue = settings.getLongOrNull(ReviewSettingsFields.FIRST_INSTALLATION_DATE.name)
        return if (currentValue == null) {
            val now = Clock.System.now().toEpochMilliseconds()
            setFirstInstallationDate(now)
            now
        } else {
            currentValue
        }
    }

    private fun setFirstInstallationDate(timestamp: Long) =
        settings.set(ReviewSettingsFields.FIRST_INSTALLATION_DATE.name, timestamp)

    private fun getReviewRequestCount(): Int =
        settings.getInt(ReviewSettingsFields.REVIEW_REQUEST_COUNT.name, 0)

    private fun incrementReviewRequestCount() {
        val currentCount = getReviewRequestCount()
        settings[ReviewSettingsFields.REVIEW_REQUEST_COUNT.name] = currentCount + 1
    }

    private fun getLastReviewRequestDate(): Long =
        settings.getLong(ReviewSettingsFields.LAST_REVIEW_REQUEST_DATE.name, 0L)

    private fun setLastReviewRequestDate(timestamp: Long) =
        settings.set(ReviewSettingsFields.LAST_REVIEW_REQUEST_DATE.name, timestamp)

    private fun getLastReviewVersion(): String? =
        settings.getStringOrNull(ReviewSettingsFields.LAST_REVIEW_VERSION.name)

    private fun setLastReviewVersion(version: String) =
        settings.set(ReviewSettingsFields.LAST_REVIEW_VERSION.name, version)

    private companion object {
        const val MAX_REVIEW_REQUESTS = 3
        const val DAYS_BEFORE_FIRST_REVIEW = 2L
    }
}

private enum class ReviewSettingsFields {
    FIRST_INSTALLATION_DATE,
    REVIEW_REQUEST_COUNT,
    LAST_REVIEW_REQUEST_DATE,
    LAST_REVIEW_VERSION,
}
