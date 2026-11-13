package com.prof18.feedflow.android.settings.sync

import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.prof18.feedflow.android.ui.theme.FeedFlowTheme
import com.prof18.feedflow.android.ui.utils.ProvideFeedFlowStrings
import com.prof18.feedflow.android.ui.utils.rememberFeedFlowStrings
import com.prof18.feedflow.core.model.BackgroundSyncRestrictions
import com.prof18.feedflow.i18n.EnFeedFlowStrings
import com.prof18.feedflow.shared.domain.model.SyncPeriod
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SyncAndRefreshScreenContentTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun accountsRowOpensAccounts() {
        var accountsClicks = 0
        setContent(navigateToAccounts = { accountsClicks++ })

        compose.onNodeWithTag("settings_accounts").performClick()

        assertEquals(1, accountsClicks)
    }

    @Test
    fun showsRefreshSectionHeader() {
        setContent()

        compose.onNodeWithText(EnFeedFlowStrings.settingsRefreshSectionTitle).assertExists()
    }

    @Test
    fun restrictionsDisabledWhenNever() {
        setContent(syncPeriod = SyncPeriod.NEVER)

        compose.onNodeWithTag("background_sync_wifi_only").assertIsNotEnabled()
    }

    private fun setContent(
        syncPeriod: SyncPeriod = SyncPeriod.ONE_HOUR,
        navigateToAccounts: () -> Unit = {},
    ) {
        compose.setContent {
            FeedFlowTheme {
                ProvideFeedFlowStrings(rememberFeedFlowStrings()) {
                    SyncAndRefreshScreenContent(
                        syncPeriod = syncPeriod,
                        backgroundSyncRestrictions = BackgroundSyncRestrictions(),
                        refreshFeedsOnLaunch = true,
                        navigateBack = {},
                        navigateToAccounts = navigateToAccounts,
                        onSyncPeriodSelected = {},
                        onSyncOnlyOnWifiToggle = {},
                        onSyncOnlyWhenChargingToggle = {},
                        onRefreshFeedsOnLaunchToggle = {},
                    )
                }
            }
        }
    }
}
