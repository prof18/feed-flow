package com.prof18.feedflow.android.readermode

import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import com.prof18.feedflow.android.MainActivity
import com.prof18.feedflow.android.feedverification.FeedVerificationE2eIds
import com.prof18.feedflow.android.ui.theme.FeedFlowTheme
import com.prof18.feedflow.android.ui.utils.LocalFeedFlowStrings
import com.prof18.feedflow.android.ui.utils.ProvideFeedFlowStrings
import com.prof18.feedflow.android.ui.utils.rememberFeedFlowStrings
import com.prof18.feedflow.core.model.ArticleOpenMode
import com.prof18.feedflow.core.model.FeedItem
import com.prof18.feedflow.core.model.FeedItemId
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.ParsedFeedSource
import com.prof18.feedflow.core.model.ReaderModeData
import com.prof18.feedflow.core.model.ReaderModeState
import com.prof18.feedflow.core.model.ThemeMode
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.shared.presentation.ReaderModeViewModel
import com.prof18.feedflow.shared.presentation.ReaderVerificationState
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.koin.androidx.viewmodel.ext.android.getViewModel
import org.koin.core.context.GlobalContext
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

class ReaderVerificationUiTest {

    @Test
    fun confirmedChallengeOpensExplanationAndKeepsFeedReaderContent() {
        val verifies = AtomicInteger()
        val readerOptionsHint = AtomicReference<String>()
        val verificationUrl = mutableStateOf<String?>(null)
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.setContent {
                    Box(Modifier.fillMaxSize().semantics { testTagsAsResourceId = true }) {
                        FeedFlowTheme {
                            val strings = rememberFeedFlowStrings()
                            ProvideFeedFlowStrings(strings) {
                                readerOptionsHint.set(LocalFeedFlowStrings.current.readerVerificationContentOptions)
                                ReaderModeScreen(
                                    readerModeState = READER_STATE,
                                    fontSize = 16,
                                    themeMode = ThemeMode.LIGHT,
                                    onUpdateFontSize = {},
                                    lineHeight = 24,
                                    onUpdateLineHeight = {},
                                    onBookmarkClick = { _, _ -> },
                                    navigateBack = {},
                                    canNavigatePrevious = false,
                                    canNavigateNext = false,
                                    onNavigateToPrevious = {},
                                    onNavigateToNext = {},
                                    onToggleContentSource = {},
                                    verificationState = ReaderVerificationState(
                                        verificationUrl = verificationUrl.value,
                                    ),
                                    verificationAvailable = true,
                                    onVerifyWebsite = { verifies.incrementAndGet() },
                                )
                            }
                        }
                    }
                }
            }

            val device = device()
            val articleTag = ReaderModeE2eIds.article(ARTICLE_ID)
            assertNotNull(await(device, articleTag))
            val baselineTitle = requireNotNull(awaitText(device, ARTICLE_TITLE))
            assertNull(device.wait(Until.findObject(By.res(FeedVerificationE2eIds.VERIFY)), UI_TIMEOUT_MS))
            val baselineTitleTop = baselineTitle.visibleBounds.top

            scenario.onActivity { verificationUrl.value = CHALLENGE_URL }
            assertNotNull(await(device, FeedVerificationE2eIds.EXPLANATION))
            assertNotNull(awaitText(device, requireNotNull(readerOptionsHint.get())))
            assertNull(device.wait(Until.findObject(By.res(FeedVerificationE2eIds.VERIFY)), UI_TIMEOUT_MS))
            device.pressBack()
            assertTrue(
                "Verification explanation remained visible after cancellation",
                device.wait(Until.gone(By.res(FeedVerificationE2eIds.EXPLANATION)), UI_TIMEOUT_MS),
            )
            assertNotNull(await(device, articleTag))
            assertEquals(baselineTitleTop, requireNotNull(awaitText(device, ARTICLE_TITLE)).visibleBounds.top)
            assertEquals(0, verifies.get())
            assertNull(device.wait(Until.findObject(By.res(FeedVerificationE2eIds.VERIFY)), UI_TIMEOUT_MS))

