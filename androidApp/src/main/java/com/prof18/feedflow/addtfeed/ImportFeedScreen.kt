package com.prof18.feedflow.addtfeed

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.prof18.feedflow.settings.SettingsViewModel
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ImportFeedScreen() {

    val viewModel = koinViewModel<SettingsViewModel>()

    val context = LocalContext.current
    val openFileURI = remember { mutableStateOf<Uri?>(null) }
    val openFileAction = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) {
        openFileURI.value = it
    }
    openFileURI.value?.let { uri ->
        viewModel.importFeed(uri)
        // TODO: move this toast from here??
        Toast.makeText(context, "Start importing", Toast.LENGTH_SHORT)
            .show()
    }

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
        ) {
            Button(
                onClick = {
                    openFileAction.launch(arrayOf("*/*"))
                },
            ) {
                Text("Select OPML")
            }
        }
    }

}

