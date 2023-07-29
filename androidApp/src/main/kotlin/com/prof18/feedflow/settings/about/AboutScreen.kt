package com.prof18.feedflow.settings.about

import FeedFlowTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import com.prof18.feedflow.BrowserManager
import com.prof18.feedflow.MR
import com.prof18.feedflow.ui.preview.FeedFlowPreview
import com.prof18.feedflow.ui.theme.Spacing
import com.prof18.feedflow.utils.Websites.FEED_FLOW_WEBSITE
import com.prof18.feedflow.utils.Websites.MG_WEBSITE
import dev.icerock.moko.resources.compose.stringResource
import org.koin.compose.koinInject

@Composable
fun AboutScreen(
    onBackClick: () -> Unit,
    navigateToLibrariesScreen: () -> Unit,
) {
    val browserManager = koinInject<BrowserManager>()
    val context = LocalContext.current

    AboutScreenContent(
        licensesClicked = navigateToLibrariesScreen,
        nameClicked = {
            browserManager.openUrl(
                url = MG_WEBSITE,
                context = context,
            )
        },
        onOpenWebsiteClick = {
            browserManager.openUrl(
                url = FEED_FLOW_WEBSITE,
                context = context,
            )
        },
        navigateBack = onBackClick,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AboutScreenContent(
    licensesClicked: () -> Unit,
    nameClicked: () -> Unit,
    onOpenWebsiteClick: () -> Unit,
    navigateBack: () -> Unit = {},
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(resource = MR.strings.about_nav_bar),
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            navigateBack()
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = null,
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues),
        ) {
            LazyColumn(
                modifier = Modifier.weight(1f),
            ) {
                item {
                    AboutTextItem(
                        modifier = Modifier.padding(Spacing.regular),
                    )
                }
                item {
                    AboutButtonItem(
                        onClick = onOpenWebsiteClick,
                        buttonText = stringResource(MR.strings.open_website_button),
                    )
                }
                item {
                    AboutButtonItem(
                        onClick = licensesClicked,
                        buttonText = stringResource(MR.strings.open_source_licenses),
                    )
                }
            }
            AuthorText(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                nameClicked = nameClicked,
            )
        }
    }
}

@Composable
private fun AuthorText(nameClicked: () -> Unit, modifier: Modifier = Modifier) {
    AnnotatedClickableText(
        modifier = modifier
            .padding(Spacing.medium),
        onTextClick = nameClicked,
    )
}

@Composable
private fun AboutButtonItem(
    onClick: () -> Unit,
    buttonText: String,
) {
    Button(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.regular)
            .padding(vertical = Spacing.small),
        onClick = onClick,
    ) {
        Text(buttonText)
    }
}

@Composable
private fun AboutTextItem(
    modifier: Modifier = Modifier,
) {
    Text(
        modifier = modifier,
        color = MaterialTheme.colorScheme.onBackground,
        text = stringResource(MR.strings.about_the_app),
        style = MaterialTheme.typography.bodyMedium,
    )
}

@Composable
private fun AnnotatedClickableText(
    modifier: Modifier = Modifier,
    onTextClick: () -> Unit,
) {
    val annotatedText = buildAnnotatedString {
        append(stringResource(MR.strings.author_label))

        pushStringAnnotation(
            tag = "URL",
            annotation = MG_WEBSITE,
        )
        withStyle(
            style = SpanStyle(
                color = Color.Blue,
                fontWeight = FontWeight.Bold,
            ),
        ) {
            append(" Marco Gomiero")
        }

        pop()
    }

    ClickableText(
        modifier = modifier,
        text = annotatedText,
        style = MaterialTheme.typography.bodyMedium.copy(
            color = MaterialTheme.colorScheme.onBackground,
        ),
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

@FeedFlowPreview
@Composable
private fun AboutScreenPreview() {
    FeedFlowTheme {
        Surface {
            AboutScreenContent(
                licensesClicked = {},
                nameClicked = {},
                onOpenWebsiteClick = {},
            )
        }
    }
}
