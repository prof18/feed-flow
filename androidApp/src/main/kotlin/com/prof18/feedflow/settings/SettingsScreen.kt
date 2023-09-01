@file:OptIn(ExperimentalMaterial3Api::class)

package com.prof18.feedflow.settings

import FeedFlowTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prof18.feedflow.BrowserManager
import com.prof18.feedflow.MR
import com.prof18.feedflow.domain.model.Browser
import com.prof18.feedflow.presentation.preview.browsersForPreview
import com.prof18.feedflow.settings.components.BrowserSelectionDialog
import com.prof18.feedflow.ui.preview.FeedFlowPreview
import com.prof18.feedflow.ui.settings.SettingsDivider
import com.prof18.feedflow.ui.settings.SettingsMenuItem
import com.prof18.feedflow.utils.UserFeedbackReporter
import dev.icerock.moko.resources.compose.stringResource
import org.koin.compose.koinInject

@Composable
fun SettingsScreen(
    onFeedListClick: () -> Unit,
    navigateBack: () -> Unit,
    onAboutClick: () -> Unit,
    navigateToImportExport: () -> Unit,
) {
    val context = LocalContext.current

    val browserManager = koinInject<BrowserManager>()

    val browserListState by browserManager.browserListState.collectAsStateWithLifecycle()

    SettingsScreenContent(
        browsers = browserListState,
        onFeedListClick = onFeedListClick,
        onBrowserSelected = { browser ->
            browserManager.setFavouriteBrowser(browser)
        },
        navigateBack = navigateBack,
        onAboutClick = onAboutClick,
        onBugReportClick = {
            browserManager.openUrlWithFavoriteBrowser(
                url = UserFeedbackReporter.getFeedbackUrl(),
                context = context,
            )
        },
        navigateToImportExport = navigateToImportExport,
    )
}

@Composable
private fun SettingsScreenContent(
    browsers: List<Browser>,
    onFeedListClick: () -> Unit,
    onBrowserSelected: (Browser) -> Unit,
    navigateBack: () -> Unit,
    onAboutClick: () -> Unit,
    onBugReportClick: () -> Unit,
    navigateToImportExport: () -> Unit,
) {
    Scaffold(
        topBar = {
            SettingsNavBar(navigateBack)
        },
    ) { paddingValues ->

        var showBrowserSelection by remember {
            mutableStateOf(
                false,
            )
        }

        if (showBrowserSelection) {
            BrowserSelectionDialog(
                browserList = browsers,
                onBrowserSelected = { browser ->
                    onBrowserSelected(browser)
                },
                dismissDialog = {
                    showBrowserSelection = false
                },
            )
        }

        SettingsList(
            modifier = Modifier
                .padding(paddingValues),
            onFeedListClick = onFeedListClick,
            onBrowserSelectionClick = {
                showBrowserSelection = true
            },
            navigateToImportExport = navigateToImportExport,
            onAboutClick = onAboutClick,
            onBugReportClick = onBugReportClick,
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun SettingsNavBar(navigateBack: () -> Unit) {
    TopAppBar(
        title = {
            Text(
                stringResource(resource = MR.strings.settings_title),
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
}

@Composable
private fun SettingsList(
    modifier: Modifier = Modifier,
    onFeedListClick: () -> Unit,
    onBrowserSelectionClick: () -> Unit,
    navigateToImportExport: () -> Unit,
    onAboutClick: () -> Unit,
    onBugReportClick: () -> Unit,
) {
    LazyColumn(
        modifier = modifier,
    ) {
        item {
            SettingsMenuItem(
                text = stringResource(resource = MR.strings.feeds_title),
            ) {
                onFeedListClick()
            }
        }

        item {
            SettingsDivider()
        }

        item {
            SettingsMenuItem(
                text = stringResource(resource = MR.strings.browser_selection_button),
            ) {
                onBrowserSelectionClick()
            }
        }

        item {
            SettingsDivider()
        }

        item {
            SettingsMenuItem(
                text = stringResource(resource = MR.strings.import_export_opml),
            ) {
                navigateToImportExport()
            }
        }

        item {
            SettingsDivider()
        }

        item {
            SettingsMenuItem(
                text = stringResource(
                    resource = MR.strings.report_issue_button,
                ),
            ) {
                onBugReportClick()
            }
        }

        item {
            SettingsDivider()
        }

        item {
            SettingsMenuItem(
                text = stringResource(
                    resource = MR.strings.about_button,
                ),
            ) {
                onAboutClick()
            }
        }

//            item {
//                SettingsMenuItem(text = "Contact us") {
//                    val intent = Intent(Intent.ACTION_SEND).apply {
//                        type = "plain/text"
//                        putExtra(Intent.EXTRA_EMAIL, arrayOf("mgp.dev.studio@gmail.com"))
//                        putExtra(Intent.EXTRA_SUBJECT, "FeedFlow Info")
//
//                    }
//                    context.startActivity(Intent.createChooser(intent, "Send mail..."))
//                }
//            }
    }
}

@FeedFlowPreview
@Composable
private fun SettingsScreenPreview() {
    FeedFlowTheme {
        SettingsScreenContent(
            browsers = browsersForPreview,
            onFeedListClick = {},
            onBrowserSelected = {},
            navigateBack = {},
            onAboutClick = {},
            onBugReportClick = {},
        ) {}
    }
}
