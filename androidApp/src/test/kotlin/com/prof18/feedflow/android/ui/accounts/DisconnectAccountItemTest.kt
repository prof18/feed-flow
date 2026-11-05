package com.prof18.feedflow.android.ui.accounts

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import com.prof18.feedflow.android.ui.theme.FeedFlowTheme
import com.prof18.feedflow.android.ui.utils.ProvideFeedFlowStrings
import com.prof18.feedflow.android.ui.utils.rememberFeedFlowStrings
import com.prof18.feedflow.i18n.EnFeedFlowStrings
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DisconnectAccountItemTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `clicking the row asks for confirmation with the reader message`() {
        var confirmCount = 0
        setContent {
            DisconnectAccountItem(
                kind = DisconnectAccountKind.ReaderService,
                isEnabled = true,
                onConfirm = { confirmCount++ },
            )
        }

        compose.onNodeWithTag(AccountE2eIds.DISCONNECT_BUTTON).performClick()

        compose.onNodeWithText(EnFeedFlowStrings.accountDisconnectTitle).assertExists()
        compose.onNodeWithText(EnFeedFlowStrings.accountDisconnectServerMessage).assertExists()
        assertEquals(0, confirmCount)
    }

    @Test
    fun `backup services show the backup message`() {
        setContent {
            DisconnectAccountItem(
                kind = DisconnectAccountKind.BackupService,
                isEnabled = true,
                onConfirm = {},
            )
        }

        compose.onNodeWithTag(AccountE2eIds.DISCONNECT_BUTTON).performClick()

        compose.onNodeWithText(EnFeedFlowStrings.accountDisconnectBackupMessage).assertExists()
        compose.onNodeWithText(EnFeedFlowStrings.accountDisconnectServerMessage).assertDoesNotExist()
    }

    @Test
    fun `confirming disconnects once and closes the dialog`() {
        var confirmCount = 0
        setContent {
            DisconnectAccountItem(
                kind = DisconnectAccountKind.ReaderService,
                isEnabled = true,
                onConfirm = { confirmCount++ },
            )
        }

        compose.onNodeWithTag(AccountE2eIds.DISCONNECT_BUTTON).performClick()
        compose.onNodeWithTag(AccountE2eIds.DISCONNECT_CONFIRM_BUTTON).performClick()

        assertEquals(1, confirmCount)
        compose.onNodeWithText(EnFeedFlowStrings.accountDisconnectTitle).assertDoesNotExist()
    }

    @Test
    fun `cancelling closes the dialog without disconnecting`() {
        var confirmCount = 0
        setContent {
            DisconnectAccountItem(
                kind = DisconnectAccountKind.ReaderService,
                isEnabled = true,
                onConfirm = { confirmCount++ },
            )
        }

        compose.onNodeWithTag(AccountE2eIds.DISCONNECT_BUTTON).performClick()
        compose.onNodeWithText(EnFeedFlowStrings.cancelButton).performClick()

        assertEquals(0, confirmCount)
        compose.onNodeWithText(EnFeedFlowStrings.accountDisconnectTitle).assertDoesNotExist()
    }

    @Test
    fun `disabled row does not open the dialog`() {
        setContent {
            DisconnectAccountItem(
                kind = DisconnectAccountKind.ReaderService,
                isEnabled = false,
                onConfirm = {},
            )
        }

        compose.onNodeWithTag(AccountE2eIds.DISCONNECT_BUTTON).performClick()

        compose.onNodeWithText(EnFeedFlowStrings.accountDisconnectTitle).assertDoesNotExist()
    }

    @Test
    fun `a sync starting while the dialog is open closes it for good`() {
        var isEnabled by mutableStateOf(true)
        setContent {
            DisconnectAccountItem(
                kind = DisconnectAccountKind.ReaderService,
                isEnabled = isEnabled,
                onConfirm = {},
            )
        }

        compose.onNodeWithTag(AccountE2eIds.DISCONNECT_BUTTON).performClick()
        compose.onNodeWithText(EnFeedFlowStrings.accountDisconnectTitle).assertExists()

        isEnabled = false
        compose.waitForIdle()
        isEnabled = true
        compose.waitForIdle()

        compose.onNodeWithText(EnFeedFlowStrings.accountDisconnectTitle).assertDoesNotExist()
    }

    @Test
    fun `a double click on confirm disconnects once`() {
        var confirmCount = 0
        setContent {
            DisconnectAccountItem(
                kind = DisconnectAccountKind.ReaderService,
                isEnabled = true,
                onConfirm = { confirmCount++ },
            )
        }

        compose.onNodeWithTag(AccountE2eIds.DISCONNECT_BUTTON).performClick()
        compose.onNodeWithTag(AccountE2eIds.DISCONNECT_CONFIRM_BUTTON)
            .performSemanticsAction(SemanticsActions.OnClick) {
                it()
                it()
            }

        assertEquals(1, confirmCount)
    }

    private fun setContent(content: @Composable () -> Unit) {
        compose.setContent {
            FeedFlowTheme {
                ProvideFeedFlowStrings(rememberFeedFlowStrings()) { content() }
            }
        }
    }
}
