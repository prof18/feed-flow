package com.prof18.feedflow.desktop.reaadermode

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.BookmarkRemove
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.mikepenz.markdown.coil3.Coil3ImageTransformerImpl
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownTypography
import com.mikepenz.markdown.model.markdownAnimations
import com.prof18.feedflow.core.model.FeedItemId
import com.prof18.feedflow.core.model.FeedItemUrlInfo
import com.prof18.feedflow.core.model.ReaderModeState
import com.prof18.feedflow.desktop.desktopViewModel
import com.prof18.feedflow.desktop.di.DI
import com.prof18.feedflow.desktop.utils.copyToClipboard
import com.prof18.feedflow.desktop.utils.generateUniqueKey
import com.prof18.feedflow.shared.presentation.ReaderModeViewModel
import com.prof18.feedflow.shared.ui.readermode.SliderWithPlusMinus
import com.prof18.feedflow.shared.ui.readermode.hammerIcon
import com.prof18.feedflow.shared.ui.style.Spacing
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings
import com.prof18.feedflow.shared.utils.getArchiveISUrl
import com.prof18.feedflow.shared.utils.isValidUrl
import kotlinx.coroutines.launch

internal data class ReaderModeScreen(
    private val feedItemUrlInfo: FeedItemUrlInfo,
) : Screen {
    override val key: String = generateUniqueKey()

    @Composable
    override fun Content() {
        val readerModeViewModel = desktopViewModel { DI.koin.get<ReaderModeViewModel>() }
        val state by readerModeViewModel.readerModeState.collectAsState()
        val fontSize by readerModeViewModel.readerFontSizeState.collectAsState()
        val navigator = LocalNavigator.currentOrThrow

        LaunchedEffect(feedItemUrlInfo) {
            readerModeViewModel.getReaderModeHtml(feedItemUrlInfo)
        }

        val snackbarHostState = remember { SnackbarHostState() }
        val scope = rememberCoroutineScope()

        val message = LocalFeedFlowStrings.current.linkCopiedSuccess
        val uriHandler = LocalUriHandler.current

        val canNavigatePrevious by readerModeViewModel.canNavigateToPreviousState.collectAsState()
        val canNavigateNext by readerModeViewModel.canNavigateToNextState.collectAsState()

        val focusRequester = remember { FocusRequester() }

        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
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
            Scaffold(
                topBar = {
                    ReaderModeToolbar(
                        readerModeState = state,
                        fontSize = fontSize,
                        navigateBack = { navigator.pop() },
                        openInBrowser = { url ->
                            if (isValidUrl(url)) {
                                uriHandler.openUri(url)
                            }
                        },
                        onShareClick = { url ->
                            val result = copyToClipboard(url)
                            if (result) {
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        message = message,
                                        duration = SnackbarDuration.Short,
                                    )
                                }
                            }
                        },
                        onArchiveClick = { articleUrl ->
                            val archiveUrl = getArchiveISUrl(articleUrl)
                            if (isValidUrl(archiveUrl)) {
                                uriHandler.openUri(archiveUrl)
                            }
                        },
                        onCommentsClick = { commentsUrl ->
                            if (isValidUrl(commentsUrl)) {
                                uriHandler.openUri(commentsUrl)
                            }
                        },
                        onFontSizeChange = { readerModeViewModel.updateFontSize(it) },
                        onBookmarkClick = { feedItemId: FeedItemId, isBookmarked: Boolean ->
                            readerModeViewModel.updateBookmarkStatus(feedItemId, isBookmarked)
                        },
                    )
                },
                snackbarHost = { SnackbarHost(snackbarHostState) },
            ) { contentPadding ->
                BoxWithConstraints(
                    modifier = Modifier
                        .padding(contentPadding)
                        .fillMaxSize(),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    val isWideScreen = maxWidth > 840.dp
                    val contentModifier = if (isWideScreen) {
                        Modifier.fillMaxWidth(0.6f)
                    } else {
                        Modifier.fillMaxWidth()
                    }

                    when (val s = state) {
                        is ReaderModeState.HtmlNotAvailable -> {
                            ReaderModeFallbackContent(
                                modifier = contentModifier
                                    .fillMaxHeight(),
                                onOpenInBrowser = {
                                    if (isValidUrl(s.url)) {
                                        uriHandler.openUri(s.url)
                                    }
                                },
                            )
                        }

                        ReaderModeState.Loading -> {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .fillMaxSize(),
                            ) {
                                androidx.compose.material3.CircularProgressIndicator()
                            }
                        }

                        is ReaderModeState.Success -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState()),
                                contentAlignment = Alignment.TopCenter,
                            ) {
                                Column(
                                    modifier = contentModifier,
                                ) {
                                    Text(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = Spacing.regular),
                                        text = LocalFeedFlowStrings.current.readerModeWarning,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Light,
                                    )

                                    key(s.readerModeData.content, fontSize) {
                                        SelectionContainer {
                                            Markdown(
                                                modifier = Modifier
                                                    .padding(Spacing.regular)
                                                    .padding(bottom = 64.dp),
                                                content = s.readerModeData.content,
                                                imageTransformer = Coil3ImageTransformerImpl,
                                                animations = markdownAnimations(
                                                    animateTextSize = {
                                                        this
                                                        /** No animation */
                                                    },
                                                ),
                                                typography = markdownTypography(
                                                    h1 = MaterialTheme.typography.displaySmall.copy(
                                                        fontSize = (fontSize + 20).sp,
                                                        lineHeight = (fontSize + 32).sp,
                                                    ),
                                                    h2 = MaterialTheme.typography.titleLarge.copy(
                                                        fontSize = (fontSize + 6).sp,
                                                        lineHeight = (fontSize + 16).sp,
                                                    ),
                                                    h3 = MaterialTheme.typography.titleLarge.copy(
                                                        fontSize = (fontSize + 6).sp,
                                                        lineHeight = (fontSize + 16).sp,
                                                    ),
                                                    h4 = MaterialTheme.typography.titleMedium.copy(
                                                        fontSize = fontSize.sp,
                                                        lineHeight = (fontSize + 12).sp,
                                                    ),
                                                    h5 = MaterialTheme.typography.titleMedium.copy(
                                                        fontSize = fontSize.sp,
                                                        lineHeight = (fontSize + 12).sp,
                                                    ),
                                                    h6 = MaterialTheme.typography.titleMedium.copy(
                                                        fontSize = fontSize.sp,
                                                        lineHeight = (fontSize + 12).sp,
                                                    ),
                                                    paragraph = MaterialTheme.typography.bodyLarge.copy(
                                                        fontSize = fontSize.sp,
                                                        lineHeight = (fontSize + 12).sp,
                                                    ),
                                                    text = MaterialTheme.typography.bodyLarge.copy(
                                                        fontSize = fontSize.sp,
                                                        lineHeight = (fontSize + 12).sp,
                                                    ),
                                                    code = MaterialTheme.typography.bodyMedium.copy(
                                                        fontSize = (fontSize - 2).sp,
                                                        lineHeight = (fontSize + 8).sp,
                                                    ),
                                                    list = MaterialTheme.typography.bodyLarge.copy(
                                                        fontSize = fontSize.sp,
                                                        lineHeight = (fontSize + 12).sp,
                                                    ),
                                                    textLink = TextLinkStyles(
                                                        style = MaterialTheme.typography.bodyLarge.copy(
                                                            fontSize = fontSize.sp,
                                                            lineHeight = (fontSize + 12).sp,
                                                            fontWeight = FontWeight.Bold,
                                                            textDecoration = TextDecoration.Underline,
                                                        ).toSpanStyle(),
                                                    ),
                                                ),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (state is ReaderModeState.Success || state is ReaderModeState.HtmlNotAvailable) {
                val strings = LocalFeedFlowStrings.current

                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = Spacing.regular, bottom = Spacing.regular)
                        .clip(RoundedCornerShape(12.dp)),
                    tonalElevation = 3.dp,
                    shadowElevation = 8.dp,
                ) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = Spacing.small),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.small),
                    ) {
                        TooltipBox(
                            positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                                positioning = TooltipAnchorPosition.Above,
                            ),
                            state = rememberTooltipState(),
                            tooltip = { PlainTooltip { Text(strings.previousArticle) } },
                        ) {
                            IconButton(
                                onClick = { readerModeViewModel.navigateToPreviousArticle() },
                                enabled = canNavigatePrevious,
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = strings.previousArticle,
                                )
                            }
                        }

                        TooltipBox(
                            positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                                positioning = TooltipAnchorPosition.Above,
                            ),
                            state = rememberTooltipState(),
                            tooltip = { PlainTooltip { Text(strings.nextArticle) } },
                        ) {
                            IconButton(
                                onClick = { readerModeViewModel.navigateToNextArticle() },
                                enabled = canNavigateNext,
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = strings.nextArticle,
                                )
                            }
                        }
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
    val canNavigate = state is ReaderModeState.Success || state is ReaderModeState.HtmlNotAvailable
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
private fun ReaderModeToolbar(
    readerModeState: ReaderModeState,
    fontSize: Int,
    navigateBack: () -> Unit,
    openInBrowser: (String) -> Unit,
    onShareClick: (String) -> Unit,
    onArchiveClick: (String) -> Unit,
    onCommentsClick: (String) -> Unit,
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

                    if (id != null) {
                        BookmarkButton(
                            isBookmarked = isBookmarked,
                            onClick = {
                                isBookmarked = !isBookmarked
                                onBookmarkClick(FeedItemId(id), isBookmarked)
                            },
                        )
                    }

                    if (url != null) {
                        TooltipBox(
                            positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                                positioning = TooltipAnchorPosition.Above,
                            ),
                            state = rememberTooltipState(),
                            tooltip = { PlainTooltip { Text(LocalFeedFlowStrings.current.menuShare) } },
                        ) {
                            IconButton(
                                onClick = {
                                    onShareClick(url)
                                },
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = null,
                                )
                            }
                        }

                        TooltipBox(
                            positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                                positioning = TooltipAnchorPosition.Above,
                            ),
                            state = rememberTooltipState(),
                            tooltip = {
                                PlainTooltip {
                                    Text(
                                        LocalFeedFlowStrings.current.readerModeArchiveButton,
                                    )
                                }
                            },
                        ) {
                            IconButton(
                                onClick = {
                                    onArchiveClick(url)
                                },
                            ) {
                                Icon(
                                    imageVector = hammerIcon,
                                    contentDescription = LocalFeedFlowStrings.current.readerModeArchiveButton,
                                )
                            }
                        }
                    }

                    if (readerModeState is ReaderModeState.Success) {
                        readerModeState.readerModeData.commentsUrl?.let { commentsUrl ->
                            TooltipBox(
                                positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                                    positioning = TooltipAnchorPosition.Above,
                                ),
                                state = rememberTooltipState(),
                                tooltip = {
                                    PlainTooltip {
                                        Text(
                                            LocalFeedFlowStrings.current.readerModeCommentsButtonContentDescription,
                                        )
                                    }
                                },
                            ) {
                                IconButton(
                                    onClick = {
                                        onCommentsClick(commentsUrl)
                                    },
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Comment,
                                        contentDescription = null,
                                    )
                                }
                            }
                        }

                        TooltipBox(
                            positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                                positioning = TooltipAnchorPosition.Above,
                            ),
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
                                    openInBrowser(readerModeState.readerModeData.url)
                                },
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Language,
                                    contentDescription = null,
                                )
                            }
                        }

                        TooltipBox(
                            positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                                positioning = TooltipAnchorPosition.Above,
                            ),
                            state = rememberTooltipState(),
                            tooltip = { PlainTooltip { Text(LocalFeedFlowStrings.current.readerModeFontSize) } },
                        ) {
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
                        }

                        DropdownMenu(
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
            }
        },
    )
}

