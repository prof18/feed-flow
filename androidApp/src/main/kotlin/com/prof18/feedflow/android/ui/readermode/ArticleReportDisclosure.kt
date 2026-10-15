package com.prof18.feedflow.android.ui.readermode

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.android.ui.utils.LocalFeedFlowStrings
import com.prof18.feedflow.android.ui.utils.exposeTestTagsAsResourceIds
import com.prof18.feedflow.core.model.ArticleReportMessage
import com.prof18.feedflow.core.model.ArticleReportUiState

@Composable
fun ArticleReportDisclosure(
    state: ArticleReportUiState,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    onDismissMessage: () -> Unit,
    modifier: Modifier = Modifier,
    messageBottomPadding: Dp = 0.dp,
) {
    val strings = LocalFeedFlowStrings.current
    val snackbarHostState = remember { SnackbarHostState() }
    val dismissMessage by rememberUpdatedState(onDismissMessage)
    val message = state.message
    val text = when (message) {
        ArticleReportMessage.SENDING -> strings.articleReportSending
        ArticleReportMessage.SENT -> strings.articleReportSent
        ArticleReportMessage.FAILED -> strings.articleReportFailed
        null -> null
    }
    LaunchedEffect(message, text) {
        snackbarHostState.currentSnackbarData?.dismiss()
        if (text != null) {
            snackbarHostState.showSnackbar(
                message = text,
                duration = if (message == ArticleReportMessage.SENDING) {
                    SnackbarDuration.Indefinite
                } else {
                    SnackbarDuration.Short
                },
            )
            dismissMessage()
        }
    }

    Box(modifier.fillMaxSize()) {
        if (state.showDisclosure) {
            AlertDialog(
                modifier = Modifier.exposeTestTagsAsResourceIds(),
                onDismissRequest = onCancel,
                title = { Text(strings.articleReportAction) },
                text = { Text(strings.articleReportDisclosure) },
                confirmButton = {
                    TextButton(
                        modifier = Modifier.testTag("reader-report-send"),
                        onClick = onConfirm,
                    ) {
                        Text(strings.articleReportSend)
                    }
                },
                dismissButton = {
                    TextButton(
                        modifier = Modifier.testTag("reader-report-cancel"),
                        onClick = onCancel,
                    ) {
                        Text(strings.cancelButton)
                    }
                },
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = messageBottomPadding),
        ) { data ->
            Snackbar(data, modifier = Modifier.testTag("reader-report-message"))
        }
    }
}
