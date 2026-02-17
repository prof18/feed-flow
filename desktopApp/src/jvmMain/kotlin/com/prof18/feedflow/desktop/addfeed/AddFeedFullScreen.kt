package com.prof18.feedflow.desktop.addfeed

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings

@Composable
internal fun AddFeedFullScreen(
    onFeedAdded: () -> Unit,
    navigateBack: () -> Unit,
) {
    AddFeedScreenContent(
        onFeedAdded = onFeedAdded,
        topAppBar = {
            TopAppBar(
                title = {
                    Text(LocalFeedFlowStrings.current.addFeed)
                },
                navigationIcon = {
                    IconButton(
                        onClick = navigateBack,
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                        )
                    }
                },
            )
        },
    )
}
