package com.prof18.feedflow.desktop.reaadermode

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.ImageLoader
import com.composeunstyled.rememberScrollbarState
import com.prof18.feedflow.core.model.AudioEnclosure
import com.prof18.feedflow.core.model.ReaderModeState
import com.prof18.feedflow.desktop.ui.LocalFeedFlowStrings
import com.prof18.feedflow.desktop.ui.components.DesktopDesignTokens
import com.prof18.feedflow.desktop.ui.components.FeedFlowVerticalScrollbar
import com.prof18.feedflow.desktop.ui.components.TopToolbarContentFade
import com.prof18.feedflow.shared.domain.buildReaderModeMarkdown
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.CircularProgressIndicator
import org.jetbrains.jewel.ui.component.DefaultButton
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.icons.AllIconsKeys.General

private val readerModeMaxContentWidth = 720.dp
private val readerPaneTopContentFadeHeight = 30.dp
internal const val READER_LOADING_TAG = "reader-loading"
internal const val READER_DOCUMENT_TAG = "reader-document"

@Composable
internal fun DesktopReaderContent(
    state: ReaderModeState,
    fontSize: Int,
    lineHeight: Int,
    imageLoader: ImageLoader,
    onOpenInBrowser: (String) -> Unit,
    onHoveredLinkChange: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.TopCenter,
    ) {
        val contentModifier = Modifier
            .widthIn(max = readerModeMaxContentWidth)
            .fillMaxWidth()

        when (val s = state) {
            is ReaderModeState.ContentNotAvailable -> {
                ReaderModeFallbackContent(
                    modifier = contentModifier
                        .fillMaxHeight(),
                    onOpenInBrowser = s.url.takeIf { it.isNotBlank() }?.let { url ->
                        {
                            onOpenInBrowser(url)
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
                    CircularProgressIndicator(modifier = Modifier.testTag(READER_LOADING_TAG))
                }
            }

            is ReaderModeState.Success -> {
                val scrollState = rememberScrollState()
                val scrollbarState = rememberScrollbarState(scrollState)

                Box(
                    modifier = Modifier.fillMaxSize(),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState).testTag(READER_DOCUMENT_TAG),
                        contentAlignment = Alignment.TopCenter,
                    ) {
                        Column(
                            modifier = contentModifier,
                        ) {
                            val audioUrl = AudioEnclosure.validatedUrl(s.readerModeData.audioUrl)
                            if (audioUrl != null) {
                                AudioEpisodeBanner(
                                    title = s.readerModeData.title,
                                    fontSize = fontSize,
                                    onOpenAudio = { onOpenInBrowser(audioUrl) },
                                    modifier = Modifier.padding(start = 32.dp, end = 32.dp, top = 24.dp),
                                )
                            }
                            key(s.readerModeData.id, s.readerModeData.content) {
                                val markdown = remember(s.readerModeData) {
                                    buildReaderModeMarkdown(
                                        content = s.readerModeData.content,
                                        title = s.readerModeData.title.takeIf { audioUrl == null },
                                        imageUrl = s.readerModeData.imageUrl,
                                        siteName = s.readerModeData.siteName,
                                    )
                                }
                                DesktopReaderMarkdown(
                                    markdown = markdown,
                                    fontSize = fontSize,
                                    lineHeight = lineHeight,
                                    imageLoader = imageLoader,
                                    onUrlClick = onOpenInBrowser,
                                    onHoveredLinkChange = onHoveredLinkChange,
                                    modifier = Modifier
                                        .padding(horizontal = 32.dp, vertical = 24.dp)
                                        .padding(bottom = 64.dp),
                                )
                            }
                        }
                    }

                    FeedFlowVerticalScrollbar(scrollbarState)
                }
            }
        }

        TopToolbarContentFade(
            modifier = Modifier.align(Alignment.TopCenter),
            height = readerPaneTopContentFadeHeight,
            color = JewelTheme.globalColors.panelBackground,
        )
    }
}

@Composable
private fun ReaderModeFallbackContent(
    modifier: Modifier = Modifier,
    onOpenInBrowser: (() -> Unit)?,
) {
    val strings = LocalFeedFlowStrings.current
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            key = General.Language,
            contentDescription = null,
            modifier = Modifier.padding(bottom = DesktopDesignTokens.spaceSection),
        )
        val hasUrl = onOpenInBrowser != null
        Text(
            text = if (hasUrl) strings.readerModeFallbackTitle else strings.readerModeNoContentTitle,
            modifier = Modifier.padding(bottom = DesktopDesignTokens.spaceMedium),
        )
        Text(
            text = if (hasUrl) strings.readerModeFallbackMessage else strings.readerModeNoContentMessage,
            modifier = Modifier
                .padding(start = DesktopDesignTokens.spaceReading, end = DesktopDesignTokens.spaceReading)
                .padding(bottom = DesktopDesignTokens.spaceSection),
            textAlign = TextAlign.Center,
        )
        if (onOpenInBrowser != null) {
            DefaultButton(onClick = onOpenInBrowser) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(key = General.Language, contentDescription = null)
                    Text(strings.readerModeFallbackOpenBrowserButton)
                }
            }
        }
    }
}
