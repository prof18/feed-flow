package com.prof18.feedflow.desktop.reaadermode

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
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
    val linkColor = MaterialTheme.colorScheme.primary

    LaunchedEffect(htmlContent) {
        richTextState.setHtml(htmlContent)
        richTextState.config.linkColor = linkColor
        richTextState.config.linkTextDecoration = TextDecoration.Underline
    }

    LaunchedEffect(linkColor) {
        richTextState.config.linkColor = linkColor
    }

    val customUriHandler = object : UriHandler {
        override fun openUri(uri: String) {
            onLinkClick(uri)
        }
    }

    CompositionLocalProvider(LocalUriHandler provides customUriHandler) {
        RichText(
            state = richTextState,
            modifier = modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 64.dp),
            fontSize = fontSize.sp,
            lineHeight = (fontSize + 12).sp,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}
