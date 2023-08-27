@file:OptIn(ExperimentalMaterial3Api::class)

package com.prof18.feedflow.settings

import FeedFlowTheme
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.prof18.feedflow.domain.opml.OpmlInput
import com.prof18.feedflow.domain.opml.OpmlOutput
import com.prof18.feedflow.presentation.SettingsViewModel
import com.prof18.feedflow.presentation.preview.browsersForPreview
import com.prof18.feedflow.settings.components.BrowserSelectionDialog
import com.prof18.feedflow.settings.components.SettingsDivider
import com.prof18.feedflow.settings.components.SettingsMenuItem
import com.prof18.feedflow.ui.preview.FeedFlowPreview
import com.prof18.feedflow.utils.UserFeedbackReporter
import dev.icerock.moko.resources.compose.stringResource
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@Composable
fun SettingsScreen(
    onFeedListClick: () -> Unit,
    navigateBack: () -> Unit,
    onAboutClick: () -> Unit,
) {
    val context = LocalContext.current

    val viewModel = koinViewModel<SettingsViewModel>()
    val browserManager = koinInject<BrowserManager>()

    val browserListState by browserManager.browserListState.collectAsStateWithLifecycle()

    val isImportDone by viewModel.isImportDoneState.collectAsStateWithLifecycle()

    if (isImportDone) {
        val importDoneMessage = stringResource(resource = MR.strings.feeds_import_done_message)
        Toast.makeText(context, importDoneMessage, Toast.LENGTH_SHORT)
            .show()
    }

    val isExportDone by viewModel.isExportDoneState.collectAsStateWithLifecycle()

    if (isExportDone) {
        val exportDoneMessage = stringResource(resource = MR.strings.feeds_export_done_message)
        Toast.makeText(context, exportDoneMessage, Toast.LENGTH_SHORT)
            .show()
    }

    val importingFeedMessage = stringResource(resource = MR.strings.feeds_importing_message)

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(Unit) {
        viewModel.errorState.collect { errorState ->
            snackbarHostState.showSnackbar(
                errorState!!.message.toString(context),
                duration = SnackbarDuration.Short,
            )
        }
    }

    SettingsScreenContent(
        browsers = browserListState,
        snackbarHostState = snackbarHostState,
        onFeedListClick = onFeedListClick,
        importFeed = { uri ->
            viewModel.importFeed(OpmlInput(context.contentResolver.openInputStream(uri)))
            Toast.makeText(context, importingFeedMessage, Toast.LENGTH_SHORT)
                .show()
        },
        exportFeed = {
            viewModel.exportFeed(OpmlOutput(context.contentResolver.openOutputStream(it)))
        },
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
    )
}

@Composable
private fun SettingsScreenContent(
    browsers: List<Browser>,
    snackbarHostState: SnackbarHostState,
    onFeedListClick: () -> Unit,
    importFeed: (Uri) -> Unit,
    exportFeed: (Uri) -> Unit,
    onBrowserSelected: (Browser) -> Unit,
    navigateBack: () -> Unit,
    onAboutClick: () -> Unit,
    onBugReportClick: () -> Unit,
) {
    val openFileAction = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        uri?.let { importFeed(it) }
    }

    val createFileAction = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/x-opml"),
    ) { uri ->
        uri?.let { exportFeed(it) }
    }

    Scaffold(
        topBar = {
            SettingsNavBar(navigateBack)
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
            openFileAction = openFileAction,
            createFileAction = createFileAction,
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

@Suppress("LongMethod")
@Composable
private fun SettingsList(
    modifier: Modifier = Modifier,
    onFeedListClick: () -> Unit,
    onBrowserSelectionClick: () -> Unit,
    openFileAction: ManagedActivityResultLauncher<Array<String>, Uri?>,
    createFileAction: ManagedActivityResultLauncher<String, Uri?>,
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
                text = stringResource(resource = MR.strings.import_feed_button),
            ) {
                openFileAction.launch(arrayOf("*/*"))
            }
        }

        item {
            SettingsDivider()
        }

        item {
            SettingsMenuItem(
                text = stringResource(
                    resource = MR.strings.export_feeds_button,
                ),
            ) {
                createFileAction.launch("feeds-export.opml")
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
            snackbarHostState = SnackbarHostState(),
            onFeedListClick = {},
            importFeed = {},
            exportFeed = {},
            onBrowserSelected = {},
            navigateBack = {},
            onAboutClick = {},
            onBugReportClick = {},
        )
    }
}