            scenario.onActivity { verificationUrl.value = "$CHALLENGE_URL/another" }
            assertNotNull(await(device, FeedVerificationE2eIds.EXPLANATION))
            requireNotNull(await(device, FeedVerificationE2eIds.OPEN)).click()
            assertTrue(
                "Verification explanation remained visible after confirmation",
                device.wait(Until.gone(By.res(FeedVerificationE2eIds.EXPLANATION)), UI_TIMEOUT_MS),
            )
            assertEquals(1, verifies.get())
            assertNotNull(await(device, articleTag))
            assertNull(device.wait(Until.findObject(By.res(FeedVerificationE2eIds.VERIFY)), UI_TIMEOUT_MS))
            assertEquals(baselineTitleTop, requireNotNull(awaitText(device, ARTICLE_TITLE)).visibleBounds.top)
        }
    }

    @Test
    fun recreatingTheActivityKeepsTheOpenArticleAndItsNavigation() {
        val koin = GlobalContext.get()
        val database = koin.get<DatabaseHelper>()
        runBlocking { seedArticles(database) }
        try {
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                requireNotNull(awaitText(device(), "Article $FIRST_ITEM_ID")) { "The article is not listed" }.click()
                assertTrue("The article did not open: ${describe(scenario)}", awaitArticleSelection(scenario))
                scenario.recreate()

                assertTrue(
                    "The article selection was lost on recreation: ${describe(scenario)}",
                    awaitArticleSelection(scenario),
                )
            }
        } finally {
            runBlocking { database.deleteFeedSource(SOURCE_ID) }
        }
    }

    private fun describe(scenario: ActivityScenario<MainActivity>): String {
        var description = ""
        scenario.onActivity { activity ->
            val viewModel = activity.getViewModel<ReaderModeViewModel>()
            description = "article=${viewModel.currentArticleState.value?.id} " +
                "next=${viewModel.canNavigateToNextState.value} " +
                "state=${viewModel.readerModeState.value::class.simpleName}"
        }
        return description
    }

    private fun awaitArticleSelection(scenario: ActivityScenario<MainActivity>): Boolean {
        repeat(SELECTION_POLLS) {
            var selected = false
            scenario.onActivity { activity ->
                val viewModel = activity.getViewModel<ReaderModeViewModel>()
                selected = viewModel.currentArticleState.value?.id == FIRST_ITEM_ID &&
                    viewModel.canNavigateToNextState.value
            }
            if (selected) return true
            Thread.sleep(POLL_MILLIS)
        }
        return false
    }

    private suspend fun seedArticles(database: DatabaseHelper) {
        val source = FeedSource(
            id = SOURCE_ID,
            url = "https://example.com/recreation-feed.xml",
            title = "Recreation fixture",
            category = null,
            lastSyncTimestamp = null,
            logoUrl = null,
            websiteUrl = "https://example.com",
            fetchFailed = false,
            articleOpenMode = ArticleOpenMode.FEED_CONTENT,
            isHiddenFromTimeline = false,
            isPinned = false,
            isNotificationEnabled = false,
            isHideImagesEnabled = false,
        )
        database.insertFeedSource(
            listOf(ParsedFeedSource(source.id, source.url, source.title, null, null, source.websiteUrl)),
        )
        database.insertFeedSourcePreference(
            feedSourceId = source.id,
            articleOpenMode = ArticleOpenMode.FEED_CONTENT,
            isHidden = false,
            isPinned = false,
            isNotificationEnabled = false,
            isHideImagesEnabled = false,
        )
        database.insertFeedItems(
            listOf(
                article(FIRST_ITEM_ID, source, pubDate = NEWER_PUB_DATE),
                article("reader-recreation-second", source, pubDate = OLDER_PUB_DATE),
            ),
            lastSyncTimestamp = 0,
        )
    }

    private fun article(id: String, source: FeedSource, pubDate: Long) = FeedItem(
        id = id,
        url = "https://example.com/$id",
        title = "Article $id",
        subtitle = null,
        content = "<p>${"Feed article body. ".repeat(BODY_REPEATS)}</p>",
        imageUrl = null,
        feedSource = source,
        pubDateMillis = pubDate,
        isRead = false,
        dateString = null,
        commentsUrl = null,
        isBookmarked = false,
    )

    private fun device(): UiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    private fun await(device: UiDevice, resourceId: String): UiObject2? =
        device.wait(Until.findObject(By.res(resourceId)), UI_TIMEOUT_MS)

    private fun awaitText(device: UiDevice, text: String): UiObject2? =
        device.wait(Until.findObject(By.text(text)), UI_TIMEOUT_MS)

    private companion object {
        const val ARTICLE_ID = "reader-verification-fixture"
        const val ARTICLE_TITLE = "Reader verification fixture"
        const val CHALLENGE_URL = "https://example.com/protected-feed"
        const val UI_TIMEOUT_MS = 5_000L
        const val SOURCE_ID = "reader-recreation-source"
        const val FIRST_ITEM_ID = "reader-recreation-first"
        const val NEWER_PUB_DATE = 4_102_444_800_000L
        const val OLDER_PUB_DATE = 4_102_444_700_000L
        const val SELECTION_POLLS = 50
        const val BODY_REPEATS = 20
        const val POLL_MILLIS = 200L
        val READER_STATE = ReaderModeState.Success(
            ReaderModeData(
                id = FeedItemId(ARTICLE_ID),
                title = ARTICLE_TITLE,
                content = "<h1>Article stays open</h1><p>Local reader content fixture.</p>",
                url = "https://example.com/article",
                baseUrl = "https://example.com",
                fontSize = 16,
                lineHeight = 24,
                isBookmarked = false,
            ),
        )
    }
}
