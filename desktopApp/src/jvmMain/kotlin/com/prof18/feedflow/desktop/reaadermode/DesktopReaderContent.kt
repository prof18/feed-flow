package com.prof18.feedflow.desktop.reaadermode

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.ImageLoader
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import com.prof18.feedflow.core.model.AudioEnclosure
import com.prof18.feedflow.core.model.FeedItemUrlInfo
import com.prof18.feedflow.core.model.ReaderModeData
import com.prof18.feedflow.core.model.ReaderModeState
import com.prof18.feedflow.desktop.ui.LocalFeedFlowStrings
import com.prof18.feedflow.desktop.ui.LocalReduceMotion
import com.prof18.feedflow.desktop.ui.components.DesktopDesignTokens
import com.prof18.feedflow.desktop.ui.components.FeedFlowVerticalScrollbar
import com.prof18.feedflow.desktop.ui.components.TopToolbarContentFade
import com.prof18.feedflow.shared.domain.buildReaderModeMarkdown
import com.prof18.feedflow.shared.domain.readerLineHeightToTextLineHeightSp
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
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
    loadingArticle: FeedItemUrlInfo? = null,
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

            ReaderModeState.Loading -> ReaderArticle(
                articleKey = loadingArticle?.id,
                title = loadingArticle?.title,
                siteName = loadingArticle?.feedSourceTitle,
                fontSize = fontSize,
                lineHeight = lineHeight,
            )

            is ReaderModeState.Success -> ReaderArticle(
                articleKey = s.readerModeData.id.id,
                title = s.readerModeData.title,
                siteName = s.readerModeData.siteName,
                fontSize = fontSize,
                lineHeight = lineHeight,
            ) {
                val audioUrl = AudioEnclosure.validatedUrl(s.readerModeData.audioUrl)
                if (audioUrl != null) {
                    AudioEpisodeBanner(
                        title = s.readerModeData.title,
                        onOpenAudio = { onOpenInBrowser(audioUrl) },
                        modifier = Modifier.padding(top = READER_BLOCK_SPACING),
                    )
                }
                ReaderArticleBody(
                    data = s.readerModeData,
                    fontSize = fontSize,
                    lineHeight = lineHeight,
                    imageLoader = imageLoader,
                    onUrlClick = onOpenInBrowser,
                    onHoveredLinkChange = onHoveredLinkChange,
                )
            }
        }

        TopToolbarContentFade(
            modifier = Modifier.align(Alignment.TopCenter),
            height = readerPaneTopContentFadeHeight,
            color = JewelTheme.globalColors.panelBackground,
        )
    }
}

// Loading and loaded articles share this layout: the title and source are plain text, shown at once and never
// re-rendered, so switching articles never blanks the pane. Only the body arrives later.
@Composable
private fun ReaderArticle(
    articleKey: String?,
    title: String?,
    siteName: String?,
    fontSize: Int,
    lineHeight: Int,
    modifier: Modifier = Modifier,
    body: (@Composable () -> Unit)? = null,
) {
    val scrollState = rememberScrollState()
    LaunchedEffect(articleKey) { scrollState.scrollTo(0) }
    Box(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .testTag(READER_DOCUMENT_TAG),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier.widthIn(max = readerModeMaxContentWidth).fillMaxWidth()
                    .padding(horizontal = 32.dp).padding(top = 24.dp, bottom = 88.dp),
            ) {
                ReaderArticleHeader(title = title, siteName = siteName, fontSize = fontSize, lineHeight = lineHeight)
                if (body != null) body() else DelayedReaderSpinner(articleKey)
            }
        }
        FeedFlowVerticalScrollbar(scrollState)
    }
}

@Composable
private fun ReaderArticleHeader(title: String?, siteName: String?, fontSize: Int, lineHeight: Int) {
    val bodyStyle = DesktopDesignTokens.chromeText.copy(
        color = JewelTheme.globalColors.text.normal,
        fontSize = fontSize.sp,
        lineHeight = readerLineHeightToTextLineHeightSp(fontSize, lineHeight).sp,
    )
    Column {
        ReaderArticleHeaderTexts(title = title, siteName = siteName, bodyStyle = bodyStyle, fontSize = fontSize)
    }
}

