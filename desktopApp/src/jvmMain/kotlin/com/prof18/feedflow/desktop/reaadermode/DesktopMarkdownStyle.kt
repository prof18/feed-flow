@file:OptIn(org.jetbrains.jewel.foundation.ExperimentalJewelApi::class)

package com.prof18.feedflow.desktop.reaadermode

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.jewel.intui.markdown.standalone.styling.dark
import org.jetbrains.jewel.intui.markdown.standalone.styling.light
import org.jetbrains.jewel.markdown.rendering.InlinesStyling
import org.jetbrains.jewel.markdown.rendering.MarkdownStyling
import org.jetbrains.jewel.markdown.rendering.MarkdownStyling.Heading

internal fun desktopMarkdownStyling(bodyStyle: TextStyle, codeStyle: TextStyle, dark: Boolean): MarkdownStyling {
    fun inlines(sizeDelta: Int, heightDelta: Int): InlinesStyling {
        val style = bodyStyle.copy(
            fontSize = (bodyStyle.fontSize.value + sizeDelta).sp,
            lineHeight = (bodyStyle.fontSize.value + heightDelta).sp,
            fontWeight = FontWeight.SemiBold,
        )
        return if (dark) InlinesStyling.dark(style, codeStyle) else InlinesStyling.light(style, codeStyle)
    }
    // Explicit inline styles avoid the pinned Jewel dark heading factory's light palette and gray H6.
    val heading = Heading(
        h1 = Heading.H1.light(
            inlinesStyling = inlines(sizeDelta = 16, heightDelta = 24),
            underlineWidth = 0.dp,
            padding = PaddingValues(),
        ),
        h2 = Heading.H2.light(
            inlinesStyling = inlines(sizeDelta = 6, heightDelta = 16),
            underlineWidth = 0.dp,
            padding = PaddingValues(),
        ),
        h3 = Heading.H3.light(inlinesStyling = inlines(sizeDelta = 6, heightDelta = 16), padding = PaddingValues()),
        h4 = Heading.H4.light(inlinesStyling = inlines(sizeDelta = 0, heightDelta = 12), padding = PaddingValues()),
        h5 = Heading.H5.light(inlinesStyling = inlines(sizeDelta = 0, heightDelta = 12), padding = PaddingValues()),
        h6 = Heading.H6.light(inlinesStyling = inlines(sizeDelta = 0, heightDelta = 12), padding = PaddingValues()),
    )
    return if (dark) {
        MarkdownStyling.dark(baseTextStyle = bodyStyle, editorTextStyle = codeStyle, heading = heading)
    } else {
        MarkdownStyling.light(baseTextStyle = bodyStyle, editorTextStyle = codeStyle, heading = heading)
    }
}
