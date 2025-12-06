package com.prof18.feedflow.android.readermode

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.BookmarkRemove
import androidx.compose.material.icons.filled.Comment
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.prof18.feedflow.core.model.FeedItemId
import com.prof18.feedflow.core.model.ReaderModeState
import com.prof18.feedflow.shared.ui.readermode.SliderWithPlusMinus
import com.prof18.feedflow.shared.ui.readermode.hammerIcon
import com.prof18.feedflow.shared.ui.style.Spacing
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings

@Composable
internal fun ReaderModeToolbar(
    readerModeState: ReaderModeState,
    fontSize: Int,
    navigateBack: () -> Unit,
    openInBrowser: (String) -> Unit,
    onShareClick: (String, String?) -> Unit,
    onArchiveClick: (String) -> Unit,
    onCommentsClick: (String) -> Unit,
    onFontSizeChange: (Int) -> Unit,
    onBookmarkClick: (FeedItemId, Boolean) -> Unit,
) {
    var showFontSizeMenu by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }

    TopAppBar(
        title = {},
        navigationIcon = {
            IconButton(
                onClick = navigateBack,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                )
            }
        },
        actions = {
            Row {
                if (readerModeState !is ReaderModeState.Loading) {
                    val url = readerModeState.getUrl
                    val id = readerModeState.getId
                    var isBookmarked by remember(readerModeState) {
                        mutableStateOf(readerModeState.getIsBookmarked)
                    }

                    if (url != null) {
                        TooltipBox(
                            positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                            state = rememberTooltipState(),
                            tooltip = { PlainTooltip { Text(LocalFeedFlowStrings.current.menuShare) } },
                        ) {
                            IconButton(
                                onClick = {
                                    val title = (readerModeState as? ReaderModeState.Success)?.readerModeData?.title
                                    onShareClick(url, title)
                                },
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = null,
                                )
                            }
                        }

                        TooltipBox(
                            positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                            state = rememberTooltipState(),
                            tooltip = {
                                PlainTooltip {
                                    Text(
                                        LocalFeedFlowStrings.current.readerModeBrowserButtonContentDescription,
                                    )
                                }
                            },
                        ) {
                            IconButton(
                                onClick = {
                                    openInBrowser(url)
                                },
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Language,
                                    contentDescription = null,
                                )
                            }
                        }
                    }

                    Box {
                        IconButton(
                            onClick = {
                                showOverflowMenu = true
                            },
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = null,
                            )
                        }

                        DropdownMenu(
                            expanded = showOverflowMenu,
                            onDismissRequest = {
                                showOverflowMenu = false
                            },
                        ) {
                            if (readerModeState is ReaderModeState.Success) {
                                DropdownMenuItem(
                                    text = { Text(text = LocalFeedFlowStrings.current.readerModeFontSize) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Outlined.TextFields,
                                            contentDescription = null,
                                        )
                                    },
                                    onClick = {
                                        showOverflowMenu = false
                                        showFontSizeMenu = true
                                    },
                                )
                            }

                            if (id != null) {
                                DropdownMenuItem(
                                    text = {
                                        val tooltipText = if (isBookmarked) {
                                            LocalFeedFlowStrings.current.menuRemoveFromBookmark
                                        } else {
                                            LocalFeedFlowStrings.current.menuAddToBookmark
                                        }
                                        Text(text = tooltipText)
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = if (isBookmarked) {
                                                Icons.Default.BookmarkRemove
                                            } else {
                                                Icons.Default.BookmarkAdd
                                            },
                                            contentDescription = null,
                                        )
                                    },
                                    onClick = {
                                        showOverflowMenu = false
                                        isBookmarked = !isBookmarked
                                        onBookmarkClick(FeedItemId(id), isBookmarked)
                                    },
                                )
                            }

                            val archiveLabel = LocalFeedFlowStrings.current.readerModeArchiveButton
                            if (url != null) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = archiveLabel,
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = hammerIcon,
                                            contentDescription = null,
                                        )
                                    },
                                    onClick = {
                                        showOverflowMenu = false
                                        onArchiveClick(url)
                                    },
                                )
                            }

                            if (readerModeState is ReaderModeState.Success) {
                                readerModeState.readerModeData.commentsUrl?.let { commentsUrl ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = LocalFeedFlowStrings.current
                                                    .readerModeCommentsButtonContentDescription,
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Comment,
                                                contentDescription = null,
                                            )
                                        },
                                        onClick = {
                                            showOverflowMenu = false
                                            onCommentsClick(commentsUrl)
                                        },
                                    )
                                }
                            }
                        }
                    }

                    if (readerModeState is ReaderModeState.Success) {
                        DropdownMenu(
                            expanded = showFontSizeMenu,
                            onDismissRequest = {
                                showFontSizeMenu = false
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
            }
        },
    )
}
