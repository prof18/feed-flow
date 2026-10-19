package com.prof18.feedflow.desktop.accounts

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import com.prof18.feedflow.desktop.testing.AccountE2eIds
import com.prof18.feedflow.desktop.ui.LocalFeedFlowStrings
import com.prof18.feedflow.desktop.ui.components.FeedFlowTheme
import com.prof18.feedflow.i18n.EnFeedFlowStrings
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals

class CredentialsFormTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun `remount preserves pending login before loading is observed`() {
        var requests = 0
        val state = CredentialsState().apply {
            username = androidx.compose.ui.text.input.TextFieldValue("synthetic-reader")
            password = androidx.compose.ui.text.input.TextFieldValue("synthetic-password")
            submissionPending = true
        }
        rule.setContent {
            CompositionLocalProvider(LocalFeedFlowStrings provides EnFeedFlowStrings) {
                FeedFlowTheme(darkTheme = false, useOledTheme = false) {
                    CredentialsForm(
                        serverUrlHint = null,
                        usernameHint = null,
                        passwordHint = null,
                        isLoginLoading = false,
                        hasLocalSubscriptions = false,
                        onLogin = { _, _, _ -> requests++ },
                        credentialsState = state,
                    )
                }
            }
        }
        val connect = rule.onNodeWithTag(AccountE2eIds.CONNECT_BUTTON)
        connect.assertIsNotEnabled()
        connect.performSemanticsAction(SemanticsActions.OnClick) { it() }
        rule.runOnIdle { assertEquals(0, requests) }
    }

    @Test
    fun `Connect ignores a second action before loading reaches the form`() {
        var requests = 0
        setForm(onLogin = { _, _, _ -> requests++ })
        enterCredentials()

        val connect = rule.onNodeWithTag(AccountE2eIds.CONNECT_BUTTON)
        connect.performSemanticsAction(SemanticsActions.OnClick) { it() }
        connect.performSemanticsAction(SemanticsActions.OnClick) { it() }

        rule.runOnIdle { assertEquals(1, requests) }
    }

    @Test
    fun `Enter validates and focuses the first empty field without submitting`() {
        var requests = 0
        setForm(onLogin = { _, _, _ -> requests++ })
        val password = rule.onNodeWithTag(AccountE2eIds.PASSWORD_INPUT)
        password.requestFocus()
        password.performKeyInput { pressKey(Key.Enter) }

        rule.onNodeWithTag(AccountE2eIds.SERVER_URL_INPUT).assertIsFocused()
        rule.onNodeWithTag("account-server-error").assertExists()
        rule.runOnIdle { assertEquals(0, requests) }
    }

    @Test
    fun `Enter submits valid credentials once and failed loading allows retry`() {
        var requests = 0
        val loading = mutableStateOf(false)
        setForm(loading = { loading.value }, onLogin = { _, _, _ -> requests++ })
        enterCredentials()
        val password = rule.onNodeWithTag(AccountE2eIds.PASSWORD_INPUT)
        password.requestFocus()
        password.performKeyInput {
            pressKey(Key.Enter)
            pressKey(Key.Enter)
        }
        rule.runOnIdle {
            assertEquals(1, requests)
            loading.value = true
        }
        rule.onNodeWithTag(AccountE2eIds.USERNAME_INPUT).assertIsNotEnabled()
        rule.onNodeWithTag(AccountE2eIds.PASSWORD_INPUT).assertIsNotEnabled()
        rule.onNodeWithTag(AccountE2eIds.CONNECT_BUTTON).assertIsNotEnabled()
        rule.onNodeWithText(EnFeedFlowStrings.accountConnectButton).assertExists()
        rule.runOnIdle { loading.value = false }
        rule.onNodeWithTag(AccountE2eIds.CONNECT_BUTTON).performClick()
        rule.runOnIdle { assertEquals(2, requests) }
    }

    @Test
    fun `password visibility action has a name reflecting its state`() {
        setForm()
        val toggle = rule.onNodeWithTag(AccountE2eIds.PASSWORD_VISIBILITY)
        toggle.assertContentDescriptionEquals(EnFeedFlowStrings.desktopAccountShowPassword)
        toggle.performClick()
        toggle.assertContentDescriptionEquals(EnFeedFlowStrings.desktopAccountHidePassword)
    }

    @Test
    fun `Enter on password visibility toggles masking instead of submitting`() {
        var requests = 0
        setForm(onLogin = { _, _, _ -> requests++ })
        enterCredentials()
        val toggle = rule.onNodeWithTag(AccountE2eIds.PASSWORD_VISIBILITY)
        toggle.requestFocus()
        toggle.performKeyInput { pressKey(Key.Enter) }
        toggle.assertContentDescriptionEquals(EnFeedFlowStrings.desktopAccountHidePassword)
        rule.runOnIdle { assertEquals(0, requests) }
    }

    @Test
    fun `fast retry with the same failure message leaves Connect available`() {
        var requests = 0
        val loading = mutableStateOf(false)
        val error = mutableStateOf<String?>(EnFeedFlowStrings.wrongCredentialsErrorMessage)
        val revision = mutableIntStateOf(0)
        setForm(
            loading = { loading.value },
            errorMessage = { error.value },
            errorRevision = { revision.value },
            onLogin = { _, _, _ ->
                requests++
                loading.value = true
                error.value = null
                error.value = EnFeedFlowStrings.wrongCredentialsErrorMessage
                revision.value++
                loading.value = false
            },
        )
        enterCredentials()
        val connect = rule.onNodeWithTag(AccountE2eIds.CONNECT_BUTTON)
        connect.performClick()
        connect.performSemanticsAction(SemanticsActions.OnClick) { it() }
        rule.runOnIdle { assertEquals(2, requests) }
    }

    @Test
    fun `clearing an old error does not reopen submission before loading starts`() {
        var requests = 0
        val error = mutableStateOf<String?>(EnFeedFlowStrings.wrongCredentialsErrorMessage)
        setForm(
            errorMessage = { error.value },
            onLogin = { _, _, _ ->
                requests++
                error.value = null
            },
        )
        enterCredentials()
        val connect = rule.onNodeWithTag(AccountE2eIds.CONNECT_BUTTON)
        connect.performClick()
        connect.performSemanticsAction(SemanticsActions.OnClick) { it() }
        rule.runOnIdle { assertEquals(1, requests) }
    }

    private fun setForm(
        loading: () -> Boolean = { false },
        errorMessage: () -> String? = { null },
        errorRevision: () -> Int = { 0 },
        onLogin: (String?, String, String) -> Unit = { _, _, _ -> },
    ) {
        rule.setContent {
            CompositionLocalProvider(LocalFeedFlowStrings provides EnFeedFlowStrings) {
                FeedFlowTheme(darkTheme = false, useOledTheme = false) {
                    CredentialsForm(
                        serverUrlHint = "Server endpoint",
                        usernameHint = null,
                        passwordHint = "Password guidance",
                        isLoginLoading = loading(),
                        errorMessage = errorMessage(),
                        errorRevision = errorRevision(),
                        hasLocalSubscriptions = false,
                        onLogin = onLogin,
                    )
                }
            }
        }
    }

    private fun enterCredentials() {
        rule.onNodeWithTag(AccountE2eIds.SERVER_URL_INPUT).performTextInput("https://rss.example/api/greader.php")
        rule.onNodeWithTag(AccountE2eIds.USERNAME_INPUT).performTextInput("reader")
        rule.onNodeWithTag(AccountE2eIds.PASSWORD_INPUT).performTextInput("synthetic-password")
    }

    @Test
    fun `credentials preserve login arguments loading gate and local-feed confirmation cancel`() {
        var submitted: List<String?>? = null
        val loading = mutableStateOf(false)
        val hasLocalSubscriptions = mutableStateOf(false)
        rule.setContent {
            CompositionLocalProvider(LocalFeedFlowStrings provides EnFeedFlowStrings) {
                FeedFlowTheme(darkTheme = false, useOledTheme = false) {
                    CredentialsForm(
                        serverUrlHint = EnFeedFlowStrings.freshRssUrlHint("https://rss.example/api/greader.php"),
                        usernameHint = null,
                        passwordHint = EnFeedFlowStrings.freshRssPasswordHint,
                        isLoginLoading = loading.value,
                        hasLocalSubscriptions = hasLocalSubscriptions.value,
                        onLogin = { serverUrl, username, password ->
                            submitted = listOf(serverUrl, username, password)
                        },
                    )
                }
            }
        }

        rule.onNodeWithTag(AccountE2eIds.SERVER_URL_INPUT).performTextInput("https://rss.example/api/greader.php")
        rule.onNodeWithTag(AccountE2eIds.USERNAME_INPUT).performTextInput("reader")
        rule.onNodeWithTag(AccountE2eIds.PASSWORD_INPUT).performTextInput("secret")
        rule.onNodeWithTag(AccountE2eIds.CONNECT_BUTTON).performClick()
        rule.runOnIdle {
            assertEquals(
                listOf("https://rss.example/api/greader.php", "reader", "secret"),
                submitted,
            )
        }

        rule.runOnIdle {
            submitted = null
            loading.value = true
        }
        rule.onNodeWithTag(AccountE2eIds.CONNECT_BUTTON).assertIsNotEnabled()

        rule.runOnIdle {
            loading.value = false
            hasLocalSubscriptions.value = true
        }
        rule.onNodeWithTag(AccountE2eIds.CONNECT_BUTTON).performClick()
        rule.onNodeWithTag(
            "confirmation-title",
        ).assertTextEquals(EnFeedFlowStrings.serverAccountReplaceLocalTitle)
        rule.onNodeWithText(EnFeedFlowStrings.cancelButton).performSemanticsAction(SemanticsActions.OnClick) { it() }
        rule.waitForIdle()
        rule.onNodeWithTag("confirmation-title").assertDoesNotExist()
        assertEquals(null, submitted)
    }

    @Test
    fun `field guidance stays visible after typing and credentials remain single line`() {
        val serverHint = "Server endpoint hint"
        val usernameHint = "Username format hint"
        val passwordHint = "Password format hint"
        rule.setContent {
            CompositionLocalProvider(LocalFeedFlowStrings provides EnFeedFlowStrings) {
                FeedFlowTheme(darkTheme = false, useOledTheme = false) {
                    CredentialsForm(
                        serverUrlHint = serverHint,
                        usernameHint = usernameHint,
                        passwordHint = passwordHint,
                        isLoginLoading = false,
                        hasLocalSubscriptions = false,
                        onLogin = { _, _, _ -> },
                    )
                }
            }
        }

        rule.onNodeWithTag(AccountE2eIds.SERVER_URL_INPUT).performTextInput("https://feed.example")
        rule.onNodeWithTag(AccountE2eIds.USERNAME_INPUT).performTextInput("readername")
        rule.onNodeWithTag(AccountE2eIds.PASSWORD_INPUT).performTextInput("password")

        rule.onNodeWithText(serverHint).assertExists()
        rule.onNodeWithText(usernameHint).assertExists()
        rule.onNodeWithText(passwordHint).assertExists()
    }
}