@Composable
private fun ColumnScope.ReaderArticleHeaderTexts(
    title: String?,
    siteName: String?,
    bodyStyle: TextStyle,
    fontSize: Int,
) {
    if (!title.isNullOrBlank()) {
        // Matches the reader's H1 styling, so the header looks the same as before it moved out of the markdown.
        Text(
            text = title,
            style = bodyStyle.copy(
                fontSize = (fontSize + READER_TITLE_SIZE_DELTA).sp,
                lineHeight = (fontSize + READER_TITLE_LINE_HEIGHT_DELTA).sp,
                fontWeight = FontWeight.SemiBold,
            ),
        )
    }
    if (!siteName.isNullOrBlank()) {
        Text(
            text = siteName,
            modifier = Modifier.padding(top = READER_BLOCK_SPACING),
            style = bodyStyle.copy(fontWeight = FontWeight.SemiBold),
        )
    }
}

// The body appears once its lead image is ready (or after a short wait), so the image does not push the text down
// after it is drawn, and it fades in instead of popping.
@Composable
private fun ReaderArticleBody(
    data: ReaderModeData,
    fontSize: Int,
    lineHeight: Int,
    imageLoader: ImageLoader,
    onUrlClick: (String) -> Unit,
    onHoveredLinkChange: (String?) -> Unit,
) {
    val markdown = remember(data) {
        buildReaderModeMarkdown(content = data.content, title = null, imageUrl = data.imageUrl, siteName = null)
            .withoutInlineSvg()
    }
    val leadImageUrl = remember(markdown) { leadingImageUrl(markdown) }
    var ready by remember(data.id, data.content) { mutableStateOf(leadImageUrl == null) }
    val context = LocalPlatformContext.current
    LaunchedEffect(data.id, data.content) {
        if (leadImageUrl != null) {
            withTimeoutOrNull(LEAD_IMAGE_WAIT_MILLIS) {
                imageLoader.execute(ImageRequest.Builder(context).data(leadImageUrl).build())
            }
            ready = true
        }
    }
    if (!ready) {
        DelayedReaderSpinner(data.id.id)
        return
    }
    val reduceMotion = LocalReduceMotion.current
    val alpha = remember(data.id, data.content) { Animatable(if (reduceMotion) 1f else 0f) }
    LaunchedEffect(data.id, data.content) { alpha.animateTo(1f, tween(BODY_FADE_IN_MILLIS)) }
    key(data.id, data.content) {
        DesktopReaderMarkdown(
            markdown = markdown,
            fontSize = fontSize,
            lineHeight = lineHeight,
            imageLoader = imageLoader,
            onUrlClick = onUrlClick,
            onHoveredLinkChange = onHoveredLinkChange,
            modifier = Modifier.padding(top = READER_BLOCK_SPACING).graphicsLayer { this.alpha = alpha.value },
        )
    }
}

// The spinner appears only when loading is slow, so quick switches never flash.
@Composable
private fun DelayedReaderSpinner(articleKey: String?) {
    var showSpinner by remember(articleKey) { mutableStateOf(false) }
    LaunchedEffect(articleKey) {
        delay(READER_SPINNER_DELAY_MILLIS)
        showSpinner = true
    }
    Box(
        modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (showSpinner) CircularProgressIndicator(Modifier.testTag(READER_LOADING_TAG))
    }
}

// Klead keeps inline SVG (diagrams, logos) because web renderers can draw it. Jewel's Markdown cannot and prints
// the markup as text, whether it lands in an HTML block or inline in a paragraph, so the desktop reader drops it.
internal fun String.withoutInlineSvg(): String = replace(inlineSvgRegex, "")

private val inlineSvgRegex = Regex("<svg\\b.*?</svg\\s*>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))

private fun leadingImageUrl(markdown: String): String? {
    val window = markdown.take(LEAD_IMAGE_SCAN_WINDOW)
    return markdownImageRegex.find(window)?.groupValues?.get(1)
        ?: htmlImageRegex.find(window)?.groupValues?.get(1)
}

private val markdownImageRegex = Regex("""!\[[^]]*]\(\s*<?([^)\s>]+)""")
private val htmlImageRegex = Regex("""<img\b[^>]*\bsrc\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE)

private const val LEAD_IMAGE_SCAN_WINDOW = 1000
private const val LEAD_IMAGE_WAIT_MILLIS = 500L
private const val BODY_FADE_IN_MILLIS = 150
private const val READER_TITLE_SIZE_DELTA = 16
private const val READER_TITLE_LINE_HEIGHT_DELTA = 24
private val READER_BLOCK_SPACING = 16.dp
private const val READER_SPINNER_DELAY_MILLIS = 300L

@Composable
private fun ReaderModeFallbackContent(
    onOpenInBrowser: (() -> Unit)?,
    modifier: Modifier = Modifier,
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(key = General.Language, contentDescription = null)
                    Text(strings.readerModeFallbackOpenBrowserButton)
                }
            }
        }
    }
}
