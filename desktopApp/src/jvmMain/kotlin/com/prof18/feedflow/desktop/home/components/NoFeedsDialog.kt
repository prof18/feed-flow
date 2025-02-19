package com.prof18.feedflow.desktop.home.components

import androidx.compose.foundation.LocalScrollbarStyle
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.rememberDialogState
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.prof18.feedflow.desktop.addfeed.AddFeedFullScreen
import com.prof18.feedflow.desktop.ui.components.scrollbarStyle
import com.prof18.feedflow.shared.ui.home.components.NoFeedsInfoContent
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings

@Composable
internal fun NoFeedsDialog(
    showDialog: Boolean,
    onDismissRequest: () -> Unit,
    onImportExportClick: () -> Unit,
    onAccountsClick: () -> Unit,
) {
    val dialogTitle = LocalFeedFlowStrings.current.noFeedModalTitle
    val navigator = LocalNavigator.currentOrThrow
    val dialogState = rememberDialogState(
        size = DpSize(400.dp, 400.dp),
    )

    DialogWindow(
        state = dialogState,
        title = dialogTitle,
        visible = showDialog,
        onCloseRequest = onDismissRequest,
    ) {
        Scaffold { paddingValues ->
            val listState = rememberLazyListState()
            val scrollState = rememberScrollState()
            Box(
                modifier = Modifier.fillMaxSize()
                    .padding(paddingValues)
                    .padding(end = 4.dp),
            ) {
                NoFeedsInfoContent(
                    modifier = Modifier
                        .verticalScroll(scrollState),
                    showTitle = false,
                    onDismissRequest = onDismissRequest,
                    onAddFeedClick = {
                        onDismissRequest()
                        navigator.push(
                            AddFeedFullScreen(
                                onFeedAdded = {
                                    onDismissRequest()
                                },
                            ),
                        )
                    },
                    onImportExportClick = {
                        onDismissRequest()
                        onImportExportClick()
                    },
                    onAccountsClick = {
                        onDismissRequest()
                        onAccountsClick()
                    },
                )

                CompositionLocalProvider(LocalScrollbarStyle provides scrollbarStyle()) {
                    VerticalScrollbar(
                        modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                        adapter = rememberScrollbarAdapter(
                            scrollState = listState,
                        ),
                    )
                }
            }
        }
    }
}
