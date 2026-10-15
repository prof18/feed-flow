package com.prof18.feedflow.desktop.accounts

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import com.prof18.feedflow.core.model.AccountConnectionUiState
import com.prof18.feedflow.core.model.AccountSyncUIState
import com.prof18.feedflow.core.model.SyncAccounts
import com.prof18.feedflow.desktop.testing.AccountE2eIds
import com.prof18.feedflow.desktop.ui.LocalFeedFlowStrings
import com.prof18.feedflow.desktop.ui.components.FeedFlowTheme
import com.prof18.feedflow.i18n.EnFeedFlowStrings
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.assertEquals

@RunWith(Parameterized::class)
class ProviderContentTest(private val account: SyncAccounts) {
    @get:Rule
    val compose = createComposeRule()
    private val fake = FakeDesktopAccount(account)

    @Test
    fun `actual provider presentation exposes its fields and forwards one submission`() {
        setContent()
        if (fake.isServer) {
            compose.onNodeWithTag(AccountE2eIds.USERNAME_INPUT).assertExists()
            compose.onNodeWithTag(AccountE2eIds.PASSWORD_INPUT).assertExists()
            if (account == SyncAccounts.FRESH_RSS || account == SyncAccounts.MINIFLUX) {
                compose.onNodeWithTag(AccountE2eIds.SERVER_URL_INPUT).assertExists()
            } else {
                compose.onNodeWithTag(AccountE2eIds.SERVER_URL_INPUT).assertDoesNotExist()
            }
        } else {
            compose.onNodeWithTag(AccountE2eIds.USERNAME_INPUT).assertDoesNotExist()
            compose.onNodeWithTag(AccountE2eIds.PASSWORD_INPUT).assertDoesNotExist()
        }
        prepareConnection()
        val button = compose.onNodeWithTag(connectTag())
        val action = button.fetchSemanticsNode().config[SemanticsActions.OnClick].action
        compose.runOnIdle {
            requireNotNull(action).invoke()
            action.invoke()
            assertEquals(1, fake.requests.get())
            if (fake.isServer) {
                val url = FakeDesktopAccount.ENDPOINT.takeIf {
                    account == SyncAccounts.FRESH_RSS || account == SyncAccounts.MINIFLUX
                }
                assertEquals(
                    listOf(url, FakeDesktopAccount.USERNAME, FakeDesktopAccount.PASSWORD),
                    fake.submittedCredentials,
                )
            }
            if (account == SyncAccounts.DROPBOX) assertEquals(FakeDesktopAccount.CODE, fake.submittedCode)
        }
    }

    @Test
    fun `actual provider presentation supports failure retry linked dates and confirmed disconnect`() {
        setContent()
        prepareConnection()
        connect()
        compose.runOnIdle { fake.fail() }
        compose.onNodeWithText(FakeDesktopAccount.ERROR).assertExists()
        connect()
        compose.runOnIdle {
            assertEquals(2, fake.requests.get())
            fake.link()
        }
        compose.onNodeWithTag(AccountE2eIds.CONNECTED_MESSAGE).assertExists()
        assertEquals(
            if (fake.isServer) 1 else 2,
            compose.onAllNodesWithTag(AccountE2eIds.LAST_SYNC_LABEL).fetchSemanticsNodes().size,
        )
        compose.runOnIdle {
            assertEquals("", fake.draft.password.text)
            assertEquals("", fake.draft.authCode.text)
        }
        compose.onNodeWithTag(AccountE2eIds.DISCONNECT_BUTTON)
            .performSemanticsAction(SemanticsActions.OnClick) { it() }
        compose.onNodeWithText(EnFeedFlowStrings.cancelButton)
            .performSemanticsAction(SemanticsActions.OnClick) { it() }
        compose.runOnIdle { assertEquals(0, fake.disconnects.get()) }
        compose.onNodeWithTag(AccountE2eIds.DISCONNECT_BUTTON)
            .performSemanticsAction(SemanticsActions.OnClick) { it() }
        compose.onAllNodesWithText(EnFeedFlowStrings.accountDisconnectButton)[1]
            .performSemanticsAction(SemanticsActions.OnClick) { it() }
        compose.runOnIdle { assertEquals(1, fake.disconnects.get()) }
        compose.onNodeWithTag(AccountE2eIds.CONNECTED_MESSAGE).assertDoesNotExist()
    }

    @Test
    fun `actual provider presentation disables disconnect during sync and hides connection while restoring`() {
        setContent()
        compose.runOnIdle { fake.uiState.value = AccountConnectionUiState.Loading }
        compose.onNodeWithText(EnFeedFlowStrings.desktopAccountUpdating).assertExists()
        compose.onNodeWithTag(AccountE2eIds.CONNECT_BUTTON).assertDoesNotExist()
        compose.runOnIdle {
            fake.uiState.value = AccountConnectionUiState.Linked(AccountSyncUIState.Loading)
        }
        compose.onNodeWithTag(AccountE2eIds.DISCONNECT_BUTTON).assertIsNotEnabled()
        compose.onNodeWithTag(AccountE2eIds.DISCONNECT_BUTTON)
            .performSemanticsAction(SemanticsActions.OnClick) { it() }
        if (!fake.isServer) compose.onNodeWithTag("account_backup_button").assertIsNotEnabled()
        compose.runOnIdle { assertEquals(0, fake.disconnects.get()) }
    }

    private fun setContent() {
        compose.setContent {
            CompositionLocalProvider(LocalFeedFlowStrings provides EnFeedFlowStrings) {
                FeedFlowTheme(darkTheme = false, useOledTheme = false) {
                    AccountTestContent(fake)
                }
            }
        }
    }

    private fun prepareConnection() {
        compose.runOnIdle { fake.populateDraft() }
        if (account == SyncAccounts.DROPBOX) {
            compose.onNodeWithTag("dropbox-auth-code").assertDoesNotExist()
            compose.onNodeWithTag("dropbox-start-auth").performSemanticsAction(SemanticsActions.OnClick) { it() }
            compose.onNodeWithTag("dropbox-auth-code").assertExists()
        }
    }

    private fun connectTag(): String = if (account == SyncAccounts.DROPBOX) {
        "dropbox-confirm-auth"
    } else {
        AccountE2eIds.CONNECT_BUTTON
    }

    private fun connect() {
        compose.onNodeWithTag(connectTag()).performSemanticsAction(SemanticsActions.OnClick) { it() }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun accounts(): List<Array<SyncAccounts>> = SyncAccounts.entries
            .filter { it != SyncAccounts.LOCAL }.map { arrayOf(it) }
    }
}
