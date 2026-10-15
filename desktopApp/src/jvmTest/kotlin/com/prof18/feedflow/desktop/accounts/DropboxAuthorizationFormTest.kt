package com.prof18.feedflow.desktop.accounts

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import com.prof18.feedflow.desktop.ui.LocalFeedFlowStrings
import com.prof18.feedflow.desktop.ui.components.FeedFlowTheme
import com.prof18.feedflow.i18n.EnFeedFlowStrings
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals

class DropboxAuthorizationFormTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `starting browser authorization cannot issue duplicate requests`() {
        var requests = 0
        setForm(ready = false, onStart = { requests++ })
        val start = compose.onNodeWithTag("dropbox-start-auth")
        start.performSemanticsAction(SemanticsActions.OnClick) { it() }
        start.performSemanticsAction(SemanticsActions.OnClick) { it() }
        compose.runOnIdle { assertEquals(1, requests) }
    }

    @Test
    fun `code entry is offered only after browser authorization starts`() {
        setForm(ready = false)
        compose.onNodeWithTag("dropbox-auth-code").assertDoesNotExist()
    }

    @Test
    fun `Enter confirms a synthetic authorization code once`() {
        var requests = 0
        setForm(onConfirm = { requests++ })
        val code = compose.onNodeWithTag("dropbox-auth-code")
        code.performTextInput("synthetic-authorization-code")
        code.requestFocus()
        code.performKeyInput {
            pressKey(Key.Enter)
            pressKey(Key.Enter)
        }
        compose.runOnIdle { assertEquals(1, requests) }
    }

    @Test
    fun `code confirmation is guarded before loading is observed`() {
        var requests = 0
        setForm(onConfirm = { requests++ })
        compose.onNodeWithTag("dropbox-auth-code").performTextInput("synthetic-authorization-code")
        val confirm = compose.onNodeWithTag("dropbox-confirm-auth")
        confirm.performSemanticsAction(SemanticsActions.OnClick) { it() }
        confirm.performSemanticsAction(SemanticsActions.OnClick) { it() }
        compose.runOnIdle { assertEquals(1, requests) }
    }

    @Test
    fun `loading disables named authorization input and its confirmation`() {
        setForm(loading = true)
        compose.onNodeWithTag("dropbox-auth-code")
            .assertContentDescriptionEquals(EnFeedFlowStrings.dropboxAuthCodeHint)
        compose.onNodeWithTag("dropbox-auth-code").assertIsNotEnabled()
        compose.onNodeWithTag("dropbox-confirm-auth").assertIsNotEnabled()
    }

    @Test
    fun `failed code exchange permits retry without losing synthetic draft`() {
        var requests = 0
        val state = CredentialsState()
        val revision = mutableStateOf(0)
        compose.setContent {
            CompositionLocalProvider(LocalFeedFlowStrings provides EnFeedFlowStrings) {
                FeedFlowTheme(darkTheme = false, useOledTheme = false) {
                    DropboxAuthorizationForm(
                        state = state,
                        isLoading = false,
                        isCodeEntryReady = true,
                        errorRevision = revision.value,
                        errorMessage = state.connectionError,
                        onStartAuth = {},
                        onConfirmCode = { requests++ },
                    )
                }
            }
        }
        val input = compose.onNodeWithTag("dropbox-auth-code")
        val confirm = compose.onNodeWithTag("dropbox-confirm-auth")
        input.performTextInput("synthetic-code")
        confirm.performSemanticsAction(SemanticsActions.OnClick) { it() }
        confirm.assertIsNotEnabled()
        compose.runOnIdle {
            state.connectionError = "Synthetic rejected code"
            revision.value++
        }
        compose.onNodeWithText("Synthetic rejected code").assertExists()
        compose.runOnIdle { assertEquals("synthetic-code", state.authCode.text) }
        confirm.assertIsEnabled()
        confirm.performSemanticsAction(SemanticsActions.OnClick) { it() }
        compose.runOnIdle { assertEquals(2, requests) }
    }

    @Test
    fun `restart clears the old code and cannot start twice`() {
        var requests = 0
        val state = CredentialsState()
        compose.setContent {
            CompositionLocalProvider(LocalFeedFlowStrings provides EnFeedFlowStrings) {
                FeedFlowTheme(darkTheme = false, useOledTheme = false) {
                    DropboxAuthorizationForm(
                        state = state,
                        isLoading = false,
                        isCodeEntryReady = true,
                        errorRevision = 0,
                        onStartAuth = { requests++ },
                        onConfirmCode = {},
                    )
                }
            }
        }
        compose.onNodeWithTag("dropbox-auth-code").performTextInput("synthetic-code")
        val restart = compose.onNodeWithTag("dropbox-restart-auth")
        val action = restart.fetchSemanticsNode().config[SemanticsActions.OnClick].action
        compose.runOnIdle {
            requireNotNull(action).invoke()
            action.invoke()
            assertEquals(1, requests)
            assertEquals("", state.authCode.text)
        }
        compose.onNodeWithTag("dropbox-confirm-auth").assertIsNotEnabled()
    }

    @Test
    fun `remount does not unlock code exchange before loading is observed`() {
        var requests = 0
        val state = CredentialsState().apply {
            authCode = androidx.compose.ui.text.input.TextFieldValue("synthetic-code")
            authCodePending = true
        }
        compose.setContent {
            CompositionLocalProvider(LocalFeedFlowStrings provides EnFeedFlowStrings) {
                FeedFlowTheme(darkTheme = false, useOledTheme = false) {
                    DropboxAuthorizationForm(
                        state = state,
                        isLoading = false,
                        isCodeEntryReady = true,
                        errorRevision = 0,
                        onStartAuth = {},
                        onConfirmCode = { requests++ },
                    )
                }
            }
        }
        val confirm = compose.onNodeWithTag("dropbox-confirm-auth")
        confirm.assertIsNotEnabled()
        confirm.performSemanticsAction(SemanticsActions.OnClick) { it() }
        compose.runOnIdle { assertEquals(0, requests) }
    }

    private fun setForm(
        ready: Boolean = true,
        loading: Boolean = false,
        onStart: () -> Unit = {},
        onConfirm: (String) -> Unit = {},
    ) {
        compose.setContent {
            CompositionLocalProvider(LocalFeedFlowStrings provides EnFeedFlowStrings) {
                FeedFlowTheme(darkTheme = false, useOledTheme = false) {
                    DropboxAuthorizationForm(
                        state = remember { CredentialsState() },
                        isLoading = loading,
                        isCodeEntryReady = ready,
                        errorRevision = 0,
                        onStartAuth = onStart,
                        onConfirmCode = onConfirm,
                    )
                }
            }
        }
    }
}
