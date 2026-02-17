package com.prof18.feedflow.desktop.settings.blocked

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import com.prof18.feedflow.shared.presentation.BlockedWordsViewModel
import com.prof18.feedflow.shared.ui.settings.BlockedWordsScreenContent
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun BlockedWordsScreen(
    navigateBack: () -> Unit,
) {
    val viewModel = koinViewModel<BlockedWordsViewModel>()
    val keywords = viewModel.wordsState.collectAsState().value

    BlockedWordsScreenContent(
        keywords = keywords,
        onBackClick = navigateBack,
        onAddWord = viewModel::onAddWord,
        onRemoveWord = viewModel::onRemoveWord,
    )
}
