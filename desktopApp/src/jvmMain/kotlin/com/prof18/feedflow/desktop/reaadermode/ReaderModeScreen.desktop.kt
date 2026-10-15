@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.prof18.feedflow.desktop.reaadermode

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.Comment
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.RssFeed
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.ImageLoader
import com.prof18.feedflow.core.model.FeedItemId
import com.prof18.feedflow.core.model.FeedItemUrlInfo
import com.prof18.feedflow.core.model.ReaderModeState
import com.prof18.feedflow.core.model.ShownContentSource
import com.prof18.feedflow.desktop.di.DI
import com.prof18.feedflow.desktop.home.toolbarHeight
import com.prof18.feedflow.desktop.ui.DesktopFeedbackDuration
import com.prof18.feedflow.desktop.ui.DesktopFeedbackState
import com.prof18.feedflow.desktop.ui.LocalFeedFlowStrings
import com.prof18.feedflow.desktop.ui.components.DesktopDesignTokens
import com.prof18.feedflow.desktop.ui.components.SnackbarHost
import com.prof18.feedflow.desktop.utils.copyToClipboard
import com.prof18.feedflow.desktop.utils.openUriSafely
import com.prof18.feedflow.shared.presentation.ReaderModeViewModel
import com.prof18.feedflow.shared.utils.getArchiveISUrl
import kotlinx.coroutines.launch
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.IconButton
import org.jetbrains.jewel.ui.component.SelectableIconButton
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.Tooltip
import org.jetbrains.jewel.ui.icon.IconKey
import org.jetbrains.jewel.ui.icons.AllIconsKeys.Actions
import org.jetbrains.jewel.ui.icons.AllIconsKeys.FileTypes
import org.jetbrains.jewel.ui.icons.AllIconsKeys.General
import org.jetbrains.jewel.ui.icons.AllIconsKeys.Toolwindows
import org.koin.compose.viewmodel.koinViewModel

private val readerToolbarActionSize = 32.dp

@Composable
internal fun ReaderModeScreen(
    feedItemUrlInfo: FeedItemUrlInfo,
    navigateBack: () -> Unit,
    showBackButton: Boolean = true,
    isDetailFullscreen: Boolean = false,
    onToggleDetailFullscreen: (() -> Unit)? = null,
) {
    val readerModeViewModel = koinViewModel<ReaderModeViewModel>()
    val imageLoader = remember { DI.koin.get<ImageLoader>() }
    val state by readerModeViewModel.readerModeState.collectAsState()
    val fontSettings by readerModeViewModel.readerFontSettingsState.collectAsState()
    val fontSize = fontSettings.fontSize
    val lineHeight = fontSettings.lineHeight

    LaunchedEffect(feedItemUrlInfo.id) {
        readerModeViewModel.loadReaderContent(feedItemUrlInfo)
    }

    val strings = LocalFeedFlowStrings.current
    val snackbarHostState = remember { DesktopFeedbackState() }
    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current
    fun openExternalUrl(url: String) {
        if (!uriHandler.openUriSafely(url)) {
            scope.launch {
                snackbarHostState.showFeedback(
                    message = strings.browserLaunchError,
                    duration = DesktopFeedbackDuration.Short,
                )
            }
        }
    }

    val canNavigatePrevious by readerModeViewModel.canNavigateToPreviousState.collectAsState()
    val canNavigateNext by readerModeViewModel.canNavigateToNextState.collectAsState()
    var hoveredLink by remember { mutableStateOf<String?>(null) }

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    LaunchedEffect(state) {
        hoveredLink = null
        focusRequester.requestFocus()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(JewelTheme.globalColors.panelBackground)
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { keyEvent ->
                handleKeyEvent(
                    keyEvent = keyEvent,
                    state = state,
                    canNavigatePrevious = canNavigatePrevious,
                    canNavigateNext = canNavigateNext,
                    onNavigatePrevious = { readerModeViewModel.navigateToPreviousArticle() },
                    onNavigateNext = { readerModeViewModel.navigateToNextArticle() },
                )
            },
    ) {
        Column(Modifier.fillMaxSize()) {
            ReaderModeToolbar(
                readerModeState = state,
                fontSize = fontSize,
                navigateBack = navigateBack,
                showBackButton = showBackButton,
                onToggleDetailFullscreen = onToggleDetailFullscreen,
                isDetailFullscreen = isDetailFullscreen,
                openInBrowser = ::openExternalUrl,
                onShareClick = { url ->
                    if (copyToClipboard(url)) {
                        scope.launch {
                            snackbarHostState.showFeedback(
                                message = strings.linkCopiedSuccess,
                                duration = DesktopFeedbackDuration.Short,
                            )
                        }
                    }
                },
                onArchiveClick = { articleUrl -> openExternalUrl(getArchiveISUrl(articleUrl)) },
                onCommentsClick = ::openExternalUrl,
                onFontSizeChange = { readerModeViewModel.updateFontSize(it) },
                lineHeight = lineHeight,
                onLineHeightChange = { readerModeViewModel.updateLineHeight(it) },
                onBookmarkClick = { feedItemId: FeedItemId, isBookmarked: Boolean ->
                    readerModeViewModel.updateBookmarkStatus(feedItemId, isBookmarked)
                },
                onToggleContentSource = { readerModeViewModel.toggleContentSource() },
            )
            Box(
                Modifier.fillMaxWidth().height(1.dp)
                    .background(JewelTheme.globalColors.borders.normal.copy(alpha = 0.35f)),
            )
            DesktopReaderContent(
                state = state,
                fontSize = fontSize,
                lineHeight = lineHeight,
                imageLoader = imageLoader,
                onOpenInBrowser = ::openExternalUrl,
                onHoveredLinkChange = { hoveredLink = it },
                modifier = Modifier.weight(1f).fillMaxSize(),
            )
        }

        hoveredLink?.let { hoveredUrl ->
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = DesktopDesignTokens.spaceSection, bottom = DesktopDesignTokens.spaceSection)
                    .background(JewelTheme.globalColors.panelBackground)
                    .padding(horizontal = DesktopDesignTokens.spaceSection, vertical = DesktopDesignTokens.spaceMedium),
            ) {
                Text(
                    text = hoveredUrl,
                    modifier = Modifier
                        .widthIn(max = 420.dp)
                        .padding(
                            horizontal = DesktopDesignTokens.spaceSection,
                            vertical = DesktopDesignTokens.spaceMedium,
                        ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        SnackbarHost(
            state = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )

        if (state is ReaderModeState.Success || state is ReaderModeState.ContentNotAvailable) {
            val strings = LocalFeedFlowStrings.current

            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = DesktopDesignTokens.spaceSection, bottom = DesktopDesignTokens.spaceSection),
            ) {
                Row(
                    modifier = Modifier
                        .background(JewelTheme.globalColors.panelBackground, RoundedCornerShape(8.dp))
                        .border(
                            width = 1.dp,
                            color = JewelTheme.globalColors.borders.normal.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(8.dp),
                        )
                        .padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(DesktopDesignTokens.spaceMedium),
                ) {
                    ReaderToolbarAction(Actions.Back, strings.previousArticle, canNavigatePrevious) {
                        readerModeViewModel.navigateToPreviousArticle()
                    }
                    ReaderToolbarAction(Actions.Forward, strings.nextArticle, canNavigateNext) {
                        readerModeViewModel.navigateToNextArticle()
                    }
                }
            }
        }
    }
}

