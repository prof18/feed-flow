package com.prof18.feedflow.desktop.home

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.MouseButton
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performMouseInput
import com.prof18.feedflow.core.model.ArticleOpenMode
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.desktop.home.drawer.DefaultDrawerItemVisualStyle
import com.prof18.feedflow.desktop.home.drawer.DesktopFeedSourceDrawerItem
import com.prof18.feedflow.desktop.ui.LocalFeedFlowStrings
import com.prof18.feedflow.desktop.ui.components.FeedFlowTheme
import com.prof18.feedflow.i18n.EnFeedFlowStrings
import org.jetbrains.jewel.ui.component.Text
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals

class FeedSourceDrawerPointerTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun `primary click activates feed once and secondary menu does not activate it`() {
        var activations = 0
        var edits = 0
        val source = FeedSource(
            id = "source", url = "https://example.com/feed.xml", title = "Example Feed",
            category = null, lastSyncTimestamp = null, logoUrl = null, websiteUrl = null,
            fetchFailed = false, articleOpenMode = ArticleOpenMode.DEFAULT,
            isHiddenFromTimeline = false, isPinned = false, isNotificationEnabled = false,
            isHideImagesEnabled = false,
        )
        rule.setContent {
            CompositionLocalProvider(LocalFeedFlowStrings provides EnFeedFlowStrings) {
                FeedFlowTheme(darkTheme = false, useOledTheme = false) {
                    DesktopFeedSourceDrawerItem(
                        feedSource = source,
                        label = { Text(source.title) },
                        selected = false,
                        drawerItemVisualStyle = DefaultDrawerItemVisualStyle,
                        isMultiSelected = false,
                        onClick = { activations++ },
                        icon = {},
                        onEditFeedClick = { edits++ },
                        onDeleteFeedSourceClick = {},
                        onPinFeedClick = {},
                        onChangeFeedCategoryClick = {},
                        onOpenWebsite = {},
                        onMarkAllReadForFeedSourceClick = {},
                        unreadCount = 0,
                        modifier = Modifier.testTag("feed-row"),
                    )
                }
            }
        }
        rule.onNodeWithTag("feed-row").performMouseInput { click() }
        rule.runOnIdle { assertEquals(1, activations) }
        rule.onNodeWithTag("feed-row").performMouseInput { click(button = MouseButton.Secondary) }
        rule.onNodeWithText(EnFeedFlowStrings.openFeedSettings).performMouseInput { click() }
        rule.runOnIdle {
            assertEquals(1, activations)
            assertEquals(1, edits)
        }
    }
}
