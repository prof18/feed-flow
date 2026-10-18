package com.prof18.feedflow.android.feedverification

import androidx.activity.ComponentDialog
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.prof18.feedflow.android.ui.theme.FeedFlowTheme
import com.prof18.feedflow.android.ui.utils.ProvideFeedFlowStrings
import com.prof18.feedflow.android.ui.utils.rememberFeedFlowStrings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowDialog

@RunWith(RobolectricTestRunner::class)
class WebsiteVerificationUiTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun explanationConfirmInvokesCallbackOnlyAfterClick() {
        var confirms = 0
        var dismissals = 0
        setContent {
            var showDialog by remember { mutableStateOf(true) }
            if (showDialog) {
                WebsiteVerificationExplanationDialog(
                    websiteUrl = WEBSITE_URL,
                    onDismiss = {
                        dismissals++
                        showDialog = false
                    },
                    onConfirm = {
                        confirms++
                        showDialog = false
                    },
                )
            }
        }
        compose.onNodeWithTag(FeedVerificationE2eIds.EXPLANATION).assertIsDisplayed()
        compose.runOnIdle {
            assertEquals(0, confirms)
            assertEquals(0, dismissals)
        }

        compose.onNodeWithTag(FeedVerificationE2eIds.OPEN).performClick()
        compose.onNodeWithTag(FeedVerificationE2eIds.EXPLANATION).assertDoesNotExist()
        compose.runOnIdle {
            assertEquals(1, confirms)
            assertEquals(0, dismissals)
        }
    }

    @Test
    fun explanationDismissalDoesNotConfirm() {
        var confirms = 0
        var dismissals = 0
        setContent {
            var showDialog by remember { mutableStateOf(true) }
            if (showDialog) {
                WebsiteVerificationExplanationDialog(
                    websiteUrl = WEBSITE_URL,
                    onDismiss = {
                        dismissals++
                        showDialog = false
                    },
                    onConfirm = { confirms++ },
                )
            }
        }
        compose.onNodeWithTag(FeedVerificationE2eIds.EXPLANATION).assertIsDisplayed()

        compose.runOnIdle {
            val dialog = ShadowDialog.getLatestDialog() as ComponentDialog
            dialog.onBackPressedDispatcher.onBackPressed()
        }

        compose.onNodeWithTag(FeedVerificationE2eIds.EXPLANATION).assertDoesNotExist()
        compose.runOnIdle {
            assertEquals(0, confirms)
            assertEquals(1, dismissals)
        }
    }

    @Test
    fun checkingScreenShowsOwnedProgressAndDoesNotComposeBrowser() {
        var browserComposed = false
        setContent {
            WebsiteVerificationContent(
                website = WEBSITE_HOST,
                state = VerificationPageState.Checking,
                canRetry = false,
                onCancel = {},
                onRetry = {},
                browser = {
                    browserComposed = true
                    Box(it.fillMaxSize()) { Text(RAW_XML_MARKER) }
                },
            )
        }
        compose.onNodeWithTag(FeedVerificationE2eIds.CHECKING).assertIsDisplayed()
        compose.onNodeWithTag(FeedVerificationE2eIds.CANCEL).assertIsDisplayed()
        compose.onNodeWithTag(FeedVerificationE2eIds.WEB_CONTENT).assertDoesNotExist()
        compose.onNodeWithText(RAW_XML_MARKER).assertDoesNotExist()
        compose.runOnIdle { assertFalse(browserComposed) }
    }

    @Test
    fun waitingScreenShowsBrowserWithoutCheckAgainButton() {
        var browserComposed = false
        setContent {
            WebsiteVerificationContent(
                website = WEBSITE_HOST,
                state = VerificationPageState.Waiting,
                canRetry = false,
                onCancel = {},
                onRetry = {},
                browser = { modifier ->
                    browserComposed = true
                    Box(modifier.fillMaxSize()) { Text(SECURITY_CHECK_MARKER) }
                },
            )
        }
        compose.onNodeWithTag(FeedVerificationE2eIds.WEB_CONTENT).assertIsDisplayed()
        compose.onNodeWithText(SECURITY_CHECK_MARKER).assertIsDisplayed()
        compose.onNodeWithTag(FeedVerificationE2eIds.CONTINUE).assertDoesNotExist()
        compose.runOnIdle { assertTrue(browserComposed) }
    }

    @Test
    fun failedScreenShowsCheckAgainCallback() {
        var retries = 0
        setContent {
            var state by remember { mutableStateOf(VerificationPageState.Failed) }
            WebsiteVerificationContent(
                website = WEBSITE_HOST,
                state = state,
                canRetry = state == VerificationPageState.Failed,
                onCancel = {},
                onRetry = {
                    retries++
                    state = VerificationPageState.Checking
                },
                browser = { modifier -> Box(modifier.fillMaxSize()) { Text(SECURITY_CHECK_MARKER) } },
            )
        }
        compose.onNodeWithTag(FeedVerificationE2eIds.CONTINUE).performClick()

        compose.onNodeWithTag(FeedVerificationE2eIds.CONTINUE).assertDoesNotExist()
        compose.onNodeWithTag(FeedVerificationE2eIds.CHECKING).assertIsDisplayed()
        compose.runOnIdle { assertEquals(1, retries) }
    }

    private fun setContent(content: @Composable () -> Unit) {
        compose.setContent {
            FeedFlowTheme {
                ProvideFeedFlowStrings(rememberFeedFlowStrings()) { content() }
            }
        }
    }

    private companion object {
        const val WEBSITE_URL = "https://example.com/feed"
        const val WEBSITE_HOST = "example.com"
        const val RAW_XML_MARKER = "RAW XML MARKER"
        const val SECURITY_CHECK_MARKER = "SECURITY CHECK FIXTURE"
    }
}
