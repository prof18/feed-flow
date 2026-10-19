package com.prof18.feedflow.desktop.accounts

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import com.prof18.feedflow.core.model.AccountConnectionUiState
import com.prof18.feedflow.core.model.AccountSyncUIState
import com.prof18.feedflow.desktop.testing.AccountE2eIds
import com.prof18.feedflow.desktop.ui.LocalFeedFlowStrings
import com.prof18.feedflow.desktop.ui.components.FeedFlowTheme
import com.prof18.feedflow.i18n.EnFeedFlowStrings
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals

class ProviderLifecycleTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `credential submission loading failure retry linked and disconnect lifecycle`() {
        val state = CredentialsState()
        val lifecycle = FakeProviderLifecycle()
        compose.setContent {
            TestTheme {
                LifecycleContent(state, lifecycle)
            }
        }

        compose.onNodeWithTag(AccountE2eIds.SERVER_URL_INPUT).performTextInput("https://sync.example.test")
        compose.onNodeWithTag(AccountE2eIds.USERNAME_INPUT).performTextInput("reader")
        compose.onNodeWithTag(AccountE2eIds.PASSWORD_INPUT).performTextInput("synthetic-password")
        val password = compose.onNodeWithTag(AccountE2eIds.PASSWORD_INPUT)
        password.requestFocus()
        password.performKeyInput { pressKey(Key.Enter) }
        compose.runOnIdle {
            assertEquals(1, lifecycle.loginCalls)
            assertEquals(true, lifecycle.isLoading.value)
        }
        compose.onNodeWithTag(AccountE2eIds.CONNECT_BUTTON).assertIsNotEnabled()

        compose.runOnIdle { lifecycle.failLogin("Synthetic login rejected") }
        compose.onNodeWithTag("account-login-error").assertTextEquals("Synthetic login rejected")
        compose.runOnIdle {
            assertEquals(false, state.submissionPending)
            assertEquals("reader", state.username.text)
        }
        compose.onNodeWithTag(AccountE2eIds.CONNECT_BUTTON).assertIsEnabled()
        compose.onNodeWithTag(AccountE2eIds.CONNECT_BUTTON).performSemanticsAction(SemanticsActions.OnClick) { it() }
        compose.runOnIdle { assertEquals(2, lifecycle.loginCalls) }

        compose.runOnIdle { lifecycle.link() }
        compose.onNodeWithTag(AccountE2eIds.CONNECTED_MESSAGE).assertExists()
        compose.onNodeWithTag(AccountE2eIds.SERVER_URL_INPUT).assertDoesNotExist()
        compose.runOnIdle {
            assertEquals("", state.password.text)
            assertEquals("", state.username.text)
        }

        compose.onNodeWithTag(AccountE2eIds.DISCONNECT_BUTTON).performClick()
        compose.onNodeWithTag("confirmation-title").assertExists()
        compose.onNodeWithText(EnFeedFlowStrings.cancelButton)
            .performSemanticsAction(SemanticsActions.OnClick) { it() }
        compose.onNodeWithTag("confirmation-title").assertDoesNotExist()
        compose.runOnIdle { assertEquals(0, lifecycle.disconnectCalls) }

        compose.onNodeWithTag(AccountE2eIds.DISCONNECT_BUTTON).performClick()
        val confirm = compose.onAllNodesWithText(EnFeedFlowStrings.accountDisconnectButton)[1]
        confirm.performSemanticsAction(SemanticsActions.OnClick) { it() }
        compose.runOnIdle {
            assertEquals(1, lifecycle.disconnectCalls)
            assertEquals(AccountConnectionUiState.Unlinked, lifecycle.uiState.value)
        }
        compose.onNodeWithTag(AccountE2eIds.CONNECT_BUTTON).assertExists()
    }

    @Composable
    private fun LifecycleContent(state: CredentialsState, lifecycle: FakeProviderLifecycle) {
        CloudAccountContent(
            description = "",
            successMessage = "Synthetic account linked",
            noSyncMessage = "No sync",
            uiState = lifecycle.uiState.value,
            onBackupClick = null,
            onDisconnectClick = lifecycle::disconnect,
            credentialsState = state,
            connectContent = {
                CredentialsForm(
                    serverUrlHint = "https://sync.example.test",
                    usernameHint = null,
                    passwordHint = null,
                    isLoginLoading = lifecycle.isLoading.value,
                    hasLocalSubscriptions = false,
                    onLogin = lifecycle::login,
                    errorMessage = lifecycle.error.value,
                    errorRevision = lifecycle.errorRevision.value,
                    credentialsState = state,
                )
            },
        )
    }

    @Composable
    private fun TestTheme(content: @Composable () -> Unit) {
        CompositionLocalProvider(LocalFeedFlowStrings provides EnFeedFlowStrings) {
            FeedFlowTheme(darkTheme = false, useOledTheme = false) { content() }
        }
    }

    private class FakeProviderLifecycle {
        val uiState = mutableStateOf<AccountConnectionUiState>(AccountConnectionUiState.Unlinked)
        val isLoading = mutableStateOf(false)
        val error = mutableStateOf<String?>(null)
        val errorRevision = mutableIntStateOf(0)
        var loginCalls = 0
            private set
        var disconnectCalls = 0
            private set

        fun login(serverUrl: String?, username: String, password: String) {
            assertEquals("https://sync.example.test", serverUrl)
            assertEquals("reader", username)
            assertEquals("synthetic-password", password)
            loginCalls++
            isLoading.value = true
            error.value = null
        }

        fun failLogin(message: String) {
            isLoading.value = false
            error.value = message
            errorRevision.value++
            uiState.value = AccountConnectionUiState.Unlinked
        }

        fun link() {
            isLoading.value = false
            error.value = null
            uiState.value = AccountConnectionUiState.Linked(AccountSyncUIState.None)
        }

        fun disconnect() {
            disconnectCalls++
            uiState.value = AccountConnectionUiState.Unlinked
        }
    }
}
