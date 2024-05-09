package com.prof18.feedflow.shared.ui.readermode

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.prof18.feedflow.core.model.ReaderModeState
import com.prof18.feedflow.core.utils.TestingTag
import com.prof18.feedflow.shared.ui.utils.tagForTesting

@Composable
fun ReaderModeContent(
    readerModeState: ReaderModeState,
    navigateBack: () -> Unit,
    openInBrowser: (String) -> Unit,
    onShareClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    snackbarHost: @Composable () -> Unit = {},
    readerModeSuccessView: @Composable (PaddingValues, ReaderModeState.Success) -> Unit,
) {
    var toolbarTitle by remember {
        mutableStateOf("")
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            ReaderModeToolbar(
                toolbarTitle = toolbarTitle,
                readerModeState = readerModeState,
                navigateBack = navigateBack,
                openInBrowser = openInBrowser,
                onShareClick = onShareClick,
            )
        },
        snackbarHost = snackbarHost,
    ) { contentPadding ->
        when (readerModeState) {
            is ReaderModeState.HtmlNotAvailable -> {
                navigateBack()
                openInBrowser(readerModeState.url)
            }

            ReaderModeState.Loading -> {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .padding(contentPadding)
                        .fillMaxSize(),
                ) {
                    CircularProgressIndicator()
                }
            }

            is ReaderModeState.Success -> {
                toolbarTitle = readerModeState.readerModeData.title ?: ""
                readerModeSuccessView(
                    contentPadding,
                    readerModeState,
                )
            }
        }
    }
}

@Composable
private fun ReaderModeToolbar(
    toolbarTitle: String,
    readerModeState: ReaderModeState,
    navigateBack: () -> Unit,
    openInBrowser: (String) -> Unit,
    onShareClick: (String) -> Unit,
) {
    TopAppBar(
        title = {
            Text(
                text = toolbarTitle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        navigationIcon = {
            IconButton(
                modifier = Modifier
                    .tagForTesting(TestingTag.BACK_BUTTON),
                onClick = navigateBack,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Default.ArrowBack,
                    contentDescription = null,
                )
            }
        },
        actions = {
            Row {
                if (readerModeState is ReaderModeState.Success) {
                    IconButton(
                        onClick = {
                            openInBrowser(readerModeState.readerModeData.url)
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = null,
                        )
                    }

                    IconButton(
                        onClick = {
                            onShareClick(readerModeState.readerModeData.url)
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                        )
                    }
                }
            }
        },
    )
}
