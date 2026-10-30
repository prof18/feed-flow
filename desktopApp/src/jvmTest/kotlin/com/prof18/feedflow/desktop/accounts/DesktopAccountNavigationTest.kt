package com.prof18.feedflow.desktop.accounts

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextInputSelection
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.core.model.AccountConnectionUiState
import com.prof18.feedflow.core.model.SyncAccounts
import com.prof18.feedflow.desktop.accounts.dropbox.DropboxAccountContent
import com.prof18.feedflow.desktop.accounts.googledrive.GoogleDriveAccountContent
import com.prof18.feedflow.desktop.testing.AccountE2eIds
import com.prof18.feedflow.desktop.ui.LocalFeedFlowStrings
import com.prof18.feedflow.desktop.ui.components.FeedFlowTheme
import com.prof18.feedflow.i18n.EnFeedFlowStrings
import kotlinx.collections.immutable.persistentListOf
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals

class DesktopAccountNavigationTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `provider navigation retains separate in-memory drafts and cursor without submitting`() {
        var requests = 0
        compose.setContent {
            CompositionLocalProvider(LocalFeedFlowStrings provides EnFeedFlowStrings) {
                FeedFlowTheme(darkTheme = false, useOledTheme = false) {
                    Box(Modifier.requiredSize(width = 900.dp, height = 600.dp)) {
                        DesktopAccountsContent(
                            currentAccount = SyncAccounts.LOCAL,
                            supportedAccounts = persistentListOf(SyncAccounts.MINIFLUX, SyncAccounts.FRESH_RSS),
                        ) { _, _, _, credentials ->
                            CredentialsForm(
                                serverUrlHint = "Server endpoint",
                                usernameHint = null,
                                passwordHint = null,
                                isLoginLoading = false,
                                hasLocalSubscriptions = false,
                                credentialsState = credentials,
                                onLogin = { _, _, _ -> requests++ },
                            )
                        }
                    }
                }
            }
        }
        compose.onNodeWithTag(AccountE2eIds.providerRow(SyncAccounts.MINIFLUX)).performClick()
        val username = compose.onNodeWithTag(AccountE2eIds.USERNAME_INPUT)
        username.performTextInput("first-reader")
        username.performTextInputSelection(TextRange(CURSOR_POSITION))
        compose.onNodeWithTag(AccountE2eIds.PASSWORD_INPUT).performTextInput("synthetic-password")
        compose.onNodeWithText(EnFeedFlowStrings.desktopAccountsBackToServices).performClick()
        compose.onNodeWithTag(AccountE2eIds.providerRow(SyncAccounts.FRESH_RSS)).performClick()
        username.assertTextEquals("")
        username.performTextInput("second-reader")
        compose.onNodeWithText(EnFeedFlowStrings.desktopAccountsBackToServices).performClick()
        compose.onNodeWithTag(AccountE2eIds.providerRow(SyncAccounts.MINIFLUX)).performClick()
        username.assertTextEquals("first-reader")
        assertEquals(
            TextRange(CURSOR_POSITION),
            username.fetchSemanticsNode().config[SemanticsProperties.TextSelectionRange],
        )
        compose.runOnIdle { assertEquals(0, requests) }
    }

    @Test
    fun `Dropbox authorization draft and cursor survive provider navigation without repeating authorization`() {
        var starts = 0
        var confirmations = 0
        compose.setContent {
            CompositionLocalProvider(LocalFeedFlowStrings provides EnFeedFlowStrings) {
                FeedFlowTheme(darkTheme = false, useOledTheme = false) {
                    Box(Modifier.requiredSize(width = 900.dp, height = 600.dp)) {
                        DesktopAccountsContent(
                            currentAccount = SyncAccounts.LOCAL,
                            supportedAccounts = persistentListOf(SyncAccounts.DROPBOX, SyncAccounts.GOOGLE_DRIVE),
                        ) { provider, _, _, draft ->
                            if (provider == SyncAccounts.DROPBOX) {
                                DropboxAccountContent(
                                    uiState = AccountConnectionUiState.Unlinked,
                                    credentialsState = draft,
                                    onStartAuth = {
                                        starts++
                                        draft.submissionPending = false
                                        draft.authCodeReady = true
                                    },
                                    onConfirmCode = { confirmations++ },
                                    onBackup = {},
                                    onDisconnect = {},
                                )
                            } else {
                                GoogleDriveAccountContent(
                                    uiState = AccountConnectionUiState.Unlinked,
                                    credentialsState = draft,
                                    onConnect = {},
                                    onBackup = {},
                                    onDisconnect = {},
                                )
                            }
                        }
                    }
                }
            }
        }
        compose.onNodeWithTag(AccountE2eIds.providerRow(SyncAccounts.DROPBOX)).performClick()
        compose.onNodeWithTag("dropbox-start-auth").performSemanticsAction(SemanticsActions.OnClick) { it() }
        val code = compose.onNodeWithTag("dropbox-auth-code")
        code.performTextInput("synthetic-code")
        code.performTextInputSelection(TextRange(CURSOR_POSITION))
        compose.onNodeWithText(EnFeedFlowStrings.desktopAccountsBackToServices).performClick()
        compose.onNodeWithTag(AccountE2eIds.providerRow(SyncAccounts.GOOGLE_DRIVE)).performClick()
        code.assertDoesNotExist()
        compose.onNodeWithText(EnFeedFlowStrings.desktopAccountsBackToServices).performClick()
        compose.onNodeWithTag(AccountE2eIds.providerRow(SyncAccounts.DROPBOX)).performClick()
        assertEquals(
            TextRange(CURSOR_POSITION),
            code.fetchSemanticsNode().config[SemanticsProperties.TextSelectionRange],
        )
        compose.onNodeWithTag("dropbox-start-auth").assertDoesNotExist()
        compose.runOnIdle {
            assertEquals(1, starts)
            assertEquals(0, confirmations)
        }
    }
    private companion object {
        const val CURSOR_POSITION = 3
    }
}