@Composable
private fun BookmarkButton(
    isBookmarked: Boolean,
    onClick: () -> Unit,
) {
    val tooltipText = if (isBookmarked) {
        LocalFeedFlowStrings.current.menuRemoveFromBookmark
    } else {
        LocalFeedFlowStrings.current.menuAddToBookmark
    }
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
            positioning = TooltipAnchorPosition.Above,
        ),
        state = rememberTooltipState(),
        tooltip = { PlainTooltip { Text(tooltipText) } },
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
}

@Composable
private fun ReaderModeFallbackContent(
    modifier: Modifier = Modifier,
    onOpenInBrowser: () -> Unit,
) {
    val strings = LocalFeedFlowStrings.current

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Default.Language,
            contentDescription = null,
            modifier = Modifier.padding(bottom = Spacing.regular),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Text(
            text = strings.readerModeFallbackTitle,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = Spacing.small),
        )

        Text(
            text = strings.readerModeFallbackMessage,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .padding(start = Spacing.large, end = Spacing.large)
                .padding(bottom = Spacing.regular),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )

        androidx.compose.material3.Button(
            onClick = onOpenInBrowser,
        ) {
            Icon(
                imageVector = Icons.Default.Language,
                contentDescription = null,
                modifier = Modifier.padding(end = Spacing.small),
            )
            Text(strings.readerModeFallbackOpenBrowserButton)
        }
    }
}
