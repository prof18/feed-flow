package com.prof18.feedflow.desktop.about

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.AlertDialog
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.useResource
import com.mikepenz.aboutlibraries.ui.compose.LibrariesContainer
import com.mikepenz.aboutlibraries.ui.compose.util.strippedLicenseContent
import com.prof18.feedflow.MR
import com.prof18.feedflow.shared.ui.style.Spacing
import dev.icerock.moko.resources.compose.stringResource

@Composable
fun LicensesScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(resource = MR.strings.open_source_nav_bar))
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            onBackClick()
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

        var openLicenseDialog by remember { mutableStateOf<String?>(null) }

        LibrariesContainer(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize(),
            aboutLibsJson = useResource("aboutlibraries.json") {
                it.bufferedReader().readText()
            },
            onLibraryClick = { library ->
                openLicenseDialog = library.library.licenses.firstOrNull()?.strippedLicenseContent ?: ""
            },
        )

        if (!openLicenseDialog.isNullOrBlank()) {
            LicenseDialog(
                licenseContent = requireNotNull(openLicenseDialog),
                closeDialog = {
                    openLicenseDialog = null
                },
            )
        }
    }
}

@Composable
private fun LicenseDialog(
    licenseContent: String,
    closeDialog: () -> Unit,
) {
    AlertDialog(
        title = null,
        text = null,
        onDismissRequest = {
            closeDialog()
        },
        buttons = {
            val scrollState = rememberScrollState()

            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.background),
            ) {
                IconButton(
                    modifier = Modifier
                        .align(Alignment.Start),
                    onClick = closeDialog,
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                    )
                }

                Surface(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.background)
                        .verticalScroll(scrollState)
                        .fillMaxSize(),
                ) {
                    Text(
                        modifier = Modifier
                            .padding(Spacing.regular),
                        text = licenseContent,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
        },
    )
}
