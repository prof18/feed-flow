package com.prof18.feedflow.desktop.home.components

import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.window.DialogWindow
import com.prof18.feedflow.shared.ui.home.components.NoFeedsInfoContent
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings

@Composable
internal fun NoFeedsDialog(
    showDialog: Boolean,
    onDismissRequest: () -> Unit,
    onAddFeedClick: () -> Unit,
    onImportExportClick: () -> Unit,
) {
    val dialogTitle = LocalFeedFlowStrings.current.noFeedModalTitle
    DialogWindow(
        title = dialogTitle,
        visible = showDialog,
        onCloseRequest = onDismissRequest,
    ) {
        Scaffold {
            NoFeedsInfoContent(
                showTitle = false,
                onDismissRequest = onDismissRequest,
                onAddFeedClick = {
                    onDismissRequest()
                    onAddFeedClick()
                },
                onImportExportClick = {
                    onDismissRequest()
                    onImportExportClick()
                },
            )
        }
    }
}
