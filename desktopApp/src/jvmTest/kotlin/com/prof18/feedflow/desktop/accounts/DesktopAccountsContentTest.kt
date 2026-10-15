package com.prof18.feedflow.desktop.accounts

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.core.model.SyncAccounts
import com.prof18.feedflow.desktop.settings.DesktopSettingsCategory
import com.prof18.feedflow.desktop.settings.SettingsContent
import com.prof18.feedflow.desktop.testing.AccountE2eIds
import com.prof18.feedflow.desktop.ui.LocalFeedFlowStrings
import com.prof18.feedflow.desktop.ui.components.FeedFlowTheme
import com.prof18.feedflow.i18n.EnFeedFlowStrings
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import org.jetbrains.jewel.ui.component.Text
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DesktopAccountsContentTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `local overview explains that no sync account is connected`() {
        compose.setContent { TestContent() }

        compose.onNodeWithText(EnFeedFlowStrings.desktopAccountsLocalStatus).assertExists()
        compose.onNodeWithTag(AccountE2eIds.providerRow(SyncAccounts.LOCAL)).assertDoesNotExist()
    }

    @Test
    fun `monochrome provider logos remain visible on OLED backgrounds`() {
        val providers = persistentListOf(
            SyncAccounts.DROPBOX,
            SyncAccounts.ICLOUD,
            SyncAccounts.MINIFLUX,
            SyncAccounts.FEEDBIN,
        )
        compose.setContent {
            TestTheme(oled = true) {
                Box(Modifier.requiredSize(width = 900.dp, height = 600.dp)) {
                    AccountsContent(supportedAccounts = providers)
                }
            }
        }

        providers.forEach { account ->
            val pixels = compose.onNodeWithTag("${AccountE2eIds.providerRow(account)}-icon", useUnmergedTree = true)
                .captureToImage().toPixelMap()
            val hasVisibleForeground = (0 until pixels.width).any { x ->
                (0 until pixels.height).any { y ->
                    val pixel = pixels[x, y]
                    pixel.red > MIN_VISIBLE_CHANNEL && pixel.green > MIN_VISIBLE_CHANNEL &&
                        pixel.blue > MIN_VISIBLE_CHANNEL
                }
            }
            assertTrue(hasVisibleForeground, "$account logo is invisible on the OLED background")
        }
    }

    @Test
    fun `connected overview names active account and explains disabled alternatives`() {
        compose.setContent { TestContent(currentAccount = SyncAccounts.FRESH_RSS) }

        compose.onNodeWithTag(ACCOUNT_CURRENT_STATUS_TAG)
            .assertTextEquals(EnFeedFlowStrings.desktopAccountsConnectedStatus)
        compose.onNodeWithText(EnFeedFlowStrings.desktopAccountsSingleProviderExplanation).assertExists()
        compose.onNodeWithTag(AccountE2eIds.providerRow(SyncAccounts.MINIFLUX)).assertIsNotEnabled()
        compose.onNodeWithTag(ACCOUNT_MANAGE_TAG).performClick()
        compose.onNodeWithTag(DETAIL_TAG).assertTextEquals(SyncAccounts.FRESH_RSS.name)
    }

    @Test
    fun `pointer selection shows details without connecting and retains wide service list`() {
        val currentAccount = mutableStateOf(SyncAccounts.LOCAL)
        compose.setContent { TestContent(currentAccount = currentAccount.value) }

        compose.onNodeWithTag(AccountE2eIds.providerRow(SyncAccounts.MINIFLUX)).performClick()

        compose.onNodeWithTag(DETAIL_TAG).assertTextEquals(SyncAccounts.MINIFLUX.name)
        compose.onNodeWithTag(AccountE2eIds.providerRow(SyncAccounts.FRESH_RSS)).assertExists()
        compose.runOnIdle { assertEquals(SyncAccounts.LOCAL, currentAccount.value) }
    }

    @Test
    fun `keyboard service selection opens the same details as pointer selection`() {
        compose.setContent { TestContent() }
        val list = compose.onNodeWithTag(ACCOUNT_PROVIDERS_TAG)
        list.requestFocus()
        list.performKeyInput {
            pressKey(Key.DirectionDown)
            pressKey(Key.Enter)
        }

        compose.onNodeWithTag(DETAIL_TAG).assertTextEquals(SyncAccounts.MINIFLUX.name)
    }

    @Test
    fun `unsupported services are omitted from navigation`() {
        compose.setContent { TestContent(supportedAccounts = persistentListOf(SyncAccounts.FRESH_RSS)) }

        compose.onNodeWithTag(AccountE2eIds.providerRow(SyncAccounts.ICLOUD)).assertDoesNotExist()
        compose.onNodeWithTag(AccountE2eIds.providerRow(SyncAccounts.MINIFLUX)).assertDoesNotExist()
    }

    @Test
    fun `disabled service cannot open by pointer or keyboard`() {
        compose.setContent { TestContent(currentAccount = SyncAccounts.FRESH_RSS) }
        compose.onNodeWithTag(AccountE2eIds.providerRow(SyncAccounts.MINIFLUX)).performTouchInput { click() }
        compose.onNodeWithTag(DETAIL_TAG).assertDoesNotExist()
        val list = compose.onNodeWithTag(ACCOUNT_PROVIDERS_TAG)
        list.requestFocus()
        list.performKeyInput {
            pressKey(Key.DirectionDown)
            pressKey(Key.Enter)
        }

        compose.onNodeWithTag(DETAIL_TAG).assertTextEquals(SyncAccounts.FRESH_RSS.name)
    }

    @Test
    fun `narrow services scroll anchor survives opening a lower service and Back`() {
        val accounts = persistentListOf(
            SyncAccounts.ICLOUD,
            SyncAccounts.DROPBOX,
            SyncAccounts.GOOGLE_DRIVE,
            SyncAccounts.FRESH_RSS,
            SyncAccounts.MINIFLUX,
            SyncAccounts.FEEDBIN,
            SyncAccounts.BAZQUX,
        )
        compose.setContent { TestContent(supportedAccounts = accounts, narrow = true) }
        val list = compose.onNodeWithTag(ACCOUNT_PROVIDERS_TAG)
        list.performScrollToNode(hasTestTag(AccountE2eIds.providerRow(SyncAccounts.BAZQUX)))
        val anchor = list.fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange].value()
        assertTrue(anchor > 0)
        compose.onNodeWithTag(AccountE2eIds.providerRow(SyncAccounts.BAZQUX)).performClick()
        compose.onNodeWithTag(DETAIL_TAG).assertTextEquals(SyncAccounts.BAZQUX.name)
        compose.onNodeWithText(EnFeedFlowStrings.desktopAccountsBackToServices).performClick()

        assertEquals(anchor, list.fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange].value())
    }

    @Test
    fun `returning from another settings category retains selected account detail`() {
        compose.setContent {
            TestTheme {
                SettingsContent(DesktopSettingsCategory.ACCOUNTS) { category ->
                    if (category == DesktopSettingsCategory.ACCOUNTS) {
                        AccountsContent()
                    } else {
                        Text(category.name, Modifier.testTag(OTHER_CATEGORY_TAG))
                    }
                }
            }
        }
        compose.onNodeWithTag(AccountE2eIds.providerRow(SyncAccounts.MINIFLUX)).performClick()
        compose.onNodeWithText(EnFeedFlowStrings.settingsFeedListTitle).performClick()
        compose.onNodeWithTag(OTHER_CATEGORY_TAG).assertTextEquals(DesktopSettingsCategory.FEED_LIST.name)
        compose.onNodeWithText(EnFeedFlowStrings.settingsAccounts).performClick()

        compose.onNodeWithTag(DETAIL_TAG).assertTextEquals(SyncAccounts.MINIFLUX.name)
    }

    @Test
    fun `narrow Back returns to services and resizing restores selected detail`() {
        val narrow = mutableStateOf(false)
        compose.setContent { TestContent(narrow = narrow.value) }
        compose.onNodeWithTag(AccountE2eIds.providerRow(SyncAccounts.MINIFLUX)).performClick()
        compose.runOnIdle { narrow.value = true }
        compose.onNodeWithText(EnFeedFlowStrings.desktopAccountsBackToServices).performClick()
        compose.onNodeWithTag(AccountE2eIds.providerRow(SyncAccounts.MINIFLUX)).assertIsSelected()

        compose.runOnIdle { narrow.value = false }

        compose.onNodeWithTag(DETAIL_TAG).assertTextEquals(SyncAccounts.MINIFLUX.name)
    }

    @Test
    fun `account activation elsewhere cannot leave a disabled provider detail open`() {
        val currentAccount = mutableStateOf(SyncAccounts.LOCAL)
        compose.setContent { TestContent(currentAccount = currentAccount.value) }
        compose.onNodeWithTag(AccountE2eIds.providerRow(SyncAccounts.MINIFLUX)).performClick()

        compose.runOnIdle { currentAccount.value = SyncAccounts.FRESH_RSS }

        compose.onNodeWithTag(DETAIL_TAG).assertDoesNotExist()
        compose.onNodeWithTag(AccountE2eIds.providerRow(SyncAccounts.MINIFLUX)).assertIsNotEnabled()
    }

    @Composable
    private fun TestContent(
        currentAccount: SyncAccounts = SyncAccounts.LOCAL,
        supportedAccounts: ImmutableList<SyncAccounts> = SUPPORTED_ACCOUNTS,
        narrow: Boolean = false,
    ) {
        TestTheme {
            Box(Modifier.requiredSize(width = if (narrow) 480.dp else 900.dp, height = 600.dp)) {
                AccountsContent(currentAccount, supportedAccounts)
            }
        }
    }

    @Composable
    private fun AccountsContent(
        currentAccount: SyncAccounts = SyncAccounts.LOCAL,
        supportedAccounts: ImmutableList<SyncAccounts> = SUPPORTED_ACCOUNTS,
    ) {
        DesktopAccountsContent(
            currentAccount = currentAccount,
            supportedAccounts = supportedAccounts,
        ) { account, _, _, _ ->
            Text(account.name, Modifier.testTag(DETAIL_TAG))
        }
    }

    @Composable
    private fun TestTheme(oled: Boolean = false, content: @Composable () -> Unit) {
        CompositionLocalProvider(LocalFeedFlowStrings provides EnFeedFlowStrings) {
            FeedFlowTheme(darkTheme = oled, useOledTheme = oled, content = content)
        }
    }

    private companion object {
        const val DETAIL_TAG = "account-detail"
        const val OTHER_CATEGORY_TAG = "other-settings-category"
        const val MIN_VISIBLE_CHANNEL = 0.5f
        val SUPPORTED_ACCOUNTS = persistentListOf(SyncAccounts.FRESH_RSS, SyncAccounts.MINIFLUX)
    }
}
