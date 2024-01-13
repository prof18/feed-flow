package com.prof18.feedflow.android.settings.about

import FeedFlowTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import com.prof18.feedflow.MR
import com.prof18.feedflow.android.BrowserManager
import com.prof18.feedflow.android.BuildConfig
import com.prof18.feedflow.core.utils.Websites.FEED_FLOW_WEBSITE
import com.prof18.feedflow.core.utils.Websites.MG_WEBSITE
import com.prof18.feedflow.shared.ui.about.AboutButtonItem
import com.prof18.feedflow.shared.ui.about.AboutTextItem
import com.prof18.feedflow.shared.ui.about.AuthorText
import com.prof18.feedflow.shared.ui.preview.FeedFlowPhonePreview
import com.prof18.feedflow.shared.ui.style.Spacing
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
        navigateBack = onBackClick,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("LongMethod")
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

                item {
                    Text(
                        modifier = Modifier
                            .fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onBackground,
                        text = stringResource(MR.strings.about_app_version, BuildConfig.VERSION_NAME),
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

@FeedFlowPhonePreview
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
