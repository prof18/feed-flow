package com.prof18.feedflow.desktop.reaadermode

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.mikepenz.markdown.coil3.Coil3ImageTransformerImpl
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownTypography
import com.prof18.feedflow.core.model.FeedItemId
import com.prof18.feedflow.core.model.FeedItemUrlInfo
import com.prof18.feedflow.desktop.desktopViewModel
import com.prof18.feedflow.desktop.di.DI
import com.prof18.feedflow.desktop.utils.copyToClipboard
import com.prof18.feedflow.shared.presentation.ReaderModeViewModel
import com.prof18.feedflow.shared.ui.readermode.ReaderModeContent
import com.prof18.feedflow.shared.ui.style.Spacing
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings
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

        ReaderModeContent(
            readerModeState = state,
            navigateBack = {
                navigator.pop()
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            openInBrowser = { url ->
                uriHandler.openUri(url)
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
            fontSize = fontSize,
            onFontSizeChange = {
                readerModeViewModel.updateFontSize(it)
            },
            readerModeSuccessView = { contentPadding, successState ->
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

                    Markdown(
                        modifier = Modifier
                            .padding(Spacing.regular),
                        content = successState.readerModeData.content,
                        imageTransformer = Coil3ImageTransformerImpl,
                        typography = markdownTypography(
                            h1 = MaterialTheme.typography.displaySmall,
                            h2 = MaterialTheme.typography.titleLarge,
                            h3 = MaterialTheme.typography.titleLarge,
                            h4 = MaterialTheme.typography.titleMedium,
                            h5 = MaterialTheme.typography.titleMedium,
                            h6 = MaterialTheme.typography.titleMedium,
                            paragraph = MaterialTheme.typography.bodyLarge.copy(
                                lineHeight = (fontSize + 15).sp,
                                fontSize = fontSize.sp,
                            ),
                        ),
                    )
                }
            },
            onBookmarkClick = { feedItemId: FeedItemId, isBookmarked: Boolean ->
                readerModeViewModel.updateBookmarkStatus(feedItemId, isBookmarked)
            },
        )
    }
}
