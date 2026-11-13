package com.prof18.feedflow.android.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.prof18.feedflow.android.ui.theme.FeedFlowTheme
import com.prof18.feedflow.android.ui.utils.ProvideFeedFlowStrings
import com.prof18.feedflow.android.ui.utils.rememberFeedFlowStrings
import com.prof18.feedflow.core.model.FeedFilter
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class HomeAppBarDropdownMenuTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `refresh item refreshes once and closes the menu`() {
        var refreshCount = 0
        var closeCount = 0
        setMenu(
            feedFilter = FeedFilter.Timeline,
            closeMenu = { closeCount++ },
            onRefreshClick = { refreshCount++ },
        )

        compose.onNodeWithTag(HomeToolbarE2eIds.REFRESH_MENU_ITEM).performClick()

        assertEquals(1, refreshCount)
        assertEquals(1, closeCount)
    }

    @Test
    fun `timeline shows mark all as read`() {
        setMenu(feedFilter = FeedFilter.Timeline)

        compose.onNodeWithTag(HomeToolbarE2eIds.MARK_ALL_READ_MENU_ITEM).assertExists()
    }

    @Test
    fun `bookmarks shows mark all as read`() {
        setMenu(feedFilter = FeedFilter.Bookmarks)

        compose.onNodeWithTag(HomeToolbarE2eIds.MARK_ALL_READ_MENU_ITEM).assertExists()
    }

    @Test
    fun `read hides mark all as read and keeps refresh`() {
        setMenu(feedFilter = FeedFilter.Read)

        compose.onNodeWithTag(HomeToolbarE2eIds.MARK_ALL_READ_MENU_ITEM).assertDoesNotExist()
        compose.onNodeWithTag(HomeToolbarE2eIds.REFRESH_MENU_ITEM).assertExists()
    }

    private fun setMenu(
        feedFilter: FeedFilter,
        closeMenu: () -> Unit = {},
        onRefreshClick: () -> Unit = {},
    ) {
        setContent {
            HomeAppBarDropdownMenu(
                showMenu = true,
                feedFilter = feedFilter,
                closeMenu = closeMenu,
                onRefreshClick = onRefreshClick,
                onMarkAllReadClicked = {},
                onClearOldArticlesClicked = {},
                onEditFeedClick = {},
                isSyncUploadRequired = false,
                onBackupClick = {},
                onViewOptionsClick = {},
            )
        }
    }

    private fun setContent(content: @Composable () -> Unit) {
        compose.setContent {
            FeedFlowTheme {
                ProvideFeedFlowStrings(rememberFeedFlowStrings()) { content() }
            }
        }
    }
}