private fun handleKeyEvent(
    keyEvent: androidx.compose.ui.input.key.KeyEvent,
    state: ReaderModeState,
    canNavigatePrevious: Boolean,
    canNavigateNext: Boolean,
    onNavigatePrevious: () -> Unit,
    onNavigateNext: () -> Unit,
): Boolean {
    val canNavigate = state is ReaderModeState.Success || state is ReaderModeState.ContentNotAvailable
    if (keyEvent.type == KeyEventType.KeyDown && canNavigate) {
        return when (keyEvent.key) {
            Key.DirectionLeft -> {
                if (canNavigatePrevious) {
                    onNavigatePrevious()
                    true
                } else {
                    false
                }
            }
            Key.DirectionRight -> {
                if (canNavigateNext) {
                    onNavigateNext()
                    true
                } else {
                    false
                }
            }
            else -> false
        }
    }
    return false
}

@Composable
internal fun ReaderModeToolbar(
    readerModeState: ReaderModeState,
    fontSize: Int,
    navigateBack: () -> Unit,
    showBackButton: Boolean,
    onToggleDetailFullscreen: (() -> Unit)?,
    isDetailFullscreen: Boolean,
    openInBrowser: (String) -> Unit,
    onShareClick: (String) -> Unit,
    onArchiveClick: (String) -> Unit,
    onCommentsClick: (String) -> Unit,
    onFontSizeChange: (Int) -> Unit,
    lineHeight: Int,
    onLineHeightChange: (Int) -> Unit,
    onBookmarkClick: (FeedItemId, Boolean) -> Unit,
    onToggleContentSource: () -> Unit,
) {
    val strings = LocalFeedFlowStrings.current
    var showTextSettings by remember { mutableStateOf(false) }
    var isBookmarked by remember(readerModeState) { mutableStateOf(readerModeState.getIsBookmarked) }
    val url = readerModeState.getUrl?.takeIf(String::isNotBlank)
    val itemId = readerModeState.getId
    val contentSource = (readerModeState as? ReaderModeState.Success)?.readerModeData?.shownContentSource
    val canToggleContentSource = (readerModeState as? ReaderModeState.Success)
        ?.readerModeData?.canToggleContentSource == true

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(toolbarHeight)
            .background(JewelTheme.globalColors.panelBackground)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        ReaderNavigationActions(
            navigateBack = navigateBack,
            showBackButton = showBackButton,
            onToggleDetailFullscreen = onToggleDetailFullscreen,
            isDetailFullscreen = isDetailFullscreen,
        )
        Spacer(Modifier.weight(1f))
        if (readerModeState !is ReaderModeState.Loading) {
            if (itemId != null) {
                val description = if (isBookmarked) {
                    strings.menuRemoveFromBookmark
                } else {
                    strings.menuAddToBookmark
                }
                Tooltip(tooltip = { Text(description) }) {
                    SelectableIconButton(
                        selected = isBookmarked,
                        modifier = Modifier.size(readerToolbarActionSize),
                        onClick = {
                            isBookmarked = !isBookmarked
                            onBookmarkClick(FeedItemId(itemId), isBookmarked)
                        },
                    ) {
                        Icon(
                            painter = rememberVectorPainter(
                                if (isBookmarked) Icons.Outlined.Bookmark else Icons.Outlined.BookmarkBorder,
                            ),
                            contentDescription = description,
                            tint = JewelTheme.globalColors.text.normal,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
            if (contentSource != null && canToggleContentSource && readerModeState.getUrl?.isNotBlank() == true) {
                Tooltip(tooltip = { Text(strings.readerContentSourceFeed) }) {
                    SelectableIconButton(
                        selected = contentSource == ShownContentSource.FEED,
                        modifier = Modifier.size(readerToolbarActionSize),
                        onClick = onToggleContentSource,
                    ) {
                        Icon(
                            painter = rememberVectorPainter(Icons.Outlined.RssFeed),
                            contentDescription = strings.readerContentSourceFeed,
                            tint = JewelTheme.globalColors.text.normal,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
            if (url != null) {
                ReaderToolbarAction(Actions.Share, strings.menuShare) { onShareClick(url) }
                ReaderToolbarAction(FileTypes.Archive, strings.readerModeArchiveButton) { onArchiveClick(url) }
            }
            if (readerModeState is ReaderModeState.Success) {
                readerModeState.readerModeData.commentsUrl?.let { commentsUrl ->
                    ReaderToolbarAction(
                        Toolwindows.ToolWindowMessages,
                        strings.readerModeCommentsButtonContentDescription,
                    ) {
                        onCommentsClick(commentsUrl)
                    }
                }
                if (url != null) {
                    ReaderToolbarAction(General.Language, strings.readerModeBrowserButtonContentDescription) {
                        openInBrowser(url)
                    }
                }
                ReaderToolbarAction(General.Settings, strings.readerModeTextSettings) {
                    showTextSettings = true
                }
            }
        }
    }

    if (showTextSettings) {
        ReaderTextSettingsDialog(
            fontSize = fontSize,
            lineHeight = lineHeight,
            onFontSizeChange = onFontSizeChange,
            onLineHeightChange = onLineHeightChange,
            onDismiss = { showTextSettings = false },
        )
    }
}

@Composable
private fun ReaderNavigationActions(
    navigateBack: () -> Unit,
    showBackButton: Boolean,
    onToggleDetailFullscreen: (() -> Unit)?,
    isDetailFullscreen: Boolean,
) {
    val strings = LocalFeedFlowStrings.current
    if (onToggleDetailFullscreen != null) {
        ReaderToolbarAction(
            iconKey = General.Close,
            description = strings.closeButtonContentDescription,
            onClick = navigateBack,
        )
        ReaderToolbarAction(
            iconKey = if (isDetailFullscreen) Actions.Back else General.OpenInToolWindow,
            description = if (isDetailFullscreen) {
                strings.desktopRestoreReaderLayout
            } else {
                strings.desktopExpandReader
            },
            onClick = onToggleDetailFullscreen,
        )
    } else if (showBackButton) {
        ReaderToolbarAction(
            iconKey = Actions.Back,
            description = strings.desktopSearchBackButton,
            onClick = navigateBack,
        )
    }
}

@Composable
private fun ReaderToolbarAction(
    iconKey: IconKey,
    description: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Tooltip(tooltip = { Text(description) }) {
        IconButton(onClick = onClick, enabled = enabled, modifier = Modifier.size(readerToolbarActionSize)) {
            val vector = when (iconKey) {
                Actions.Back -> Icons.AutoMirrored.Outlined.ArrowBack
                Actions.Forward -> Icons.AutoMirrored.Outlined.ArrowForward
                Actions.Share -> Icons.Outlined.Share
                FileTypes.Archive -> Icons.Outlined.LockOpen
                Toolwindows.ToolWindowMessages -> Icons.AutoMirrored.Outlined.Comment
                General.Language -> Icons.Outlined.Language
                General.Settings -> Icons.Outlined.TextFields
                General.Close -> Icons.Outlined.Close
                else -> Icons.Outlined.Fullscreen
            }
            Icon(
                painter = rememberVectorPainter(vector),
                contentDescription = description,
                tint = if (enabled) JewelTheme.globalColors.text.normal else JewelTheme.globalColors.text.disabled,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
