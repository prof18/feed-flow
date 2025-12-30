package com.prof18.feedflow.desktop.reaadermode

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import com.mohamedrejeb.richeditor.ui.material3.RichText

@Composable
internal fun RichTextContent(
    htmlContent: String,
    fontSize: Int,
    modifier: Modifier = Modifier,
    onLinkClick: (String) -> Unit = {},
) {
    val richTextState = rememberRichTextState()

    LaunchedEffect(htmlContent) {
        richTextState.setHtml(htmlContent)
    }

    val linkColor = MaterialTheme.colorScheme.primary

    RichText(
        state = richTextState,
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = 64.dp),
        style = MaterialTheme.typography.bodyLarge.copy(
            fontSize = fontSize.sp,
            lineHeight = (fontSize + 12).sp,
        ),
        linkStyle = SpanStyle(
            color = linkColor,
            fontWeight = FontWeight.Medium,
            textDecoration = TextDecoration.Underline,
        ),
        onLinkClick = { url ->
            onLinkClick(url)
        },
    )
}
