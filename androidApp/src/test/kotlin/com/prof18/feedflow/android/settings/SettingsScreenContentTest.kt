package com.prof18.feedflow.android.settings

import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import com.prof18.feedflow.android.ui.theme.FeedFlowTheme
import com.prof18.feedflow.android.ui.utils.ProvideFeedFlowStrings
import com.prof18.feedflow.android.ui.utils.rememberFeedFlowStrings
import com.prof18.feedflow.i18n.EnFeedFlowStrings
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SettingsScreenContentTest {
    @get:Rule
    val compose = createComposeRule()

    private val clicks = mutableMapOf<String, Int>()

    @Test
    fun showsSectionHeaders() {
        setContent()

        listOf(
            EnFeedFlowStrings.feedsTitle,
            EnFeedFlowStrings.settingsSyncAndStorage,
            EnFeedFlowStrings.settingsSectionDisplayAndReading,
        ).forEach { text ->
            assertExistsAfterScroll(hasText(text) and isHeading())
        }
    }

    @Test
    fun rowsOpenTheirPages() {
        setContent()

        listOf(
            "settings_sync",
            "settings_storage",
            "settings_notifications",
            "feed_source_list_settings_row",
            "settings_import_export",
            "settings_blocked_words",
        ).forEach { tag ->
            compose.onNodeWithTag(SettingsE2eIds.ROOT_LIST).performScrollToNode(hasTestTag(tag))
            compose.onNodeWithTag(tag).performClick()
            assertEquals("Click count for $tag", 1, clicks[tag])
        }
    }

    @Test
    fun oldRowsAreGone() {
        setContent()

        compose.onAllNodesWithTag("settings_feeds_accounts").assertCountEquals(0)
        compose.onAllNodesWithTag("settings_sync_storage").assertCountEquals(0)
    }

    @Test
    fun syncRowShowsSubtitle() {
        setContent()

        assertExistsAfterScroll(hasText(EnFeedFlowStrings.settingsSyncAndRefreshSubtitle))
    }

    private fun assertExistsAfterScroll(matcher: SemanticsMatcher) {
        compose.onNodeWithTag(SettingsE2eIds.ROOT_LIST).performScrollToNode(matcher)
        compose.onNode(matcher).assertExists()
    }

    private fun click(tag: String): () -> Unit = { clicks[tag] = (clicks[tag] ?: 0) + 1 }

    private fun setContent() {
        compose.setContent {
            FeedFlowTheme {
                ProvideFeedFlowStrings(rememberFeedFlowStrings()) {
                    SettingsScreenContent(
                        appVersion = "1.0.0",
                        navigateBack = {},
                        navigateToFeeds = click("feed_source_list_settings_row"),
                        navigateToAddFeed = {},
                        navigateToImportExport = click("settings_import_export"),
                        navigateToBlockedWords = click("settings_blocked_words"),
                        navigateToSyncAndRefresh = click("settings_sync"),
                        navigateToNotifications = click("settings_notifications"),
                        navigateToStorage = click("settings_storage"),
                        navigateToAppearance = {},
                        navigateToFeedListSettings = {},
                        navigateToReadingBehavior = {},
                        navigateToWidgetSettings = {},
                        navigateToAboutAndSupport = {},
                        onSupportProjectClick = {},
                        showWidgetSettings = true,
                        showSupportProject = true,
                    )
                }
            }
        }
    }
}
