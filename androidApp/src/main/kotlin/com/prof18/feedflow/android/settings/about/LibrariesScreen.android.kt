package com.prof18.feedflow.android.settings.about

import FeedFlowTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mikepenz.aboutlibraries.ui.compose.LibrariesContainer
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings

@Composable
fun LicensesScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FeedFlowTheme {
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
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = null,
                            )
                        }
                    },
                )
            },
        ) { paddingValues ->
            LibrariesContainer(
                Modifier
                    .padding(paddingValues)
                    .fillMaxSize(),
            )
        }
    }
}
