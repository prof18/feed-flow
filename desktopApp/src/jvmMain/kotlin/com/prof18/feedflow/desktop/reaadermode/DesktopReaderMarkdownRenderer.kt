@file:OptIn(
    org.jetbrains.jewel.foundation.ExperimentalJewelApi::class,
    androidx.compose.ui.ExperimentalComposeUiApi::class,
)

package com.prof18.feedflow.desktop.reaadermode

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.style.TextOverflow
import org.jetbrains.jewel.markdown.MarkdownBlock
import org.jetbrains.jewel.markdown.extensions.MarkdownRendererExtension
import org.jetbrains.jewel.markdown.rendering.DefaultMarkdownBlockRenderer
import org.jetbrains.jewel.markdown.rendering.MarkdownStyling

internal class DesktopReaderMarkdownRenderer(
    styling: MarkdownStyling,
    extensions: List<MarkdownRendererExtension>,
    private val onHoveredLinkChange: (String?) -> Unit,
) : DefaultMarkdownBlockRenderer(styling, extensions) {
    @Composable
    override fun RenderParagraph(
        block: MarkdownBlock.Paragraph,
        styling: MarkdownStyling.Paragraph,
        enabled: Boolean,
        onUrlClick: (String) -> Unit,
        onTextLayout: (TextLayoutResult) -> Unit,
        modifier: Modifier,
        overflow: TextOverflow,
        softWrap: Boolean,
        maxLines: Int,
    ) {
        var layout by remember(block) { mutableStateOf<TextLayoutResult?>(null) }
        super.RenderParagraph(
            block = block,
            styling = styling,
            enabled = enabled,
            onUrlClick = onUrlClick,
            onTextLayout = {
                layout = it
                onTextLayout(it)
            },
            modifier = modifier
                .onPointerEvent(PointerEventType.Enter) { event ->
                    onHoveredLinkChange(event.changes.firstOrNull()?.position?.let { layout?.hoveredUrl(it) })
                }
                .onPointerEvent(PointerEventType.Move) { event ->
                    onHoveredLinkChange(event.changes.firstOrNull()?.position?.let { layout?.hoveredUrl(it) })
                }
                .onPointerEvent(PointerEventType.Exit) { onHoveredLinkChange(null) },
            overflow = overflow,
            softWrap = softWrap,
            maxLines = maxLines,
        )
    }

    @Composable
    override fun RenderHeading(
        block: MarkdownBlock.Heading,
        styling: MarkdownStyling.Heading.HN,
        enabled: Boolean,
        onUrlClick: (String) -> Unit,
        modifier: Modifier,
    ) {
        RenderParagraph(
            block = MarkdownBlock.Paragraph(block.inlineContent),
            styling = MarkdownStyling.Paragraph(styling.inlinesStyling),
            enabled = enabled,
            onUrlClick = onUrlClick,
            onTextLayout = {},
            modifier = modifier.semantics { heading() },
            overflow = TextOverflow.Clip,
            softWrap = true,
            maxLines = Int.MAX_VALUE,
        )
    }
}

private fun TextLayoutResult.hoveredUrl(position: Offset): String? {
    val text = layoutInput.text
    if (text.isEmpty()) return null
    val offset = getOffsetForPosition(position).coerceIn(0, text.length - 1)
    if (!getBoundingBox(offset).contains(position)) return null
    return when (val link = text.getLinkAnnotations(offset, offset + 1).firstOrNull()?.item) {
        is LinkAnnotation.Clickable -> link.tag
        is LinkAnnotation.Url -> link.url
        else -> null
    }
}
