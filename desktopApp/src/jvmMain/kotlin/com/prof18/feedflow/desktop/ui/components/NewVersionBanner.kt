package com.prof18.feedflow.desktop.ui.components

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import com.prof18.feedflow.desktop.utils.WindowWidthSizeClass
import com.prof18.feedflow.desktop.utils.calculateWindowSizeClass
import com.prof18.feedflow.shared.ui.style.Spacing
import com.prof18.feedflow.shared.ui.theme.FeedFlowTheme
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings

@Composable
internal fun NewVersionBanner(
    window: ComposeWindow,
    onDownloadLinkClick: () -> Unit,
    onCloseClick: () -> Unit,
) {
    val windowSize = calculateWindowSizeClass(window)

    when (windowSize.widthSizeClass) {
        WindowWidthSizeClass.Compact -> {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primaryContainer),
            ) {
                CloseButton(
                    modifier = Modifier
                        .align(Alignment.End),
                    onCloseClick = onCloseClick,
                )

                NewVersionMessage()

                DownloadLink(
                    modifier = Modifier.fillMaxWidth()
                        .padding(start = Spacing.regular)
                        .padding(bottom = Spacing.regular),
                    onDownloadLinkClick = onDownloadLinkClick,
                )
            }
        }

        else -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primaryContainer),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                NewVersionMessage()

                DownloadLink(
                    modifier = Modifier
                        .weight(1f),
                    onDownloadLinkClick = onDownloadLinkClick,
                )

                CloseButton(onCloseClick = onCloseClick)
            }
        }
    }
}

@Composable
private fun NewVersionMessage() {
    Text(
        modifier = Modifier
            .padding(Spacing.regular),
        text = LocalFeedFlowStrings.current.newReleaseAvailableTitle,
        color = MaterialTheme.colorScheme.onPrimaryContainer,
        style = MaterialTheme.typography.titleMedium,
    )
}

@Composable
private fun DownloadLink(
    modifier: Modifier = Modifier,
    onDownloadLinkClick: () -> Unit,
) {
    AnnotatedClickableText(
        modifier = modifier,
        onTextClick = onDownloadLinkClick,
    )
}

@Composable
private fun CloseButton(
    modifier: Modifier = Modifier,
    onCloseClick: () -> Unit,
) {
    IconButton(
        modifier = modifier,
        onClick = {
            onCloseClick()
        },
    ) {
        Icon(
            imageVector = Icons.Default.Close,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

@Composable
private fun AnnotatedClickableText(
    modifier: Modifier = Modifier,
    onTextClick: () -> Unit,
) {
    val annotatedText = buildAnnotatedString {
        pushStringAnnotation(
            tag = "URL",
            annotation = "URL",
        )
        withStyle(
            style = SpanStyle(
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.Bold,
                textDecoration = TextDecoration.Underline,
            ),
        ) {
            append(LocalFeedFlowStrings.current.newReleaseAvailableLink)
        }

        pop()
    }

    ClickableText(
        modifier = modifier,
        text = annotatedText,
        style = MaterialTheme.typography.bodyMedium,
        onClick = { offset ->
            annotatedText.getStringAnnotations(
                tag = "URL",
                start = offset,
                end = offset,
            ).firstOrNull()?.let {
                onTextClick()
            }
        },
    )
}

@Preview
@Composable
private fun NewVersionBannerPreview() {
    FeedFlowTheme {
        NewVersionBanner(
            window = ComposeWindow(),
            onDownloadLinkClick = {},
            onCloseClick = {},
        )
    }
}
