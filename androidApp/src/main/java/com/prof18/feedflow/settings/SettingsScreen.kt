package com.prof18.feedflow.settings

import FeedFlowTheme
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prof18.feedflow.settings.components.SettingsDivider
import com.prof18.feedflow.settings.components.SettingsMenuItem
import com.prof18.feedflow.ui.preview.FeedFlowPreview
import com.prof18.feedflow.ui.theme.Spacing
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onAddFeedClick: () -> Unit,
    onFeedListClick: () -> Unit,
) {

    val context = LocalContext.current

    val viewModel = koinViewModel<SettingsViewModel>()

    val openFileAction = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            viewModel.importFeed(it)
            Toast.makeText(context, "Importing feed", Toast.LENGTH_SHORT)
            .show()
        }
    }

    val isImportDone by viewModel.isImportDoneState.collectAsStateWithLifecycle()

    if (isImportDone) {
        Toast.makeText(context, "Import Done", Toast.LENGTH_SHORT)
            .show()
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Settings") }) }
    ) { paddingValues ->

        LazyColumn(
            modifier = Modifier
                .padding(paddingValues)
        ) {

            item {
                SettingsMenuItem(text = "Import Feed from OPML") {
                    openFileAction.launch(arrayOf("*/*"))
                }
            }

            item {
                SettingsDivider()
            }

            item {
                SettingsMenuItem(text = "Add feed") {
                    onAddFeedClick()
                }
            }

            item {
                SettingsDivider()
            }

            item {
                SettingsMenuItem(text = "Feeds") {
                    onFeedListClick()
                }
            }

            item {
                SettingsDivider()
            }

            item {
                // TODO: add field to VM and persist to settings
                val checkedState = remember { mutableStateOf(true) }

                Row(
                    modifier = Modifier
                        .padding(vertical = Spacing.small)
                        .padding(horizontal = Spacing.regular)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        modifier = Modifier
                            .weight(1f),
                        text = "Delete old feeds every week",
                        style = MaterialTheme.typography.bodyMedium,
                        )

                    Switch(
                        checked = checkedState.value,
                        onCheckedChange = {
                            checkedState.value = it
                            viewModel.scheduleCleaning(it)
                        }
                    )
                }
            }

            item {
                SettingsDivider()
            }

            item {
                SettingsMenuItem(text = "Contact us") {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "plain/text"
                        putExtra(Intent.EXTRA_EMAIL, arrayOf("mgp.dev.studio@gmail.com"))
                        putExtra(Intent.EXTRA_SUBJECT, "FeedFlow Info")

                    }
                    context.startActivity(Intent.createChooser(intent, "Send mail..."))
                }
            }
        }

    }
}

@FeedFlowPreview
@Composable
private fun SettingsScreenPreview() {
    FeedFlowTheme {
        Surface {
            SettingsScreen(
                onAddFeedClick = {},
                onFeedListClick = {},
            )
        }
    }
}


