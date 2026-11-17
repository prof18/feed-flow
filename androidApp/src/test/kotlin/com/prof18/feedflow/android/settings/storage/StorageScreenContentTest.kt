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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h2000dp")
class StorageScreenContentTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun showsStorageRowsOnly() {
        setStorageContent()

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

    @Test
    fun showsOfflineCopiesSectionWithDescriptions() {
        setStorageContent()

        compose.onNodeWithText(EnFeedFlowStrings.settingsOfflineCopiesSectionTitle).assertExists()
        compose.onNodeWithTag("storage_save_content").assertExists()
        compose.onNodeWithTag("storage_prefetch_content").assertExists()
        compose.onNodeWithText(EnFeedFlowStrings.settingsSaveReaderModeContentDescription).assertExists()
        compose.onNodeWithText(EnFeedFlowStrings.settingsPrefetchArticleContentDescription).assertExists()
        compose.onNodeWithText(EnFeedFlowStrings.settingsClearDownloadedArticlesDescription).assertExists()
        compose.onNodeWithText(EnFeedFlowStrings.settingsAutoDeleteDescription).assertExists()
    }

    @Test
    fun tappingSaveContentTurnsItOn() {
        var saveValue: Boolean? = null
        setStorageContent(onSaveReaderModeContentChange = { saveValue = it })

        compose.onNodeWithTag("storage_save_content").performClick()

        compose.runOnIdle { assertEquals(true, saveValue) }
    }

    @Test
    fun turningOnPrefetchAsksForConfirmation() {
        var prefetchValue: Boolean? = null
        setStorageContent(onPrefetchArticleContentChange = { prefetchValue = it })

        compose.onNodeWithTag("storage_prefetch_content").performClick()
        compose.onNodeWithText(EnFeedFlowStrings.settingsPrefetchArticleContentWarning).assertExists()
        compose.runOnIdle { assertEquals(null, prefetchValue) }
        compose.onNodeWithText(EnFeedFlowStrings.confirmButton).performClick()

        compose.runOnIdle { assertEquals(true, prefetchValue) }
    }

    private fun setStorageContent(
        onClearImageCache: () -> Unit = {},
        onSaveReaderModeContentChange: (Boolean) -> Unit = {},
        onPrefetchArticleContentChange: (Boolean) -> Unit = {},
    ) {
        compose.setContent {
            FeedFlowTheme {
                ProvideFeedFlowStrings(rememberFeedFlowStrings()) {
                    StorageScreenContent(
                        autoDeletePeriod = AutoDeletePeriod.ONE_WEEK,
                        navigateBack = {},
                        onAutoDeletePeriodSelected = {},
                        isSaveReaderModeContentEnabled = false,
                        isPrefetchArticleContentEnabled = false,
                        onSaveReaderModeContentChange = onSaveReaderModeContentChange,
                        onPrefetchArticleContentChange = onPrefetchArticleContentChange,
                        onClearDownloadedArticles = {},
                        onClearImageCache = onClearImageCache,
                    )
                }
            }
        }
    }
}
