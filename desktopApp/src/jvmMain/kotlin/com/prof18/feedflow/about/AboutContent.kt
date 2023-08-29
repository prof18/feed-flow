package com.prof18.feedflow.about

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.LocalScrollbarStyle
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.MR
import com.prof18.feedflow.core.utils.Websites.FEED_FLOW_WEBSITE
import com.prof18.feedflow.core.utils.Websites.MG_WEBSITE
import com.prof18.feedflow.openInBrowser
import com.prof18.feedflow.scrollbarStyle
import com.prof18.feedflow.ui.about.AboutButtonItem
import com.prof18.feedflow.ui.about.AboutTextItem
import com.prof18.feedflow.ui.about.AuthorText
import com.prof18.feedflow.ui.style.FeedFlowTheme
import com.prof18.feedflow.ui.style.Spacing
import dev.icerock.moko.resources.compose.stringResource

@Composable
fun AboutContent() {
    FeedFlowTheme {
        Scaffold { paddingValues ->

            var showLicensesScreen by remember { mutableStateOf(false) }

            if (showLicensesScreen) {
                LicensesScreen(
                    onBackClick = {
                        showLicensesScreen = false
                    },
                )
            } else {
                val listState = rememberLazyListState()

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
                            showLicensesScreen = {
                                showLicensesScreen = true
                            },
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
                        nameClicked = {
                            openInBrowser(MG_WEBSITE)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsItemList(
    listState: LazyListState,
    showLicensesScreen: () -> Unit,
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
                onClick = {
                    openInBrowser(FEED_FLOW_WEBSITE)
                },
                buttonText = stringResource(MR.strings.open_website_button),
            )
        }

        item {
            AboutButtonItem(
                onClick = showLicensesScreen,
                buttonText = stringResource(MR.strings.open_source_licenses),
            )
        }
    }
}

@Preview
@Composable
private fun AboutContentPreview() {
    FeedFlowTheme {
        AboutContent()
    }
}
