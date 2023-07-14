package com.prof18.feedflow.settings

import FeedFlowTheme
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import com.prof18.feedflow.MR
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import com.prof18.feedflow.BrowserSelector
import com.prof18.feedflow.domain.opml.OPMLInput
import com.prof18.feedflow.domain.opml.OPMLOutput
import com.prof18.feedflow.presentation.SettingsViewModel
import com.prof18.feedflow.settings.components.BrowserSelectionDialog
import com.prof18.feedflow.settings.components.SettingsDivider
import com.prof18.feedflow.settings.components.SettingsMenuItem
import com.prof18.feedflow.ui.preview.FeedFlowPreview
import dev.icerock.moko.resources.StringResource
import dev.icerock.moko.resources.compose.stringResource
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onFeedListClick: () -> Unit,
    navigateBack: () -> Unit,
) {

    val context = LocalContext.current

    val viewModel = koinViewModel<SettingsViewModel>()
    val browserSelector = koinInject<BrowserSelector>()

    val openFileAction = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            viewModel.importFeed(OPMLInput(context.contentResolver.openInputStream(uri)))
            Toast.makeText(context, "Importing feed", Toast.LENGTH_SHORT)
                .show()
        }
    }

    val createFileURI = remember { mutableStateOf<Uri?>(null) }
    val createFileAction = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/x-opml"),
    ) {
        createFileURI.value = it
    }
    createFileURI.value?.let { uri ->
        viewModel.exportFeed(OPMLOutput(context.contentResolver.openOutputStream(uri)))
    }

    val isImportDone by viewModel.isImportDoneState.collectAsStateWithLifecycle()

    if (isImportDone) {
        Toast.makeText(context, "Import Done", Toast.LENGTH_SHORT)
            .show()
    }

    val isExportDone by viewModel.isExportDoneState.collectAsStateWithLifecycle()

    if (isExportDone) {
        Toast.makeText(context, "Export Done", Toast.LENGTH_SHORT)
            .show()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
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
                }
            )
        }
    ) { paddingValues ->

        var showBrowserSelection by remember {
            mutableStateOf(
                false,
            )
        }
        val browserListState by browserSelector.browserListState.collectAsStateWithLifecycle()

        if (showBrowserSelection) {
            BrowserSelectionDialog(
                browserList = browserListState,
                onBrowserSelected = { browser ->
                    browserSelector.setFavouriteBrowser(browser)
                },
                dismissDialog = {
                    showBrowserSelection = false
                },
            )
        }

        LazyColumn(
            modifier = Modifier
                .padding(paddingValues)
        ) {

            item {
//                SettingsMenuItem(text = stringResource(MR.strings.my_string)) {
                SettingsMenuItem(text = "Feeds") {
                    onFeedListClick()
                }
            }

            item {
                SettingsDivider()
            }

            item {
                SettingsMenuItem(text = "Browser Selection") {
                    showBrowserSelection = true
                }
            }

            item {
                SettingsDivider()
            }

            item {
                SettingsMenuItem(text = "Import Feed from OPML") {
                    openFileAction.launch(arrayOf("*/*"))
                }
            }

            item {
                SettingsDivider()
            }

            item {
                SettingsMenuItem(text = "Export Feeds to OPML") {
                    createFileAction.launch("feeds-export.opml")
                }
            }

            item {
                SettingsDivider()
            }

            item {
                SettingsMenuItem(text = "About") {
                    // TODO
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
}

@FeedFlowPreview
@Composable
private fun SettingsScreenPreview() {
    FeedFlowTheme {
        Surface {
            SettingsScreen(
                onFeedListClick = {},
                navigateBack = {},
            )
        }
    }
}


