package com.prof18.feedflow.android.settings.about

import FeedFlowTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import com.prof18.feedflow.android.BrowserManager
import com.prof18.feedflow.android.BuildConfig
import com.prof18.feedflow.core.utils.TestingTag
import com.prof18.feedflow.core.utils.Websites.FEED_FLOW_WEBSITE
import com.prof18.feedflow.core.utils.Websites.MG_WEBSITE
import com.prof18.feedflow.core.utils.Websites.TRANSLATION_WEBSITE
import com.prof18.feedflow.shared.ui.about.AboutButtonItem
import com.prof18.feedflow.shared.ui.about.AboutTextItem
import com.prof18.feedflow.shared.ui.about.AuthorText
import com.prof18.feedflow.shared.ui.preview.PreviewPhone
import com.prof18.feedflow.shared.ui.style.Spacing
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings
import com.prof18.feedflow.shared.ui.utils.tagForTesting
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
            browserManager.openUrlWithDefaultBrowser(
                url = MG_WEBSITE,
                context = context,
            )
        },
        onOpenWebsiteClick = {
            browserManager.openUrlWithDefaultBrowser(
                url = FEED_FLOW_WEBSITE,
                context = context,
            )
        },
        onHelpWithTranslationsClick = {
            browserManager.openUrlWithDefaultBrowser(
                url = TRANSLATION_WEBSITE,
                context = context,
            )
        },
        navigateBack = onBackClick,
    )
}

@Composable
private fun AboutScreenContent(
    licensesClicked: () -> Unit,
    nameClicked: () -> Unit,
    onOpenWebsiteClick: () -> Unit,
    onHelpWithTranslationsClick: () -> Unit,
    navigateBack: () -> Unit = {},
) {
    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier
                    .tagForTesting(TestingTag.ABOUT_TOOLBAR),
                title = {
                    Text(
                        LocalFeedFlowStrings.current.aboutNavBar,
                    )
                },
                navigationIcon = {
                    IconButton(
                        modifier = Modifier
                            .tagForTesting(TestingTag.BACK_BUTTON_FEED_SETTINGS),
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
                        buttonText = LocalFeedFlowStrings.current.openWebsiteButton,
                    )
                }
                item {
                    AboutButtonItem(
                        onClick = onHelpWithTranslationsClick,
                        buttonText = LocalFeedFlowStrings.current.aboutMenuContributeTranslations,
                    )
                }
                item {
                    AboutButtonItem(
                        onClick = licensesClicked,
                        buttonText = LocalFeedFlowStrings.current.openSourceLicenses,
                    )
                }
                item {
                    Text(
                        modifier = Modifier
                            .fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onBackground,
                        text = LocalFeedFlowStrings.current.aboutAppVersion(BuildConfig.VERSION_NAME),
                        style = MaterialTheme.typography.bodySmall,
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

@PreviewPhone
@Composable
private fun AboutScreenPreview() {
    FeedFlowTheme {
        Surface {
            AboutScreenContent(
                licensesClicked = {},
                nameClicked = {},
                onOpenWebsiteClick = {},
                onHelpWithTranslationsClick = {},
            )
        }
    }
}
