package com.prof18.feedflow.desktop.home.components

import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.window.DialogWindow
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.prof18.feedflow.desktop.feedsourcelist.FeedSourceListScreen
import com.prof18.feedflow.shared.ui.home.components.NoFeedsInfoContent
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings

@Composable
internal fun NoFeedsDialog(
    showDialog: Boolean,
    onDismissRequest: () -> Unit,
    onImportExportClick: () -> Unit,
) {
    val dialogTitle = LocalFeedFlowStrings.current.noFeedModalTitle
    val navigator = LocalNavigator.currentOrThrow

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
                    navigator.push(FeedSourceListScreen())
                },
                onImportExportClick = {
                    onDismissRequest()
                    onImportExportClick()
                },
            )
        }
    }
}
