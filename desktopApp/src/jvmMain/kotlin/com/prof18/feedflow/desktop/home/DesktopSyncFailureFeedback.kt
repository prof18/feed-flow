package com.prof18.feedflow.desktop.home

import com.prof18.feedflow.core.model.ErrorCode
import com.prof18.feedflow.core.model.FeedSyncError
import com.prof18.feedflow.core.model.SyncFeedError
import com.prof18.feedflow.desktop.ui.DesktopFeedbackDuration
import com.prof18.feedflow.desktop.ui.DesktopFeedbackResult
import com.prof18.feedflow.desktop.ui.DesktopFeedbackState
import com.prof18.feedflow.i18n.FeedFlowStrings
import com.prof18.feedflow.shared.presentation.model.syncErrorMessage

internal suspend fun showDesktopSyncFailureFeedback(
    state: DesktopFeedbackState,
    errorCode: ErrorCode,
    strings: FeedFlowStrings,
    onRetryRefresh: () -> Unit,
    onOpenAccounts: () -> Unit,
) {
    val action = when (errorCode) {
        FeedSyncError.FetchFeedSourcesAndCategoriesFailed,
        FeedSyncError.SyncFeedsFailed,
        SyncFeedError.FeedSourcesSyncFailed,
        SyncFeedError.FeedItemsSyncFailed,
        -> SyncFailureAction.RetryRefresh

        FeedSyncError.GReaderBadToken -> SyncFailureAction.OpenAccounts
        else -> null
    }
    val actionLabel = when (action) {
        SyncFailureAction.RetryRefresh -> strings.retryButton
        SyncFailureAction.OpenAccounts -> strings.settingsAccounts
        null -> null
    }
    val duration = if (action == null) DesktopFeedbackDuration.Short else DesktopFeedbackDuration.Long

    val result = state.showFeedback(
        message = strings.syncErrorMessage(errorCode),
        actionLabel = actionLabel,
        duration = duration,
    )
    if (result != DesktopFeedbackResult.ActionPerformed) return

    when (action) {
        SyncFailureAction.RetryRefresh -> onRetryRefresh()
        SyncFailureAction.OpenAccounts -> onOpenAccounts()
        null -> Unit
    }
}

private enum class SyncFailureAction { RetryRefresh, OpenAccounts }
