package com.prof18.feedflow.desktop.accounts

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import com.prof18.feedflow.desktop.testing.AccountE2eIds
import com.prof18.feedflow.desktop.ui.LocalFeedFlowStrings
import com.prof18.feedflow.desktop.ui.components.FeedFlowTheme
import com.prof18.feedflow.i18n.EnFeedFlowStrings
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals

class AccountConnectButtonTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun `duplicate pointer actions submit only once`() {
        var submissions = 0
        val state = CredentialsState()
        rule.setContent {
            CompositionLocalProvider(LocalFeedFlowStrings provides EnFeedFlowStrings) {
                FeedFlowTheme(darkTheme = false, useOledTheme = false) {
                    AccountConnectButton(
                        label = "Connect provider",
                        isLoading = false,
                        errorRevision = 0,
                        onConnect = { submissions++ },
                        state = state,
                    )
                }
            }
        }

        val button = rule.onNodeWithTag(AccountE2eIds.CONNECT_BUTTON)
        button.performSemanticsAction(SemanticsActions.OnClick) { it() }
        button.performSemanticsAction(SemanticsActions.OnClick) { it() }

        rule.runOnIdle { assertEquals(1, submissions) }
    }

    @Test
    fun `loading disables button but keeps label and then allows retry`() {
        var submissions = 0
        val isLoading = mutableStateOf(true)
        rule.setContent {
            CompositionLocalProvider(LocalFeedFlowStrings provides EnFeedFlowStrings) {
                FeedFlowTheme(darkTheme = false, useOledTheme = false) {
                    AccountConnectButton(
                        label = EnFeedFlowStrings.googleDriveConnectButton,
                        isLoading = isLoading.value,
                        errorRevision = 0,
                        onConnect = { submissions++ },
                    )
                }
            }
        }

        val button = rule.onNodeWithTag(AccountE2eIds.CONNECT_BUTTON)
        button.assertIsNotEnabled()
        rule.onNodeWithText(EnFeedFlowStrings.googleDriveConnectButton).assertTextEquals(
            EnFeedFlowStrings.googleDriveConnectButton,
        )
        rule.runOnIdle { isLoading.value = false }
        button.assertIsEnabled()
        button.performSemanticsAction(SemanticsActions.OnClick) { it() }
        rule.runOnIdle { assertEquals(1, submissions) }
    }

    @Test
    fun `new error revision unlocks a fast failed submission`() {
        var submissions = 0
        val errorRevision = mutableStateOf(0)
        val state = CredentialsState().apply { submissionPending = true }
        rule.setContent {
            CompositionLocalProvider(LocalFeedFlowStrings provides EnFeedFlowStrings) {
                FeedFlowTheme(darkTheme = false, useOledTheme = false) {
                    AccountConnectButton(
                        label = "Connect provider",
                        isLoading = false,
                        errorRevision = errorRevision.value,
                        onConnect = { submissions++ },
                        state = state,
                    )
                }
            }
        }

        val button = rule.onNodeWithTag(AccountE2eIds.CONNECT_BUTTON)
        button.assertIsNotEnabled()
        rule.runOnIdle { errorRevision.value++ }
        button.assertIsEnabled()
        button.performSemanticsAction(SemanticsActions.OnClick) { it() }
        rule.runOnIdle { assertEquals(1, submissions) }
    }

    @Test
    fun `Enter activates the same connect callback`() {
        var submissions = 0
        rule.setContent {
            CompositionLocalProvider(LocalFeedFlowStrings provides EnFeedFlowStrings) {
                FeedFlowTheme(darkTheme = false, useOledTheme = false) {
                    AccountConnectButton(
                        label = "Connect provider",
                        isLoading = false,
                        errorRevision = 0,
                        onConnect = { submissions++ },
                    )
                }
            }
        }

        val button = rule.onNodeWithTag(AccountE2eIds.CONNECT_BUTTON)
        button.performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        button.performKeyInput { pressKey(Key.Enter) }
        rule.runOnIdle { assertEquals(1, submissions) }
    }

    @Test
    fun `pending draft survives unmount and same error revision does not enable retry`() {
        var submissions = 0
        val isMounted = mutableStateOf(true)
        val errorRevision = mutableStateOf(0)
        val state = CredentialsState()
        rule.setContent {
            CompositionLocalProvider(LocalFeedFlowStrings provides EnFeedFlowStrings) {
                FeedFlowTheme(darkTheme = false, useOledTheme = false) {
                    if (isMounted.value) {
                        AccountConnectButton(
                            label = "Connect provider",
                            isLoading = false,
                            errorRevision = errorRevision.value,
                            onConnect = { submissions++ },
                            state = state,
                        )
                    }
                }
            }
        }

        val button = rule.onNodeWithTag(AccountE2eIds.CONNECT_BUTTON)
        button.performSemanticsAction(SemanticsActions.OnClick) { it() }
        rule.runOnIdle { isMounted.value = false }
        rule.runOnIdle { isMounted.value = true }
        button.assertIsNotEnabled()

        val sameRevisionRetry = button.fetchSemanticsNode().config[SemanticsActions.OnClick].action
        rule.runOnIdle {
            requireNotNull(sameRevisionRetry).invoke()
            assertEquals(1, submissions)
            errorRevision.value++
        }
        button.assertIsEnabled()
        button.performSemanticsAction(SemanticsActions.OnClick) { it() }
        rule.runOnIdle { assertEquals(2, submissions) }
    }

    @Test
    fun `captured click cannot submit after actual loading starts`() {
        var submissions = 0
        val isLoading = mutableStateOf(false)
        rule.setContent {
            CompositionLocalProvider(LocalFeedFlowStrings provides EnFeedFlowStrings) {
                FeedFlowTheme(darkTheme = false, useOledTheme = false) {
                    AccountConnectButton(
                        label = "Connect provider",
                        isLoading = isLoading.value,
                        errorRevision = 0,
                        onConnect = { submissions++ },
                    )
                }
            }
        }

        val button = rule.onNodeWithTag(AccountE2eIds.CONNECT_BUTTON)
        val capturedClick = button.fetchSemanticsNode().config[SemanticsActions.OnClick].action
        rule.runOnIdle { isLoading.value = true }
        button.assertIsNotEnabled()
        rule.runOnIdle {
            requireNotNull(capturedClick).invoke()
            assertEquals(0, submissions)
        }
    }
}
