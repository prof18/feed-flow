package com.prof18.feedflow.desktop.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.prof18.feedflow.desktop.ui.LocalFeedFlowStrings
import com.prof18.feedflow.desktop.ui.components.FeedFlowTheme
import com.prof18.feedflow.i18n.EnFeedFlowStrings
import com.prof18.feedflow.shared.domain.model.SyncPeriod
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals

class SettingsSubpagesTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `feeds page links to import export and blocked words`() {
        val opened = mutableListOf<String>()
        setContent {
            FeedsPane(
                onImportExport = { opened += "import" },
                onBlockedWords = { opened += "blocked" },
            )
        }

        compose.onNodeWithText(EnFeedFlowStrings.importExportLabel).performClick()
        compose.onNodeWithText(EnFeedFlowStrings.settingsBlockedWords).performClick()

        assertEquals(listOf("import", "blocked"), opened)
    }

    @Test
    fun `sync page shows the accounts row before the refresh section`() {
        var accountsClicks = 0
        setContent {
            SyncPane(
                currentAccountTitle = "FreshRSS",
                onAccounts = { accountsClicks++ },
                isRefreshFeedsOnLaunchEnabled = true,
                syncPeriod = SyncPeriod.ONE_HOUR,
                onRefreshFeedsOnLaunchToggled = {},
                onSyncPeriodSelected = {},
            )
        }

        compose.onNodeWithText("FreshRSS").assertExists()
        compose.onNodeWithText(EnFeedFlowStrings.settingsRefreshSectionTitle).assertExists()
        val accountsTop = compose.onNodeWithText(EnFeedFlowStrings.settingsAccounts)
            .fetchSemanticsNode().boundsInRoot.top
        val refreshTop = compose.onNodeWithText(EnFeedFlowStrings.settingsRefreshSectionTitle)
            .fetchSemanticsNode().boundsInRoot.top
        assert(accountsTop < refreshTop) { "Accounts must come before the Refresh section" }

        compose.onNodeWithText(EnFeedFlowStrings.settingsAccounts).performClick()
        assertEquals(1, accountsClicks)
    }

    @Test
    fun `subpage back link returns to the parent page`() {
        var backClicks = 0
        setContent {
            SettingsSubpage(backLabel = EnFeedFlowStrings.feedsTitle, onBack = { backClicks++ }) {}
        }

        compose.onNodeWithTag(SETTINGS_BACK_LINK_TAG).performClick()

        assertEquals(1, backClicks)
    }

    private fun setContent(content: @Composable () -> Unit) {
        compose.setContent {
            CompositionLocalProvider(LocalFeedFlowStrings provides EnFeedFlowStrings) {
                FeedFlowTheme(darkTheme = false, useOledTheme = false) {
                    content()
                }
            }
        }
    }
}
