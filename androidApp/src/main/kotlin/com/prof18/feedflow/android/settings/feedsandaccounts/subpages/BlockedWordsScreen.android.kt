package com.prof18.feedflow.android.settings.feedsandaccounts.subpages

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prof18.feedflow.shared.presentation.BlockedWordsViewModel
import com.prof18.feedflow.shared.ui.settings.BlockedWordsScreenContent
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun BlockedWordsScreen(
    navigateBack: () -> Unit,
) {
    val viewModel = koinViewModel<BlockedWordsViewModel>()
    val keywords by viewModel.wordsState.collectAsStateWithLifecycle()

    BlockedWordsScreenContent(
        keywords = keywords,
        onBackClick = navigateBack,
        onAddWord = viewModel::onAddWord,
        onRemoveWord = viewModel::onRemoveWord,
    )
}
