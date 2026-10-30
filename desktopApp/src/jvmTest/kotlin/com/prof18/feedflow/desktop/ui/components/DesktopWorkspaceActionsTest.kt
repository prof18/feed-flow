package com.prof18.feedflow.desktop.ui.components

import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.core.utils.getDesktopOS
import com.prof18.feedflow.core.utils.isMacOs
import com.prof18.feedflow.desktop.ui.LocalFeedFlowStrings
import com.prof18.feedflow.i18n.EnFeedFlowStrings
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals

class DesktopWorkspaceActionsTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun `refresh dispatches once and loading also gates header double click`() {
        val refreshing = mutableStateOf(false)
        var refreshes = 0
        var scrolls = 0
        rule.setContent {
            CompositionLocalProvider(LocalFeedFlowStrings provides EnFeedFlowStrings) {
                FeedFlowTheme(darkTheme = false, useOledTheme = false) {
                    HomeToolbar(
                        currentFeedFilter = FeedFilter.Timeline,
                        unReadCount = 0,
                        isUnreadCountHidden = false,
                        showDrawerMenu = true,
                        isDrawerOpen = false,
                        onDrawerMenuClick = {},
                        onClick = { scrolls++ },
                        onDoubleClick = {
                            scrolls++
                            refreshes++
                        },
                        onSearchClick = {},
                        onRefreshClick = { refreshes++ },
                        isRefreshing = refreshing.value,
                        modifier = Modifier.requiredSize(width = 500.dp, height = 56.dp).testTag("toolbar"),
                    )
                }
            }
        }
        val shortcut = if (getDesktopOS().isMacOs()) " (⌘R)" else " (F5)"
        val refresh = rule.onNodeWithContentDescription(EnFeedFlowStrings.refreshFeeds + shortcut)
        refresh.performClick()
        rule.runOnIdle { assertEquals(1, refreshes) }
        rule.onNodeWithTag("toolbar").performMouseInput { doubleClick(Offset(x = 200f, y = 28f)) }
        rule.runOnIdle {
            assertEquals(2, refreshes)
            refreshing.value = true
        }
        refresh.assertIsNotEnabled()
        rule.onNodeWithTag("toolbar").performMouseInput { doubleClick(Offset(x = 200f, y = 28f)) }
        rule.runOnIdle {
            assertEquals(2, refreshes)
            assertEquals(2, scrolls)
        }
    }

    @Test
    fun `bulk operation disables refresh gestures even without an active refresh`() {
        var refreshes = 0
        rule.setContent {
            CompositionLocalProvider(LocalFeedFlowStrings provides EnFeedFlowStrings) {
                FeedFlowTheme(darkTheme = true, useOledTheme = true) {
                    HomeToolbar(
                        currentFeedFilter = FeedFilter.Timeline,
                        unReadCount = 0,
                        isUnreadCountHidden = false,
                        showDrawerMenu = false,
                        isDrawerOpen = false,
                        onDrawerMenuClick = {},
                        onClick = {},
                        onDoubleClick = { refreshes++ },
                        onSearchClick = {},
                        onRefreshClick = { refreshes++ },
                        isRefreshing = false,
                        refreshEnabled = false,
                        modifier = Modifier.requiredSize(width = 360.dp, height = 56.dp).testTag("toolbar"),
                    )
                }
            }
        }
        val shortcut = if (getDesktopOS().isMacOs()) " (⌘R)" else " (F5)"
        rule.onNodeWithContentDescription(EnFeedFlowStrings.refreshFeeds + shortcut).assertIsNotEnabled()
        rule.onNodeWithTag("toolbar").performMouseInput { doubleClick(Offset(x = 140f, y = 28f)) }
        rule.runOnIdle { assertEquals(0, refreshes) }
    }
}
