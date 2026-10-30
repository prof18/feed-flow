package com.prof18.feedflow.desktop.home

import com.prof18.feedflow.core.model.FeedSyncError
import com.prof18.feedflow.core.model.NoCode
import com.prof18.feedflow.core.model.SyncFeedError
import com.prof18.feedflow.desktop.ui.DesktopFeedbackDuration
import com.prof18.feedflow.desktop.ui.DesktopFeedbackState
import com.prof18.feedflow.i18n.EnFeedFlowStrings
import com.prof18.feedflow.shared.presentation.model.syncErrorMessage
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class DesktopSyncFailureFeedbackTest {
    @Test
    fun `retryable refresh errors offer long retry feedback and retry only after action`() = runTest {
        val errors = listOf(
            FeedSyncError.FetchFeedSourcesAndCategoriesFailed,
            FeedSyncError.SyncFeedsFailed,
            SyncFeedError.FeedSourcesSyncFailed,
            SyncFeedError.FeedItemsSyncFailed,
        )

        for (error in errors) {
            val state = DesktopFeedbackState()
            val calls = mutableListOf<String>()
            val feedback = async(start = CoroutineStart.UNDISPATCHED) {
                showDesktopSyncFailureFeedback(
                    state = state,
                    errorCode = error,
                    strings = EnFeedFlowStrings,
                    onRetryRefresh = { calls += "retry" },
                    onOpenAccounts = { calls += "accounts" },
                )
            }

            val visible = assertNotNull(state.currentFeedback)
            assertEquals(EnFeedFlowStrings.syncErrorMessage(error), visible.message)
            assertEquals(EnFeedFlowStrings.retryButton, visible.actionLabel)
            assertEquals(DesktopFeedbackDuration.Long, visible.duration)
            assertEquals(emptyList(), calls)

            visible.performAction()
            feedback.await()
            assertEquals(listOf("retry"), calls)
            assertNull(state.currentFeedback)
        }
    }

    @Test
    fun `bad reader token offers accounts action only after selection`() = runTest {
        val state = DesktopFeedbackState()
        val calls = mutableListOf<String>()
        val feedback = async(start = CoroutineStart.UNDISPATCHED) {
            showDesktopSyncFailureFeedback(
                state = state,
                errorCode = FeedSyncError.GReaderBadToken,
                strings = EnFeedFlowStrings,
                onRetryRefresh = { calls += "retry" },
                onOpenAccounts = { calls += "accounts" },
            )
        }

        val visible = assertNotNull(state.currentFeedback)
        assertEquals(EnFeedFlowStrings.syncErrorMessage(FeedSyncError.GReaderBadToken), visible.message)
        assertEquals(EnFeedFlowStrings.settingsAccounts, visible.actionLabel)
        assertEquals(DesktopFeedbackDuration.Long, visible.duration)
        visible.performAction()
        feedback.await()
        assertEquals(listOf("accounts"), calls)
        assertNull(state.currentFeedback)
    }

    @Test
    fun `dismissing retry feedback does not repeat refresh`() = runTest {
        val state = DesktopFeedbackState()
        var retryCount = 0
        val feedback = async(start = CoroutineStart.UNDISPATCHED) {
            showDesktopSyncFailureFeedback(
                state = state,
                errorCode = FeedSyncError.SyncFeedsFailed,
                strings = EnFeedFlowStrings,
                onRetryRefresh = { retryCount += 1 },
                onOpenAccounts = {},
            )
        }

        assertNotNull(state.currentFeedback).dismiss()
        feedback.await()
        assertEquals(0, retryCount)
        assertNull(state.currentFeedback)
    }

    @Test
    fun `cancellation does not repeat refresh`() = runTest {
        val state = DesktopFeedbackState()
        var retryCount = 0
        val feedback = async(start = CoroutineStart.UNDISPATCHED) {
            showDesktopSyncFailureFeedback(
                state = state,
                errorCode = FeedSyncError.SyncFeedsFailed,
                strings = EnFeedFlowStrings,
                onRetryRefresh = { retryCount += 1 },
                onOpenAccounts = {},
            )
        }

        assertNotNull(state.currentFeedback)
        feedback.cancel()
        feedback.join()
        assertEquals(0, retryCount)
        assertNull(state.currentFeedback)
    }

    @Test
    fun `delete and unknown errors are message only`() = runTest {
        val errors = listOf(FeedSyncError.DeleteFeedSourceFailed, NoCode)

        for (error in errors) {
            val state = DesktopFeedbackState()
            val calls = mutableListOf<String>()
            val feedback = async(start = CoroutineStart.UNDISPATCHED) {
                showDesktopSyncFailureFeedback(
                    state = state,
                    errorCode = error,
                    strings = EnFeedFlowStrings,
                    onRetryRefresh = { calls += "retry" },
                    onOpenAccounts = { calls += "accounts" },
                )
            }

            val visible = assertNotNull(state.currentFeedback)
            assertEquals(EnFeedFlowStrings.syncErrorMessage(error), visible.message)
            assertNull(visible.actionLabel)
            assertEquals(DesktopFeedbackDuration.Short, visible.duration)
            visible.dismiss()
            feedback.await()
            assertEquals(emptyList(), calls)
            assertNull(state.currentFeedback)
        }
    }
}
