@file:OptIn(ExperimentalMaterial3Api::class)

package com.prof18.feedflow

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FFTopAppBar(
    title: @Composable () -> Unit,
    actionIcon: ImageVector,
    modifier: Modifier = Modifier,
    onActionClick: () -> Unit = {}
) {
    TopAppBar(
        title = title,
        actions = {
            IconButton(onClick = onActionClick) {
                Icon(
                    imageVector = actionIcon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        modifier = modifier,
    )
}


@Preview("Top App Bar")
@Composable
private fun FFTopAppBarPreview() {
    FFTopAppBar(
        title = { Text("FeedFlow") },
        actionIcon = Icons.Default.Settings,
    )
}