package com.prof18.feedflow.android.settings.storage

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.prof18.feedflow.android.ui.theme.FeedFlowTheme
import com.prof18.feedflow.android.ui.utils.ProvideFeedFlowStrings
import com.prof18.feedflow.android.ui.utils.rememberFeedFlowStrings
import com.prof18.feedflow.core.model.AutoDeletePeriod
import com.prof18.feedflow.i18n.EnFeedFlowStrings
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class StorageScreenContentTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun showsStorageRowsOnly() {
        compose.setContent {
            FeedFlowTheme {
                ProvideFeedFlowStrings(rememberFeedFlowStrings()) {
                    StorageScreenContent(
                        autoDeletePeriod = AutoDeletePeriod.ONE_WEEK,
                        navigateBack = {},
                        onAutoDeletePeriodSelected = {},
                        onClearDownloadedArticles = {},
                        onClearImageCache = {},
                    )
                }
            }
        }

        compose.onNodeWithTag("sync_storage_auto_delete").assertExists()
        compose.onNodeWithTag("sync_storage_clear_downloaded").assertExists()
        compose.onAllNodesWithTag("sync_storage_refresh_on_launch").assertCountEquals(0)
        compose.onNodeWithText(EnFeedFlowStrings.settingsStorage).assertExists()
    }

    @Test
    fun showsClearDataHeaderAndNoDanger() {
        setStorageContent()

        compose.onNodeWithText(EnFeedFlowStrings.settingsClearDataTitle).assertExists()
        compose.onAllNodesWithText("Danger").assertCountEquals(0)
    }

    @Test
    fun confirmingImageCacheCallsCallback() {
        var cleared = false
        setStorageContent(onClearImageCache = { cleared = true })

        compose.onNodeWithText(EnFeedFlowStrings.settingsClearImageCache).performClick()
        compose.onNodeWithText(EnFeedFlowStrings.confirmButton).performClick()

        compose.runOnIdle { assertTrue(cleared) }
    }

    private fun setStorageContent(onClearImageCache: () -> Unit = {}) {
        compose.setContent {
            FeedFlowTheme {
                ProvideFeedFlowStrings(rememberFeedFlowStrings()) {
                    StorageScreenContent(
                        autoDeletePeriod = AutoDeletePeriod.ONE_WEEK,
                        navigateBack = {},
                        onAutoDeletePeriodSelected = {},
                        onClearDownloadedArticles = {},
                        onClearImageCache = onClearImageCache,
                    )
                }
            }
        }
    }
}
