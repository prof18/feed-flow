package com.prof18.feedflow.android.ui.feed

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.prof18.feedflow.android.ui.feed.addfeed.AddFeedContent
import com.prof18.feedflow.android.ui.feed.editfeed.EditFeedContent
import com.prof18.feedflow.android.ui.theme.FeedFlowTheme
import com.prof18.feedflow.android.ui.utils.ProvideFeedFlowStrings
import com.prof18.feedflow.android.ui.utils.rememberFeedFlowStrings
import com.prof18.feedflow.core.model.CategoriesState
import com.prof18.feedflow.core.model.FeedSourceSettings
import com.prof18.feedflow.i18n.EnFeedFlowStrings
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h2000dp")
class FeedFormNotificationToggleTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun addFormAlwaysShowsTheToggle() {
        setAddContent(isNotificationEnabled = false, permissionMissing = false)

        compose.onNodeWithTag(TOGGLE).assertExists()
    }

    @Test
    fun addFormExplainsMissingPermissionWhenOn() {
        setAddContent(isNotificationEnabled = true, permissionMissing = true)

        compose.onNodeWithText(EnFeedFlowStrings.feedNotificationsPermissionNeeded).assertExists()
    }

    @Test
    fun addFormHidesReasonWhenOff() {
        setAddContent(isNotificationEnabled = false, permissionMissing = true)

        compose.onNodeWithText(EnFeedFlowStrings.feedNotificationsPermissionNeeded).assertDoesNotExist()
    }

    @Test
    fun addFormHidesReasonWithPermission() {
        setAddContent(isNotificationEnabled = true, permissionMissing = false)

        compose.onNodeWithText(EnFeedFlowStrings.feedNotificationsPermissionNeeded).assertDoesNotExist()
    }

    @Test
    fun addFormToggleReportsChanges() {
        val changes = mutableListOf<Boolean>()
        setAddContent(isNotificationEnabled = false, permissionMissing = false, onToggle = { changes += it })

        compose.onNodeWithTag(TOGGLE).performClick()

        assertEquals(listOf(true), changes)
    }

    @Test
    fun editFormAlwaysShowsTheToggle() {
        setEditContent(isNotificationEnabled = false, permissionMissing = false)

        compose.onNodeWithTag(TOGGLE).assertExists()
    }

    @Test
    fun editFormExplainsMissingPermissionWhenOn() {
        setEditContent(isNotificationEnabled = true, permissionMissing = true)

        compose.onNodeWithText(EnFeedFlowStrings.feedNotificationsPermissionNeeded).assertExists()
    }

    @Test
    fun editFormHidesReasonWhenOff() {
        setEditContent(isNotificationEnabled = false, permissionMissing = true)

        compose.onNodeWithText(EnFeedFlowStrings.feedNotificationsPermissionNeeded).assertDoesNotExist()
    }

    @Test
    fun editFormHidesReasonWithPermission() {
        setEditContent(isNotificationEnabled = true, permissionMissing = false)

        compose.onNodeWithText(EnFeedFlowStrings.feedNotificationsPermissionNeeded).assertDoesNotExist()
    }

    @Test
    fun editFormToggleReportsChanges() {
        val changes = mutableListOf<Boolean>()
        setEditContent(isNotificationEnabled = false, permissionMissing = false, onToggle = { changes += it })

        compose.onNodeWithTag(TOGGLE).performClick()

        assertEquals(listOf(true), changes)
    }

    private fun setAddContent(
        isNotificationEnabled: Boolean,
        permissionMissing: Boolean,
        onToggle: (Boolean) -> Unit = {},
    ) {
        compose.setContent {
            FeedFlowTheme {
                ProvideFeedFlowStrings(rememberFeedFlowStrings()) {
                    AddFeedContent(
                        feedUrl = "https://www.ablog.com/feed",
                        showError = false,
                        showLoading = false,
                        errorMessage = "",
                        categoriesState = CategoriesState(),
                        onFeedUrlUpdated = {},
                        addFeed = {},
                        onCategorySelectorClick = {},
                        isNotificationEnabled = isNotificationEnabled,
                        notificationPermissionMissing = permissionMissing,
                        onNotificationToggleChanged = onToggle,
                        canForceAdd = false,
                        onForceAddFeed = {},
                    )
                }
            }
        }
    }

    private fun setEditContent(
        isNotificationEnabled: Boolean,
        permissionMissing: Boolean,
        onToggle: (Boolean) -> Unit = {},
    ) {
        compose.setContent {
            FeedFlowTheme {
                ProvideFeedFlowStrings(rememberFeedFlowStrings()) {
                    EditFeedContent(
                        feedUrl = "https://www.ablog.com/feed",
                        feedName = "Feed Name",
                        showError = false,
                        showLoading = false,
                        errorMessage = "",
                        canEditUrl = true,
                        categoriesState = CategoriesState(),
                        feedSourceSettings = FeedSourceSettings(isNotificationEnabled = isNotificationEnabled),
                        onFeedUrlUpdated = {},
                        onFeedNameUpdated = {},
                        articleOpenModeSelector = {},
                        onHiddenToggled = {},
                        onHideImagesToggled = {},
                        onKeepUnreadOnScrollToggled = {},
                        isMarkReadWhenScrollingEnabled = true,
                        onPinnedToggled = {},
                        notificationPermissionMissing = permissionMissing,
                        onNotificationToggleChanged = onToggle,
                        onCategorySelectorClick = {},
                        showDeleteDialog = false,
                        onShowDeleteDialog = {},
                        onDismissDeleteDialog = {},
                        onConfirmDelete = {},
                    )
                }
            }
        }
    }

    private companion object {
        const val TOGGLE = "feed_form_notifications_toggle"
    }
}
