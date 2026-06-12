package com.prof18.feedflow.desktop.about

import androidx.compose.foundation.LocalScrollbarStyle
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.core.utils.Websites.FEED_FLOW_WEBSITE
import com.prof18.feedflow.core.utils.Websites.MG_WEBSITE
import com.prof18.feedflow.core.utils.Websites.TRANSLATION_WEBSITE
import com.prof18.feedflow.desktop.ui.components.scrollbarStyle
import com.prof18.feedflow.desktop.utils.openUriSafely
import com.prof18.feedflow.shared.ui.about.AboutButtonItem
import com.prof18.feedflow.shared.ui.about.AboutTextItem
import com.prof18.feedflow.shared.ui.about.AuthorText
import com.prof18.feedflow.shared.ui.style.Spacing
import com.prof18.feedflow.shared.ui.theme.FeedFlowTheme
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings
import kotlinx.coroutines.launch

@Composable
fun AboutContent(
    versionLabel: String,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->

        var showLicensesScreen by remember { mutableStateOf(false) }

        if (showLicensesScreen) {
            LicensesScreen(
                onBackClick = {
                    showLicensesScreen = false
                },
            )
        } else {
            val listState = rememberLazyListState()
            val uriHandler = LocalUriHandler.current
            val scope = rememberCoroutineScope()
            val strings = LocalFeedFlowStrings.current
            fun openExternalUrl(url: String) {
                if (!uriHandler.openUriSafely(url)) {
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            message = strings.browserLaunchError,
                            duration = SnackbarDuration.Short,
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .padding(paddingValues),
            ) {
                Box(
                    modifier = Modifier.fillMaxSize()
                        .padding(paddingValues)
                        .weight(1f)
                        .padding(end = 4.dp),
                ) {
                    SettingsItemList(
                        listState = listState,
                        versionLabel = versionLabel,
                        showLicensesScreen = {
                            showLicensesScreen = true
                        },
                        onOpenUrl = ::openExternalUrl,
                    )

                    CompositionLocalProvider(LocalScrollbarStyle provides scrollbarStyle()) {
                        VerticalScrollbar(
                            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                            adapter = rememberScrollbarAdapter(
                                scrollState = listState,
                            ),
                        )
                    }
                }

                AuthorText(
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    nameClicked = { openExternalUrl(MG_WEBSITE) },
                )
            }
        }
    }
}

@Composable
private fun SettingsItemList(
    listState: LazyListState,
    versionLabel: String,
    showLicensesScreen: () -> Unit,
    onOpenUrl: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier,
        state = listState,
    ) {
        item {
            AboutTextItem(
                modifier = Modifier.padding(Spacing.regular),
            )
        }

        item {
            AboutButtonItem(
                onClick = { onOpenUrl(FEED_FLOW_WEBSITE) },
                buttonText = LocalFeedFlowStrings.current.openWebsiteButton,
            )
        }

        item {
            AboutButtonItem(
                onClick = { onOpenUrl(TRANSLATION_WEBSITE) },
                buttonText = LocalFeedFlowStrings.current.aboutMenuContributeTranslations,
            )
        }

        item {
            AboutButtonItem(
                onClick = showLicensesScreen,
                buttonText = LocalFeedFlowStrings.current.openSourceLicenses,
            )
        }

        item {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = versionLabel,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Preview
@Composable
private fun AboutContentPreview() {
    FeedFlowTheme {
        AboutContent(
            versionLabel = "Version: 1.0.0",
        )
    }
}
