package com.prof18.feedflow.desktop.reaadermode

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.BookmarkRemove
import androidx.compose.material.icons.filled.Comment
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
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.mikepenz.markdown.coil3.Coil3ImageTransformerImpl
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownTypography
import com.prof18.feedflow.core.model.FeedItemId
import com.prof18.feedflow.core.model.FeedItemUrlInfo
import com.prof18.feedflow.core.model.ReaderModeState
import com.prof18.feedflow.desktop.desktopViewModel
import com.prof18.feedflow.desktop.di.DI
import com.prof18.feedflow.desktop.utils.copyToClipboard
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
            when (val s = state) {
                is ReaderModeState.HtmlNotAvailable -> {
                    navigator.pop()
                    if (isValidUrl(s.url)) {
                        uriHandler.openUri(s.url)
                    }
                }
                ReaderModeState.Loading -> {
                    androidx.compose.foundation.layout.Box(
                        contentAlignment = androidx.compose.ui.Alignment.Center,
                        modifier = Modifier
                            .padding(contentPadding)
                            .fillMaxWidth(),
                    ) {
                        androidx.compose.material3.CircularProgressIndicator()
                    }
                }
                is ReaderModeState.Success -> {
                    Column(
                        modifier = Modifier
                            .padding(contentPadding)
                            .verticalScroll(rememberScrollState()),

                    ) {
                        Text(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = Spacing.regular),
                            text = LocalFeedFlowStrings.current.readerModeWarning,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Light,
                        )

                        SelectionContainer {
                            Markdown(
                                modifier = Modifier
                                    .padding(Spacing.regular),
                                content = s.readerModeData.content,
                                imageTransformer = Coil3ImageTransformerImpl,
                                typography = markdownTypography(
                                    h1 = MaterialTheme.typography.displaySmall.copy(
                                        fontSize = (fontSize + 20).sp, // Default: 36sp (16 + 20)
                                        lineHeight = (fontSize + 32).sp, // Default: 48sp (16 + 32)
                                    ),
                                    h2 = MaterialTheme.typography.titleLarge.copy(
                                        fontSize = (fontSize + 6).sp, // Default: 22sp (16 + 6)
                                        lineHeight = (fontSize + 16).sp, // Default: 32sp (16 + 16)
                                    ),
                                    h3 = MaterialTheme.typography.titleLarge.copy(
                                        fontSize = (fontSize + 6).sp, // Default: 22sp (16 + 6)
                                        lineHeight = (fontSize + 16).sp, // Default: 32sp (16 + 16)
                                    ),
                                    h4 = MaterialTheme.typography.titleMedium.copy(
                                        fontSize = fontSize.sp, // Default: 16sp (same as base)
                                        lineHeight = (fontSize + 12).sp, // Default: 28sp (16 + 12)
                                    ),
                                    h5 = MaterialTheme.typography.titleMedium.copy(
                                        fontSize = fontSize.sp, // Default: 16sp (same as base)
                                        lineHeight = (fontSize + 12).sp, // Default: 28sp (16 + 12)
                                    ),
                                    h6 = MaterialTheme.typography.titleMedium.copy(
                                        fontSize = fontSize.sp, // Default: 16sp (same as base)
                                        lineHeight = (fontSize + 12).sp, // Default: 28sp (16 + 12)
                                    ),
                                    paragraph = MaterialTheme.typography.bodyLarge.copy(
                                        fontSize = fontSize.sp, // Default: 16sp (base)
                                        lineHeight = (fontSize + 12).sp, // Default: 28sp (16 + 12)
                                    ),
                                    text = MaterialTheme.typography.bodyLarge.copy(
                                        fontSize = fontSize.sp, // Default: 16sp (base)
                                        lineHeight = (fontSize + 12).sp, // Default: 28sp (16 + 12)
                                    ),
                                    code = MaterialTheme.typography.bodyMedium.copy(
                                        fontSize = (fontSize - 2).sp, // Default: 14sp (16 - 2)
                                        lineHeight = (fontSize + 8).sp, // Default: 24sp (16 + 8)
                                    ),
                                    list = MaterialTheme.typography.bodyLarge.copy(
                                        fontSize = fontSize.sp, // Default: 16sp (base)
                                        lineHeight = (fontSize + 12).sp, // Default: 28sp (16 + 12)
                                    ),
                                    textLink = TextLinkStyles(
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontSize = fontSize.sp, // Default: 16sp (base)
                                            lineHeight = (fontSize + 12).sp, // Default: 28sp (16 + 12)
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

                    TooltipBox(
                        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                        state = rememberTooltipState(),
                        tooltip = { PlainTooltip { Text(LocalFeedFlowStrings.current.menuShare) } },
                    ) {
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

                    TooltipBox(
                        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                        state = rememberTooltipState(),
                        tooltip = {
                            PlainTooltip {
                                Text(
                                    LocalFeedFlowStrings.current.readerModeArchiveButtonContentDescription,
                                )
                            }
                        },
                    ) {
                        IconButton(
                            onClick = {
                                onArchiveClick(readerModeState.readerModeData.url)
                            },
                        ) {
                            val label = LocalFeedFlowStrings.current.readerModeArchiveButtonContentDescription
                            Icon(
                                imageVector = hammerIcon,
                                contentDescription = label,
                            )
                        }
                    }

                    readerModeState.readerModeData.commentsUrl?.let { commentsUrl ->
                        TooltipBox(
                            positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
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
                                    imageVector = Icons.Default.Comment,
                                    contentDescription = null,
                                )
                            }
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
                        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
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
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
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
