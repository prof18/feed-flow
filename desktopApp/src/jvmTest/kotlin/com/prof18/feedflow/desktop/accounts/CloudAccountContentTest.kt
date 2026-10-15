package com.prof18.feedflow.desktop.accounts

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import com.prof18.feedflow.core.model.AccountConnectionUiState
import com.prof18.feedflow.core.model.AccountSyncUIState
import com.prof18.feedflow.desktop.testing.AccountE2eIds
import com.prof18.feedflow.desktop.ui.LocalFeedFlowStrings
import com.prof18.feedflow.desktop.ui.components.FeedFlowTheme
import com.prof18.feedflow.i18n.EnFeedFlowStrings
import org.jetbrains.jewel.ui.component.OutlinedButton
import org.jetbrains.jewel.ui.component.Text
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals

class CloudAccountContentTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun `backup and disconnect stay disabled during active sync`() {
        var backupRequests = 0
        var disconnectRequests = 0
        rule.setContent {
            CompositionLocalProvider(LocalFeedFlowStrings provides EnFeedFlowStrings) {
                FeedFlowTheme(darkTheme = false, useOledTheme = false) {
                    CloudAccountContent(
                        description = "",
                        successMessage = "Account connected",
                        noSyncMessage = "No sync yet",
                        uiState = AccountConnectionUiState.Linked(AccountSyncUIState.Loading),
                        onBackupClick = { backupRequests++ },
                        onDisconnectClick = { disconnectRequests++ },
                        connectContent = {},
                    )
                }
            }
        }

