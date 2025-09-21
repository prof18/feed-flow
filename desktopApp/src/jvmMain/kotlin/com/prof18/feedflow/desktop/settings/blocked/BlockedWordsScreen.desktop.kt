package com.prof18.feedflow.desktop.settings.blocked

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.prof18.feedflow.desktop.desktopViewModel
import com.prof18.feedflow.desktop.di.DI
import com.prof18.feedflow.shared.presentation.BlockedWordsViewModel
import com.prof18.feedflow.shared.ui.settings.BlockedWordsScreenContent

internal class BlockedWordsScreen : Screen {

    @Composable
    override fun Content() {
        val viewModel = desktopViewModel { DI.koin.get<BlockedWordsViewModel>() }
        val navigator = LocalNavigator.currentOrThrow
        val keywords = viewModel.wordsState.collectAsState().value

        BlockedWordsScreenContent(
            keywords = keywords,
            onBackClick = { navigator.pop() },
            onAddWord = viewModel::onAddWord,
            onRemoveWord = viewModel::onRemoveWord,
        )
    }
}
