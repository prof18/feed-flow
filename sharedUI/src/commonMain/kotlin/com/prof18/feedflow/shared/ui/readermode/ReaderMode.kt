package com.prof18.feedflow.shared.ui.readermode

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.BookmarkRemove
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.prof18.feedflow.core.model.FeedItemId
import com.prof18.feedflow.core.model.ReaderModeState
import com.prof18.feedflow.shared.ui.style.Spacing
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings

@Composable
fun ReaderModeContent(
    readerModeState: ReaderModeState,
    fontSize: Int,
    navigateBack: () -> Unit,
    openInBrowser: (String) -> Unit,
    onShareClick: (String) -> Unit,
    onArchiveClick: (articleUrl: String) -> Unit,
    modifier: Modifier = Modifier,
    onFontSizeChange: (Int) -> Unit,
    onBookmarkClick: (FeedItemId, Boolean) -> Unit,
    snackbarHost: @Composable () -> Unit = {},
    readerModeSuccessView: @Composable (PaddingValues, ReaderModeState.Success) -> Unit,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            ReaderModeToolbar(
                readerModeState = readerModeState,
                fontSize = fontSize,
                navigateBack = navigateBack,
                openInBrowser = openInBrowser,
                onShareClick = onShareClick,
                onArchiveClick = onArchiveClick,
                onFontSizeChange = onFontSizeChange,
                onBookmarkClick = onBookmarkClick,
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
    readerModeState: ReaderModeState,
    fontSize: Int,
    navigateBack: () -> Unit,
    openInBrowser: (String) -> Unit,
    onShareClick: (String) -> Unit,
    onArchiveClick: (articleUrl: String) -> Unit,
    onFontSizeChange: (Int) -> Unit,
    onBookmarkClick: (FeedItemId, Boolean) -> Unit,
) {
    var showMenu by remember { mutableStateOf(false) }

    TopAppBar(
        title = {},
        navigationIcon = {
            IconButton(
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
                if (readerModeState is ReaderModeState.HtmlNotAvailable) {
                    var isBookmarked by remember {
                        mutableStateOf(readerModeState.isBookmarked)
                    }
                    BookmarkButton(
                        isBookmarked = isBookmarked,
                        onClick = {
                            isBookmarked = !isBookmarked
                            onBookmarkClick(FeedItemId(readerModeState.id), isBookmarked)
                        },
                    )
                }

                if (readerModeState is ReaderModeState.Success) {
                    var isBookmarked by remember {
                        mutableStateOf(readerModeState.readerModeData.isBookmarked)
                    }

                    BookmarkButton(
                        isBookmarked = isBookmarked,
                        onClick = {
                            isBookmarked = !isBookmarked
                            onBookmarkClick(readerModeState.readerModeData.id, isBookmarked)
                        },
                    )

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

                    IconButton(
                        onClick = {
                            onArchiveClick(readerModeState.readerModeData.url)
                        },
                    ) {
                        Icon(
                            imageVector = hammerIcon,
                            contentDescription = LocalFeedFlowStrings.current.readerModeArchiveButtonContentDescription,
                        )
                    }

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
                            showMenu = true
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.TextFields,
                            contentDescription = null,
                        )
                    }

                    DropdownMenu(
                        modifier = Modifier
                            .fillMaxWidth(fraction = 0.8f),
                        expanded = showMenu,
                        onDismissRequest = {
                            showMenu = false
                        },
                    ) {
                        Column(
                            modifier = Modifier.padding(Spacing.regular),
                        ) {
                            Text(
                                text = LocalFeedFlowStrings.current.readerModeFontSize,
                                style = MaterialTheme.typography.titleMedium,
                            )

                            SliderWithPlusMinus(
                                value = fontSize.toFloat(),
                                onValueChange = {
                                    onFontSizeChange(it.toInt())
                                },
                                valueRange = 12f..40f,
                                steps = 40,
                            )
                        }
                    }
                }
            }
        },
    )
}

@Composable
private fun BookmarkButton(
    isBookmarked: Boolean,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
    ) {
        Icon(
            imageVector = if (isBookmarked) {
                Icons.Default.BookmarkRemove
            } else {
                Icons.Default.BookmarkAdd
            },
            contentDescription = null,
        )
    }
}
