package com.prof18.feedflow.android.readermode

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.BookmarkRemove
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.core.model.FeedItemId
import com.prof18.feedflow.core.model.ReaderModeState
import com.prof18.feedflow.shared.ui.readermode.SliderWithPlusMinus
import com.prof18.feedflow.shared.ui.readermode.hammerIcon
import com.prof18.feedflow.shared.ui.style.Spacing
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

private data class ToolbarAction(
    val icon: ImageVector,
    val label: String,
    val testTag: String? = null,
    val onClick: () -> Unit,
)

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
    var showOverflowMenu by remember { mutableStateOf(false) }
    val strings = LocalFeedFlowStrings.current
    val isDarkTheme = isSystemInDarkTheme()
    val toolbarContainerColor = if (isDarkTheme) {
        MaterialTheme.colorScheme.surfaceContainerHighest
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val toolbarBorder = if (isDarkTheme) {
        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    } else {
        null
    }

    val url = readerModeState.getUrl
    val id = readerModeState.getId
    val latestOpenInBrowser by rememberUpdatedState(openInBrowser)
    val latestOnShareClick by rememberUpdatedState(onShareClick)
    val latestOnBookmarkClick by rememberUpdatedState(onBookmarkClick)
    val latestOnArchiveClick by rememberUpdatedState(onArchiveClick)
    val latestOnCommentsClick by rememberUpdatedState(onCommentsClick)
    var isBookmarked by remember(readerModeState) {
        mutableStateOf(readerModeState.getIsBookmarked)
    }

    val isContentVisible = expanded && readerModeState !is ReaderModeState.Loading

    // Build action lists regardless of isContentVisible so AnimatedVisibility can animate exit
    // Order: Browser, Share | < > | Bookmark, Comments, Archive, Font Size
    val leadingActions = remember(readerModeState, strings, url) {
        buildList {
            if (readerModeState !is ReaderModeState.Loading && url != null) {
                add(
                    ToolbarAction(
                        icon = Icons.Default.Language,
                        label = strings.readerModeBrowserButtonContentDescription,
                        testTag = ReaderModeE2eIds.BROWSER_BUTTON,
                        onClick = { latestOpenInBrowser(url) },
                    ),
                )
                add(
                    ToolbarAction(
                        icon = Icons.Default.Share,
                        label = strings.menuShare,
                        onClick = {
                            val title = (readerModeState as? ReaderModeState.Success)?.readerModeData?.title
                            latestOnShareClick(url, title)
                        },
                    ),
                )
            }
        }
            .toImmutableList()
    }

    val trailingActions = remember(readerModeState, strings, url, id, isBookmarked) {
        buildList {
            if (id != null) {
                val bookmarkLabel = if (isBookmarked) {
                    strings.menuRemoveFromBookmark
                } else {
                    strings.menuAddToBookmark
                }
                val bookmarkIcon = if (isBookmarked) {
                    Icons.Default.BookmarkRemove
                } else {
                    Icons.Default.BookmarkAdd
                }
                add(
                    ToolbarAction(
                        icon = bookmarkIcon,
                        label = bookmarkLabel,
                        testTag = ReaderModeE2eIds.BOOKMARK_BUTTON,
                        onClick = {
                            val newIsBookmarked = !isBookmarked
                            isBookmarked = newIsBookmarked
                            latestOnBookmarkClick(FeedItemId(id), newIsBookmarked)
                        },
                    ),
                )
            }
            if (readerModeState is ReaderModeState.Success) {
                readerModeState.readerModeData.commentsUrl?.let { commentsUrl ->
                    add(
                        ToolbarAction(
                            icon = Icons.AutoMirrored.Filled.Comment,
                            label = strings.readerModeCommentsButtonContentDescription,
                            onClick = { latestOnCommentsClick(commentsUrl) },
                        ),
                    )
                }
            }
            if (url != null) {
                add(
                    ToolbarAction(
                        icon = hammerIcon,
                        label = strings.readerModeArchiveButton,
                        onClick = { latestOnArchiveClick(url) },
                    ),
                )
            }
            if (readerModeState is ReaderModeState.Success) {
                add(
                    ToolbarAction(
                        icon = Icons.Outlined.TextFields,
                        label = strings.readerModeFontSize,
                        testTag = ReaderModeE2eIds.FONT_SIZE_BUTTON,
                        onClick = { showFontSizeMenu = true },
                    ),
                )
            }
        }
            .toImmutableList()
    }

    Surface(
        modifier = modifier,
        shape = FloatingToolbarDefaults.ContainerShape,
        color = toolbarContainerColor,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shadowElevation = 6.dp,
        border = toolbarBorder,
    ) {
        Box {
            OverflowToolbarLayout(
                leadingActions = leadingActions,
                trailingActions = trailingActions,
                isContentVisible = isContentVisible,
                showOverflowMenu = showOverflowMenu,
                onShowOverflowMenu = { showOverflowMenu = it },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            ) {
                IconButton(
                    modifier = Modifier
                        .focusProperties { canFocus = expanded }
                        .testTag(ReaderModeE2eIds.PREVIOUS_BUTTON),
                    enabled = canNavigatePrevious,
                    onClick = onNavigateToPrevious,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = strings.previousArticle,
                    )
                }

                IconButton(
                    modifier = Modifier
                        .focusProperties { canFocus = expanded }
                        .testTag(ReaderModeE2eIds.NEXT_BUTTON),
                    enabled = canNavigateNext,
                    onClick = onNavigateToNext,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = strings.nextArticle,
                    )
                }
            }

            DropdownMenu(
                expanded = showFontSizeMenu,
                onDismissRequest = { showFontSizeMenu = false },
                modifier = Modifier.testTag(ReaderModeE2eIds.FONT_SIZE_MENU),
                shape = MaterialTheme.shapes.large,
            ) {
                Column(modifier = Modifier.padding(Spacing.regular)) {
                    Text(
                        text = strings.readerModeFontSize,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    SliderWithPlusMinus(
                        value = fontSize.toFloat(),
                        onValueChange = { onFontSizeChange(it.toInt()) },
                        valueRange = 12f..40f,
                        steps = 40,
                    )
                }
            }
        }
    }
}

