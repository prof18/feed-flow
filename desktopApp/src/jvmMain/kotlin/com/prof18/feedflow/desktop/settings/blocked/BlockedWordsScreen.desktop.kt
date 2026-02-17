package com.prof18.feedflow.desktop.settings.blocked

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.desktop.ui.components.DesktopDialogWindow
import com.prof18.feedflow.shared.presentation.BlockedWordsViewModel
import com.prof18.feedflow.shared.ui.settings.BlockedWordsContent
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun BlockedWordsScreen(
    visible: Boolean,
    onCloseRequest: () -> Unit,
) {
    val viewModel = koinViewModel<BlockedWordsViewModel>()
    val keywords = viewModel.wordsState.collectAsState().value
    val title = LocalFeedFlowStrings.current.settingsBlockedWords

    DesktopDialogWindow(
        title = title,
        size = DpSize(500.dp, 700.dp),
        visible = visible,
        onCloseRequest = onCloseRequest,
    ) { modifier ->
        BlockedWordsContent(
            keywords = keywords,
            onAddWord = viewModel::onAddWord,
            onRemoveWord = viewModel::onRemoveWord,
            modifier = modifier,
        )
    }
}
