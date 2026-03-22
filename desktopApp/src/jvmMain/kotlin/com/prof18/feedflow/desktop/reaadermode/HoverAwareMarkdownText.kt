package com.prof18.feedflow.desktop.reaadermode

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import com.mikepenz.markdown.annotator.AnnotatorSettings
import com.mikepenz.markdown.annotator.annotatorSettings
import com.mikepenz.markdown.annotator.buildMarkdownAnnotatedString
import com.mikepenz.markdown.compose.elements.MarkdownText
import org.intellij.markdown.IElementType
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.ast.findChildOfType

@Composable
internal fun HoverAwareMarkdownNodeText(
    content: String,
    node: ASTNode,
    style: TextStyle,
    onHoveredLinkChange: (String?) -> Unit,
    modifier: Modifier = Modifier,
    annotatorSettings: AnnotatorSettings = annotatorSettings(),
    contentChildType: IElementType? = null,
) {
    val childNode = contentChildType?.run(node::findChildOfType) ?: node
    val styledText = buildAnnotatedString {
        pushStyle(style.toSpanStyle())
        buildMarkdownAnnotatedString(
            content = content,
            node = childNode,
            annotatorSettings = annotatorSettings,
        )
        pop()
    }

    HoverAwareMarkdownText(
        content = styledText,
        style = style,
        onHoveredLinkChange = onHoveredLinkChange,
        modifier = modifier,
    )
}

@Composable
@OptIn(ExperimentalComposeUiApi::class)
internal fun HoverAwareMarkdownText(
    content: AnnotatedString,
    style: TextStyle,
    onHoveredLinkChange: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var layoutResult by remember(content) { mutableStateOf<TextLayoutResult?>(null) }

    MarkdownText(
        content = content,
        style = style,
        modifier = modifier
            .onPointerEvent(PointerEventType.Move) { pointerEvent ->
                val position = pointerEvent.changes.firstOrNull()?.position ?: return@onPointerEvent
                onHoveredLinkChange(layoutResult.findHoveredUrl(content, position))
            }
            .onPointerEvent(PointerEventType.Exit) {
                onHoveredLinkChange(null)
            },
        onTextLayout = { result, _ ->
            layoutResult = result
        },
    )
}

private fun TextLayoutResult?.findHoveredUrl(
    content: AnnotatedString,
    position: Offset,
): String? {
    val result = this ?: return null
    if (content.isEmpty()) return null

    val offset = result.getOffsetForPosition(position).coerceIn(0, content.length - 1)

    val link = content.getLinkAnnotations(offset, offset + 1).firstOrNull()?.item as? LinkAnnotation.Url
    return link?.url
}