@Composable
private fun OverflowToolbarLayout(
    leadingActions: ImmutableList<ToolbarAction>,
    trailingActions: ImmutableList<ToolbarAction>,
    isContentVisible: Boolean,
    showOverflowMenu: Boolean,
    onShowOverflowMenu: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    fixedContent: @Composable () -> Unit,
) {
    SubcomposeLayout(modifier) { constraints ->
        // Measure a sample IconButton to get standard item dimensions
        val samplePlaceable = subcompose("sample") {
            IconButton(onClick = {}) { Icon(Icons.Default.MoreVert, null) }
        }.first().measure(constraints.copy(minWidth = 0))
        val itemWidth = samplePlaceable.width

        val fixedCount = 2 // prev + next
        val fixedWidth = fixedCount * itemWidth

        val allActions = leadingActions + trailingActions
        val availableForActions = if (constraints.hasBoundedWidth) {
            constraints.maxWidth - fixedWidth
        } else {
            Int.MAX_VALUE
        }
        val maxActionSlots = when {
            availableForActions == Int.MAX_VALUE -> allActions.size
            itemWidth <= 0 -> 0
            else -> (availableForActions / itemWidth).coerceAtLeast(0)
        }

        val needsOverflow = allActions.size > maxActionSlots
        val visibleCount = if (needsOverflow) {
            (maxActionSlots - 1).coerceAtLeast(0)
        } else {
            allActions.size
        }

        // Split visible count: leading first, then trailing fills remaining
        val visibleLeading = minOf(visibleCount, leadingActions.size)
        val visibleTrailing = (visibleCount - visibleLeading).coerceIn(0, trailingActions.size)
        val overflowActions = leadingActions.drop(visibleLeading) + trailingActions.drop(visibleTrailing)

        val contentPlaceable = subcompose("content") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AnimatedVisibility(
                    visible = isContentVisible && visibleLeading > 0,
                    enter = fadeIn() + expandHorizontally(),
                    exit = shrinkHorizontally() + fadeOut(),
                ) {
                    Row {
                        leadingActions.take(visibleLeading).forEach { action ->
                            ToolbarActionButton(action)
                        }
                    }
                }

                fixedContent()

                AnimatedVisibility(
                    visible = isContentVisible && (visibleTrailing > 0 || needsOverflow),
                    enter = fadeIn() + expandHorizontally(),
                    exit = shrinkHorizontally() + fadeOut(),
                ) {
                    Row {
                        trailingActions.take(visibleTrailing).forEach { action ->
                            ToolbarActionButton(action)
                        }

                        if (needsOverflow) {
                            Box {
                                IconButton(
                                    modifier = Modifier.testTag(ReaderModeE2eIds.MORE_MENU_BUTTON),
                                    onClick = { onShowOverflowMenu(true) },
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = null,
                                    )
                                }
                                DropdownMenu(
                                    expanded = showOverflowMenu,
                                    onDismissRequest = { onShowOverflowMenu(false) },
                                    shape = MaterialTheme.shapes.large,
                                ) {
                                    overflowActions.forEach { action ->
                                        ToolbarActionMenuItem(
                                            action = action,
                                            onClick = {
                                                action.onClick()
                                                onShowOverflowMenu(false)
                                            },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }.first().measure(constraints.copy(minWidth = 0))

        layout(contentPlaceable.width, contentPlaceable.height) {
            contentPlaceable.placeRelative(0, 0)
        }
    }
}

@Composable
private fun ToolbarActionButton(action: ToolbarAction) {
    IconButton(
        modifier = action.testTagModifier(),
        onClick = action.onClick,
    ) {
        Icon(
            imageVector = action.icon,
            contentDescription = action.label,
            tint = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun ToolbarActionMenuItem(
    action: ToolbarAction,
    onClick: () -> Unit,
) {
    DropdownMenuItem(
        modifier = action.testTagModifier(),
        text = { Text(action.label) },
        onClick = onClick,
        leadingIcon = {
            Icon(
                imageVector = action.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
    )
}

private fun ToolbarAction.testTagModifier(): Modifier =
    testTag?.let { Modifier.testTag(it) } ?: Modifier