        rule.onNodeWithText("Account connected").assertExists()
        rule.onNodeWithText(EnFeedFlowStrings.accountRefreshProgress).assertExists()
        rule.onNodeWithTag("account_backup_button").assertIsNotEnabled()
        rule.onNodeWithTag(AccountE2eIds.DISCONNECT_BUTTON).assertIsNotEnabled()
        rule.onNodeWithTag("account_backup_button")
            .performSemanticsAction(SemanticsActions.OnClick) { it() }
        rule.onNodeWithTag(AccountE2eIds.DISCONNECT_BUTTON)
            .performSemanticsAction(SemanticsActions.OnClick) { it() }
        rule.runOnIdle {
            assertEquals(0, backupRequests)
            assertEquals(0, disconnectRequests)
        }
    }

    @Test
    fun `disconnect asks for confirmation and cancel preserves account`() {
        var disconnectRequests = 0
        rule.setContent {
            CompositionLocalProvider(LocalFeedFlowStrings provides EnFeedFlowStrings) {
                FeedFlowTheme(darkTheme = false, useOledTheme = false) {
                    CloudAccountContent(
                        description = "",
                        successMessage = "Account connected",
                        noSyncMessage = "No sync yet",
                        uiState = AccountConnectionUiState.Linked(AccountSyncUIState.None),
                        onBackupClick = null,
                        onDisconnectClick = { disconnectRequests++ },
                        connectContent = {},
                    )
                }
            }
        }

        rule.onNodeWithTag(AccountE2eIds.DISCONNECT_BUTTON).performClick()
        rule.onNodeWithTag("confirmation-title").assertTextEquals(EnFeedFlowStrings.desktopAccountDisconnectTitle)
        rule.onNodeWithText(EnFeedFlowStrings.cancelButton)
            .performSemanticsAction(SemanticsActions.OnClick) { it() }
        rule.onNodeWithTag("confirmation-title").assertDoesNotExist()
        rule.runOnIdle { assertEquals(0, disconnectRequests) }
    }

    @Test
    fun `disconnect confirmation invokes callback once`() {
        var disconnectRequests = 0
        rule.setContent {
            CompositionLocalProvider(LocalFeedFlowStrings provides EnFeedFlowStrings) {
                FeedFlowTheme(darkTheme = false, useOledTheme = false) {
                    CloudAccountContent(
                        description = "",
                        successMessage = "Account connected",
                        noSyncMessage = "No sync yet",
                        uiState = AccountConnectionUiState.Linked(AccountSyncUIState.None),
                        onBackupClick = null,
                        onDisconnectClick = { disconnectRequests++ },
                        connectContent = {},
                    )
                }
            }
        }

        rule.onNodeWithTag(AccountE2eIds.DISCONNECT_BUTTON).performClick()
        rule.onNodeWithTag("confirmation-title").assertTextEquals(EnFeedFlowStrings.desktopAccountDisconnectTitle)
        val confirmAction = rule.onAllNodesWithText(EnFeedFlowStrings.accountDisconnectButton)[1]
            .fetchSemanticsNode().config[SemanticsActions.OnClick].action
        rule.runOnIdle {
            requireNotNull(confirmAction).invoke()
            confirmAction.invoke()
            assertEquals(1, disconnectRequests)
        }
        rule.onNodeWithTag("confirmation-title").assertDoesNotExist()
    }

    @Test
    fun `confirmation cannot disconnect after sync becomes busy`() {
        var disconnectRequests = 0
        val uiState = mutableStateOf<AccountConnectionUiState>(
            AccountConnectionUiState.Linked(AccountSyncUIState.None),
        )
        rule.setContent {
            CompositionLocalProvider(LocalFeedFlowStrings provides EnFeedFlowStrings) {
                FeedFlowTheme(darkTheme = false, useOledTheme = false) {
                    CloudAccountContent(
                        description = "",
                        successMessage = "Account connected",
                        noSyncMessage = "No sync yet",
                        uiState = uiState.value,
                        onBackupClick = null,
                        onDisconnectClick = { disconnectRequests++ },
                        connectContent = {},
                    )
                }
            }
        }

        rule.onNodeWithTag(AccountE2eIds.DISCONNECT_BUTTON).performClick()
        rule.onNodeWithTag("confirmation-title").assertTextEquals(EnFeedFlowStrings.desktopAccountDisconnectTitle)
        rule.runOnIdle { uiState.value = AccountConnectionUiState.Linked(AccountSyncUIState.Loading) }
        val confirmButtons = rule.onAllNodesWithText(EnFeedFlowStrings.accountDisconnectButton)
        if (confirmButtons.fetchSemanticsNodes().size > 1) confirmButtons[1].performClick()
        rule.runOnIdle { assertEquals(0, disconnectRequests) }
    }

    @Test
    fun `synced account shows only available timestamps`() {
        rule.setContent {
            CompositionLocalProvider(LocalFeedFlowStrings provides EnFeedFlowStrings) {
                FeedFlowTheme(darkTheme = false, useOledTheme = false) {
                    CloudAccountContent(
                        description = "",
                        successMessage = "Account connected",
                        noSyncMessage = "No sync yet",
                        uiState = AccountConnectionUiState.Linked(
                            AccountSyncUIState.Synced(lastDownloadDate = "Today", lastUploadDate = null),
                        ),
                        onBackupClick = null,
                        onDisconnectClick = {},
                        lastUploadMessage = { "Uploaded $it" },
                        lastDownloadMessage = { "Downloaded $it" },
                        connectContent = {},
                    )
                }
            }
        }

        rule.onNodeWithText("Downloaded Today").assertExists()
        rule.onNodeWithText("Uploaded Today").assertDoesNotExist()
    }

    @Test
    fun `connect content is visible only for unlinked account state`() {
        val uiState = mutableStateOf<AccountConnectionUiState>(AccountConnectionUiState.Unlinked)
        rule.setContent {
            CompositionLocalProvider(LocalFeedFlowStrings provides EnFeedFlowStrings) {
                FeedFlowTheme(darkTheme = false, useOledTheme = false) {
                    CloudAccountContent(
                        description = "Provider description",
                        successMessage = "Account connected",
                        noSyncMessage = "No sync yet",
                        uiState = uiState.value,
                        onBackupClick = null,
                        onDisconnectClick = {},
                        connectContent = {
                            OutlinedButton(
                                onClick = {},
                                modifier = Modifier.testTag("provider-connect"),
                            ) {
                                Text(EnFeedFlowStrings.accountConnectButton)
                            }
                        },
                    )
                }
            }
        }

        rule.onNodeWithTag("provider-connect").assertExists()
        rule.runOnIdle { uiState.value = AccountConnectionUiState.Loading }
        rule.onNodeWithTag("provider-connect").assertDoesNotExist()
    }
}
