package com.prof18.feedflow.desktop.reaadermode

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import com.prof18.feedflow.core.model.ArticleReportMessage
import com.prof18.feedflow.core.model.ArticleReportUiState
import com.prof18.feedflow.desktop.ui.DesktopFeedbackDuration
import com.prof18.feedflow.desktop.ui.DesktopFeedbackState
import com.prof18.feedflow.desktop.ui.LocalFeedFlowStrings
import com.prof18.feedflow.desktop.ui.components.ConfirmationDialog

@Composable
internal fun ArticleReportDisclosure(
    state: ArticleReportUiState,
    feedbackState: DesktopFeedbackState,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    onDismissMessage: () -> Unit,
) {
    val strings = LocalFeedFlowStrings.current
    val dismissMessage by rememberUpdatedState(onDismissMessage)
    val message = state.message
    val text = when (message) {
        ArticleReportMessage.SENDING -> strings.articleReportSending
        ArticleReportMessage.SENT -> strings.articleReportSent
        ArticleReportMessage.FAILED -> strings.articleReportFailed
        null -> null
    }
    LaunchedEffect(message, text) {
        if (text != null) {
            feedbackState.showFeedback(
                message = text,
                duration = if (message == ArticleReportMessage.SENDING) {
                    DesktopFeedbackDuration.Indefinite
                } else {
                    DesktopFeedbackDuration.Short
                },
            )
            dismissMessage()
        }
    }

    if (state.showDisclosure) {
        ConfirmationDialog(
            title = strings.articleReportAction,
            message = strings.articleReportDisclosure,
            confirmLabel = strings.articleReportSend,
            onConfirm = onConfirm,
            onDismiss = onCancel,
        )
    }
}
