package com.prof18.feedflow.desktop.reaadermode

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import com.mikepenz.markdown.compose.components.MarkdownComponents
import com.mikepenz.markdown.compose.components.markdownComponents
import org.intellij.markdown.IElementType
import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.ast.ASTNode

@Composable
internal fun desktopReaderModeMarkdownComponents(
    onHoveredLinkChange: (String?) -> Unit,
): MarkdownComponents = markdownComponents(
    paragraph = {
        HoverAwareMarkdownNodeText(
            content = it.content,
            node = it.node,
            style = it.typography.paragraph,
            onHoveredLinkChange = onHoveredLinkChange,
        )
    },
    heading1 = {
        ReaderModeHeading(
            content = it.content,
            node = it.node,
            style = it.typography.h1,
            onHoveredLinkChange = onHoveredLinkChange,
        )
    },
    heading2 = {
        ReaderModeHeading(
            content = it.content,
            node = it.node,
            style = it.typography.h2,
            onHoveredLinkChange = onHoveredLinkChange,
        )
    },
    heading3 = {
        ReaderModeHeading(
            content = it.content,
            node = it.node,
            style = it.typography.h3,
            onHoveredLinkChange = onHoveredLinkChange,
        )
    },
    heading4 = {
        ReaderModeHeading(
            content = it.content,
            node = it.node,
            style = it.typography.h4,
            onHoveredLinkChange = onHoveredLinkChange,
        )
    },
    heading5 = {
        ReaderModeHeading(
            content = it.content,
            node = it.node,
            style = it.typography.h5,
            onHoveredLinkChange = onHoveredLinkChange,
        )
    },
    heading6 = {
        ReaderModeHeading(
            content = it.content,
            node = it.node,
            style = it.typography.h6,
            onHoveredLinkChange = onHoveredLinkChange,
        )
    },
    setextHeading1 = {
        ReaderModeHeading(
            content = it.content,
            node = it.node,
            style = it.typography.h1,
            onHoveredLinkChange = onHoveredLinkChange,
            contentChildType = MarkdownTokenTypes.SETEXT_CONTENT,
        )
    },
    setextHeading2 = {
        ReaderModeHeading(
            content = it.content,
            node = it.node,
            style = it.typography.h2,
            onHoveredLinkChange = onHoveredLinkChange,
            contentChildType = MarkdownTokenTypes.SETEXT_CONTENT,
        )
    },
)

@Composable
private fun ReaderModeHeading(
    content: String,
    node: ASTNode,
    style: TextStyle,
    onHoveredLinkChange: (String?) -> Unit,
    contentChildType: IElementType = MarkdownTokenTypes.ATX_CONTENT,
) {
    HoverAwareMarkdownNodeText(
        content = content,
        node = node,
        style = style,
        onHoveredLinkChange = onHoveredLinkChange,
        modifier = Modifier.semantics { heading() },
        contentChildType = contentChildType,
    )
}
