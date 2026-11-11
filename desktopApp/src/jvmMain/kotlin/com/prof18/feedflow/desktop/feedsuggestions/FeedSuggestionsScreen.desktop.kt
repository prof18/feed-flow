package com.prof18.feedflow.desktop.feedsuggestions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.desktop.ui.LocalFeedFlowStrings
import com.prof18.feedflow.desktop.ui.components.Dialog
import com.prof18.feedflow.shared.presentation.FeedSuggestionsViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun FeedSuggestionsScreen(onCloseRequest: () -> Unit) {
    val viewModel = koinViewModel<FeedSuggestionsViewModel>()
    val categories by viewModel.suggestedCategoriesState.collectAsState()
    val selectedCategoryId by viewModel.selectedCategoryIdState.collectAsState()
    val isLoading by viewModel.isLoadingState.collectAsState()
    val operationStates by viewModel.operationStates.collectAsState()
    val undoOfferState by viewModel.undoOfferState.collectAsState()
    DisposableEffect(viewModel) {
        onDispose { viewModel.endVisit() }
    }
    FeedSuggestionsWindow(onCloseRequest) { modifier ->
        FeedSuggestionsContent(
            categories,
            selectedCategoryId,
            isLoading,
            viewModel::selectCategory,
            { feed, category -> viewModel.addFeed(feed, category, DESKTOP_UNDO_TIMEOUT_MILLIS) },
            onCloseRequest,
            modifier,
            operationStates = operationStates,
            undoOfferState = undoOfferState,
            onUndo = viewModel::undoLastAdd,
        )
    }
}

private const val DESKTOP_UNDO_TIMEOUT_MILLIS = 30_000L

@Composable
internal fun FeedSuggestionsWindow(onCloseRequest: () -> Unit, content: @Composable (Modifier) -> Unit) {
    Dialog(
        title = LocalFeedFlowStrings.current.feedSuggestionsTitle,
        size = DpSize(840.dp, 720.dp),
        onCloseRequest = onCloseRequest,
        extendBehindTitleBar = true,
    ) { content(Modifier) }
}
