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
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasScrollAction
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
    fun `opening a service shows its subpage without connecting it`() {
        val currentAccount = mutableStateOf(SyncAccounts.LOCAL)
        compose.setContent { TestContent(currentAccount = currentAccount.value) }

        compose.onNodeWithTag(AccountE2eIds.providerRow(SyncAccounts.MINIFLUX)).performClick()

        compose.onNodeWithTag(DETAIL_TAG).assertTextEquals(SyncAccounts.MINIFLUX.name)
        compose.onNodeWithTag(AccountE2eIds.providerRow(SyncAccounts.FRESH_RSS)).assertDoesNotExist()
        compose.runOnIdle { assertEquals(SyncAccounts.LOCAL, currentAccount.value) }
    }

    @Test
    fun `keyboard service selection opens the same details as pointer selection`() {
        compose.setContent { TestContent() }
        val row = compose.onNodeWithTag(AccountE2eIds.providerRow(SyncAccounts.MINIFLUX))
        row.requestFocus()
        row.performKeyInput { pressKey(Key.Enter) }

        compose.onNodeWithTag(DETAIL_TAG).assertTextEquals(SyncAccounts.MINIFLUX.name)
    }

    @Test
    fun `unsupported services are omitted from navigation`() {
        compose.setContent { TestContent(supportedAccounts = persistentListOf(SyncAccounts.FRESH_RSS)) }

        compose.onNodeWithTag(AccountE2eIds.providerRow(SyncAccounts.ICLOUD)).assertDoesNotExist()
        compose.onNodeWithTag(AccountE2eIds.providerRow(SyncAccounts.MINIFLUX)).assertDoesNotExist()
    }

    @Test
    fun `disabled service cannot open while the current account remains reachable`() {
        compose.setContent { TestContent(currentAccount = SyncAccounts.FRESH_RSS) }
        val disabled = compose.onNodeWithTag(AccountE2eIds.providerRow(SyncAccounts.MINIFLUX))
        disabled.assertIsNotEnabled()
        disabled.performTouchInput { click() }
        compose.onNodeWithTag(DETAIL_TAG).assertDoesNotExist()

        compose.onNodeWithTag(AccountE2eIds.providerRow(SyncAccounts.FRESH_RSS)).performClick()
        compose.onNodeWithTag(DETAIL_TAG).assertTextEquals(SyncAccounts.FRESH_RSS.name)
    }

    @Test
    fun `overview scroll position survives opening a lower service and Back`() {
        val accounts = persistentListOf(
            SyncAccounts.ICLOUD,
            SyncAccounts.DROPBOX,
            SyncAccounts.GOOGLE_DRIVE,
            SyncAccounts.FRESH_RSS,
            SyncAccounts.MINIFLUX,
            SyncAccounts.FEEDBIN,
            SyncAccounts.BAZQUX,
        )
        compose.setContent { TestContent(supportedAccounts = accounts, short = true) }
        val page = compose.onNode(scrollablePage)
        page.performScrollToNode(hasTestTag(AccountE2eIds.providerRow(SyncAccounts.ICLOUD)))
        val anchor = page.fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange].value()
        assertTrue(anchor > 0)
        compose.onNodeWithTag(AccountE2eIds.providerRow(SyncAccounts.ICLOUD)).performClick()
        compose.onNodeWithTag(DETAIL_TAG).assertTextEquals(SyncAccounts.ICLOUD.name)
        compose.onNodeWithText(EnFeedFlowStrings.desktopAccountsBackToServices).performClick()

        val restored = compose.onNode(scrollablePage)
            .fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange].value()
        assertEquals(anchor, restored)
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
    fun `account credential drafts survive leaving and returning to settings page`() {
        var draft: CredentialsState? = null
        compose.setContent {
            TestTheme {
                val drafts = rememberAccountDrafts()
                SettingsContent(DesktopSettingsCategory.ACCOUNTS) { category ->
                    if (category == DesktopSettingsCategory.ACCOUNTS) {
                        DesktopAccountsContent(
                            currentAccount = SyncAccounts.LOCAL,
                            supportedAccounts = SUPPORTED_ACCOUNTS,
                            drafts = drafts,
                        ) { _, _, _, credentials ->
                            draft = credentials
                            Text(credentials.username.text, Modifier.testTag("account-username-draft"))
                        }
                    } else {
                        Text(category.name)
                    }
                }
            }
        }
        compose.onNodeWithTag(AccountE2eIds.providerRow(SyncAccounts.MINIFLUX)).performClick()
        compose.runOnIdle {
            requireNotNull(draft).username = androidx.compose.ui.text.input.TextFieldValue("fixture-user")
        }
        compose.onNodeWithTag("account-username-draft").assertTextEquals("fixture-user")
        compose.onNodeWithText(EnFeedFlowStrings.settingsFeedListTitle).performClick()
        compose.onNodeWithText(EnFeedFlowStrings.settingsAccounts).performClick()
        compose.onNodeWithTag("account-username-draft").assertTextEquals("fixture-user")
    }

    @Test
    fun `Back returns from a service subpage to the overview`() {
        compose.setContent { TestContent() }
        compose.onNodeWithTag(AccountE2eIds.providerRow(SyncAccounts.MINIFLUX)).performClick()
        compose.onNodeWithTag(DETAIL_TAG).assertExists()

        compose.onNodeWithText(EnFeedFlowStrings.desktopAccountsBackToServices).performClick()

        compose.onNodeWithTag(DETAIL_TAG).assertDoesNotExist()
        compose.onNodeWithTag(AccountE2eIds.providerRow(SyncAccounts.MINIFLUX)).assertExists()
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
        short: Boolean = false,
    ) {
        TestTheme {
            Box(Modifier.requiredSize(width = 900.dp, height = if (short) 360.dp else 600.dp)) {
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

        // Jewel's scrollbar is scrollable too, but only the page itself reports a scroll range.
        val scrollablePage =
            hasScrollAction() and
                SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange)
    }
}
