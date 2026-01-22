package com.prof18.feedflow.shared.data

import com.prof18.feedflow.core.model.FeedSourceCategory
import com.prof18.feedflow.core.model.ParsedFeedSource
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.shared.test.KoinTestBase
import com.russhwolf.settings.Settings
import com.russhwolf.settings.set
import kotlinx.coroutines.test.runTest
import org.koin.test.inject
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days

class ReviewRepositoryTest : KoinTestBase() {
    private val repository: ReviewRepository by inject()
    private val settings: Settings by inject()
    private val databaseHelper: DatabaseHelper by inject()

    @Test
    fun `shouldShowReview returns false when there are no feeds`() = runTest {
        assertFalse(repository.shouldShowReview())
    }

    @Test
    fun `shouldShowReview returns false when max review requests reached`() = runTest {
        addFeed()
        repeat(4) { repository.onReviewShown() }

        assertFalse(repository.shouldShowReview())
    }

    @Test
    fun `shouldShowReview returns false when last review version is same as current`() = runTest {
        addFeed()
        repository.onReviewShown()

        assertFalse(repository.shouldShowReview())
    }

    @Test
    fun `shouldShowReview returns true when first install and after 2 days`() = runTest {
        addFeed()
        val twoDaysAgo = Clock.System.now().toEpochMilliseconds() - 3.days.inWholeMilliseconds
        settings[ReviewSettingsFields.FIRST_INSTALLATION_DATE.name] = twoDaysAgo

        assertTrue(repository.shouldShowReview())
    }

    @Test
    fun `shouldShowReview returns true when first install and before 2 days`() = runTest {
        addFeed()
        val today = Clock.System.now().toEpochMilliseconds()
        settings[ReviewSettingsFields.FIRST_INSTALLATION_DATE.name] = today

        assertTrue(repository.shouldShowReview())
    }

    @Test
    fun `shouldShowReview returns true when second request and after 30 days`() = runTest {
        addFeed()
        // First review
        repository.onReviewShown()

        val thirtyOneDaysAgo = Clock.System.now().toEpochMilliseconds() - 32.days.inWholeMilliseconds
        settings[ReviewSettingsFields.LAST_REVIEW_REQUEST_DATE.name] = thirtyOneDaysAgo
        // Reset version so it doesn't fail on version check
        settings[ReviewSettingsFields.LAST_REVIEW_VERSION.name] = "0.0.1"
        settings[ReviewSettingsFields.REVIEW_REQUEST_COUNT.name] = 1

        assertTrue(repository.shouldShowReview())
    }

    @Test
    fun `shouldShowReview returns false when second request and before 30 days`() = runTest {
        addFeed()
        // First review
        repository.onReviewShown()

        val tenDaysAgo = Clock.System.now().toEpochMilliseconds() - 10.days.inWholeMilliseconds
        settings[ReviewSettingsFields.LAST_REVIEW_REQUEST_DATE.name] = tenDaysAgo
        // Reset version so it doesn't fail on version check
        settings[ReviewSettingsFields.LAST_REVIEW_VERSION.name] = "0.0.1"
        settings[ReviewSettingsFields.REVIEW_REQUEST_COUNT.name] = 1

        val firstInstallDate = Clock.System.now().toEpochMilliseconds() - 10.days.inWholeMilliseconds
        settings[ReviewSettingsFields.FIRST_INSTALLATION_DATE.name] = firstInstallDate

        assertFalse(repository.shouldShowReview())
    }

    @Test
    fun `shouldShowReview returns true when third request and after 60 days`() = runTest {
        addFeed()
        // First review
        repository.onReviewShown()
        // Second review
        repository.onReviewShown()

        val sixtyOneDaysAgo = Clock.System.now().toEpochMilliseconds() - 62.days.inWholeMilliseconds
        settings[ReviewSettingsFields.LAST_REVIEW_REQUEST_DATE.name] = sixtyOneDaysAgo
        // Reset version so it doesn't fail on version check
        settings[ReviewSettingsFields.LAST_REVIEW_VERSION.name] = "0.0.1"
        settings[ReviewSettingsFields.REVIEW_REQUEST_COUNT.name] = 2

        assertTrue(repository.shouldShowReview())
    }

    @Test
    fun `shouldShowReview returns false when third request and before 60 days`() = runTest {
        addFeed()
        // First review
        repository.onReviewShown()
        // Second review
        repository.onReviewShown()

        val thirtyDaysAgo = Clock.System.now().toEpochMilliseconds() - 31.days.inWholeMilliseconds
        settings[ReviewSettingsFields.LAST_REVIEW_REQUEST_DATE.name] = thirtyDaysAgo
        // Reset version so it doesn't fail on version check
        settings[ReviewSettingsFields.LAST_REVIEW_VERSION.name] = "0.0.1"
        settings[ReviewSettingsFields.REVIEW_REQUEST_COUNT.name] = 2

        val firstInstallDate = Clock.System.now().toEpochMilliseconds() - 40.days.inWholeMilliseconds
        settings[ReviewSettingsFields.FIRST_INSTALLATION_DATE.name] = firstInstallDate

        assertFalse(repository.shouldShowReview())
    }

    private suspend fun addFeed() {
        val category = FeedSourceCategory("1", "Cat")
        databaseHelper.insertCategories(listOf(category))
        databaseHelper.insertFeedSource(
            listOf(
                ParsedFeedSource(
                    id = "1",
                    url = "https://www.google.it",
                    title = "Google",
                    category = category,
                    logoUrl = null,
                    websiteUrl = null,
                ),
            ),
        )
    }
}
