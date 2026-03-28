package com.prof18.feedflow.android.readermode

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.BookmarkRemove
import androidx.compose.material.icons.filled.Comment
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material3.AppBarRow
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import com.prof18.feedflow.core.model.FeedItemId
import com.prof18.feedflow.core.model.ReaderModeState
import com.prof18.feedflow.shared.ui.readermode.SliderWithPlusMinus
import com.prof18.feedflow.shared.ui.readermode.hammerIcon
import com.prof18.feedflow.shared.ui.style.Spacing
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings

@Composable
fun ReaderModeFloatingToolbar(
    readerModeState: ReaderModeState,
    fontSize: Int,
    expanded: Boolean,
    canNavigatePrevious: Boolean,
    canNavigateNext: Boolean,
    onNavigateToPrevious: () -> Unit,
    onNavigateToNext: () -> Unit,
    onBookmarkClick: (FeedItemId, Boolean) -> Unit,
    openInBrowser: (String) -> Unit,
    onShareClick: (String, String?) -> Unit,
    onArchiveClick: (String) -> Unit,
    onCommentsClick: (String) -> Unit,
    onFontSizeChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showFontSizeMenu by remember { mutableStateOf(false) }
    val strings = LocalFeedFlowStrings.current

    HorizontalFloatingToolbar(
        modifier = modifier,
        expanded = expanded,
        trailingContent = {
            if (readerModeState !is ReaderModeState.Loading) {
                val url = readerModeState.getUrl
                val id = readerModeState.getId
                var isBookmarked by remember(readerModeState) {
                    mutableStateOf(readerModeState.getIsBookmarked)
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

                AppBarRow(
                    modifier = Modifier,
                ) {
                    if (readerModeState is ReaderModeState.Success) {
                        clickableItem(
                            onClick = { showFontSizeMenu = true },
                            icon = {
                                Icon(
                                    imageVector = Icons.Outlined.TextFields,
                                    contentDescription = null,
                                )
                            },
                            label = strings.readerModeFontSize,
                        )
                    }

                    if (id != null) {
                        val label = if (isBookmarked) {
                            strings.menuRemoveFromBookmark
                        } else {
                            strings.menuAddToBookmark
                        }

                        clickableItem(
                            onClick = {
                                isBookmarked = !isBookmarked
                                onBookmarkClick(FeedItemId(id), isBookmarked)
                            },
                            icon = {
                                Icon(
                                    imageVector = if (isBookmarked) {
                                        Icons.Default.BookmarkRemove
                                    } else {
                                        Icons.Default.BookmarkAdd
                                    },
                                    contentDescription = label,
                                )
                            },
                            label = label,
                        )
                    }

                    val archiveLabel = strings.readerModeArchiveButton
                    if (url != null) {
                        clickableItem(
                            onClick = { onArchiveClick(url) },
                            icon = {
                                Icon(
                                    imageVector = hammerIcon,
                                    contentDescription = archiveLabel,
                                )
                            },
                            label = archiveLabel,
                        )
                    }

                    if (readerModeState is ReaderModeState.Success) {
                        readerModeState.readerModeData.commentsUrl?.let { commentsUrl ->

                            clickableItem(
                                onClick = { onCommentsClick(commentsUrl) },
                                icon = {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Comment,
                                        contentDescription = strings.readerModeCommentsButtonContentDescription,
                                    )
                                },
                                label = strings.readerModeCommentsButtonContentDescription,
                            )
                        }
                    }
                }
            }
        },
        leadingContent = {
            if (readerModeState !is ReaderModeState.Loading) {
                val url = readerModeState.getUrl

                if (url != null) {
                    AppBarRow {
                        clickableItem(
                            onClick = {
                                val title = (readerModeState as? ReaderModeState.Success)?.readerModeData?.title
                                onShareClick(url, title)
                            },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = strings.menuShare,
                                )
                            },
                            label = strings.menuShare,
                        )

                        clickableItem(
                            onClick = { openInBrowser(url) },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.Language,
                                    contentDescription = strings.readerModeBrowserButtonContentDescription,
                                )
                            },
                            label = strings.readerModeBrowserButtonContentDescription,
                        )
                    }
                }
            }
        },
        content = {
            TooltipBox(
                positionProvider =
                TooltipDefaults.rememberTooltipPositionProvider(
                    TooltipAnchorPosition.Above,
                ),
                tooltip = { PlainTooltip { Text(strings.previousArticle) } },
                state = rememberTooltipState(),
            ) {

                IconButton(
                    modifier = Modifier.focusProperties { canFocus = expanded },
                    onClick = onNavigateToPrevious,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = strings.previousArticle,
                    )
                }
            }

            TooltipBox(
                positionProvider =
                    TooltipDefaults.rememberTooltipPositionProvider(
                        TooltipAnchorPosition.Above,
                    ),
                tooltip = { PlainTooltip { Text(strings.nextArticle) } },
                state = rememberTooltipState(),
            ) {

                IconButton(
                    modifier = Modifier.focusProperties { canFocus = expanded },
                    onClick = onNavigateToNext,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = strings.nextArticle,
                    )
                }
            }
        },
    )
}
