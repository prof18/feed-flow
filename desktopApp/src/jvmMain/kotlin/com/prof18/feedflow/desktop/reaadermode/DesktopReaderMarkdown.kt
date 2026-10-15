@file:OptIn(org.jetbrains.jewel.foundation.ExperimentalJewelApi::class)

package com.prof18.feedflow.desktop.reaadermode

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import coil3.ImageLoader
import com.prof18.feedflow.desktop.ui.components.DesktopDesignTokens
import com.prof18.feedflow.shared.domain.readerLineHeightToTextLineHeightSp
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.intui.markdown.standalone.ProvideMarkdownStyling
import org.jetbrains.jewel.intui.markdown.standalone.styling.extensions.github.tables.dark
import org.jetbrains.jewel.intui.markdown.standalone.styling.extensions.github.tables.light
import org.jetbrains.jewel.markdown.Markdown
import org.jetbrains.jewel.markdown.extensions.autolink.AutolinkProcessorExtension
import org.jetbrains.jewel.markdown.extensions.github.strikethrough.GitHubStrikethroughProcessorExtension
import org.jetbrains.jewel.markdown.extensions.github.strikethrough.GitHubStrikethroughRendererExtension
import org.jetbrains.jewel.markdown.extensions.github.tables.GfmTableStyling
import org.jetbrains.jewel.markdown.extensions.github.tables.GitHubTableProcessorExtension
import org.jetbrains.jewel.markdown.extensions.github.tables.GitHubTableRendererExtension
import org.jetbrains.jewel.markdown.extensions.images.Coil3ImageRendererExtension
import org.jetbrains.jewel.markdown.processing.MarkdownProcessor

@Composable
internal fun DesktopReaderMarkdown(
    markdown: String,
    fontSize: Int,
    lineHeight: Int,
    imageLoader: ImageLoader,
    onUrlClick: (String) -> Unit,
    onHoveredLinkChange: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dark = JewelTheme.isDark
    val bodyStyle = DesktopDesignTokens.chromeText.copy(
        color = JewelTheme.globalColors.text.normal,
        fontSize = fontSize.sp,
        lineHeight = readerLineHeightToTextLineHeightSp(fontSize, lineHeight).sp,
    )
    val codeStyle = bodyStyle.copy(
        fontFamily = FontFamily.Monospace,
        fontSize = (fontSize - 2).sp,
        lineHeight = (fontSize + 8).sp,
    )
    val styling = remember(dark, bodyStyle, codeStyle) {
        desktopMarkdownStyling(bodyStyle = bodyStyle, codeStyle = codeStyle, dark = dark)
    }
    val processor = remember {
        MarkdownProcessor(
            extensions = listOf(
                GitHubTableProcessorExtension,
                GitHubStrikethroughProcessorExtension(),
                AutolinkProcessorExtension,
            ),
        )
    }
    val latestClick by rememberUpdatedState(onUrlClick)
    val latestHover by rememberUpdatedState(onHoveredLinkChange)
    val click: (String) -> Unit = remember { { latestClick(it) } }
    val renderer = remember(styling, dark, imageLoader) {
        val tables = if (dark) GfmTableStyling.dark() else GfmTableStyling.light()
        DesktopReaderMarkdownRenderer(
            styling = styling,
            extensions = listOf(
                GitHubTableRendererExtension(tables, styling),
                GitHubStrikethroughRendererExtension,
                Coil3ImageRendererExtension(imageLoader),
            ),
            onHoveredLinkChange = { latestHover(it) },
        )
    }
    ProvideMarkdownStyling(
        isDark = dark,
        markdownStyling = styling,
        markdownProcessor = processor,
        markdownBlockRenderer = renderer,
    ) {
        // Reset parsed blocks and image slots when changing documents, retaining them for text-size changes.
        key(markdown) {
            Markdown(markdown = markdown, modifier = modifier, selectable = true, onUrlClick = click)
        }
    }
}
