package com.prof18.feedflow.desktop.about

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.AlertDialog
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.useResource
import com.mikepenz.aboutlibraries.ui.compose.LibrariesContainer
import com.mikepenz.aboutlibraries.ui.compose.LibraryDefaults
import com.mikepenz.aboutlibraries.ui.compose.chipColors
import com.mikepenz.aboutlibraries.ui.compose.libraryColors
import com.mikepenz.aboutlibraries.ui.compose.rememberLibraries
import com.mikepenz.aboutlibraries.ui.compose.util.strippedLicenseContent
import com.prof18.feedflow.shared.ui.style.Spacing
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings

@Composable
fun LicensesScreen(
    isDarkTheme: Boolean,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(LocalFeedFlowStrings.current.openSourceNavBar)
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            onBackClick()
                        },
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                        )
                    }
                },
            )
        },
    ) { paddingValues ->

        var openLicenseDialog by remember { mutableStateOf<String?>(null) }

        val aboutLibsJson = useResource("aboutlibraries.json") {
            it.bufferedReader().readText()
        }
        val libs by rememberLibraries(aboutLibsJson)

        val backgroundColor = if (isDarkTheme) {
            Color(color = 0xFF1e1e1e)
        } else {
            Color(color = 0xFFf6f8fa)
        }

        val borderColor = if (isDarkTheme) {
            Color(color = 0xFF444444)
        } else {
            Color(color = 0xFFd1d9e0)
        }

        val colors = LibraryDefaults.libraryColors(
            backgroundColor = backgroundColor,
            contentColor = MaterialTheme.colorScheme.onBackground,
            licenseChipColors = LibraryDefaults.chipColors(
                containerColor = borderColor,
            ),
            versionChipColors = LibraryDefaults.chipColors(
                containerColor = borderColor,
            ),
        )
        LibrariesContainer(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize(),
            libraries = libs,
            colors = colors,
            onLibraryClick = { library ->
                openLicenseDialog = library.licenses.firstOrNull()?.strippedLicenseContent ?: ""
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
