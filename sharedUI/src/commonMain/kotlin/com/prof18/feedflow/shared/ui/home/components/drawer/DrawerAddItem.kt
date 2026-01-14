package com.prof18.feedflow.shared.ui.home.components.drawer

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings
import com.prof18.feedflow.shared.ui.utils.PreviewHelper

@Composable
internal fun DrawerAddItem(
    onAddFeedClicked: () -> Unit,
) {
    NavigationDrawerItem(
        selected = false,
        label = {
            Text(
                text = LocalFeedFlowStrings.current.addFeed,
            )
        },
        icon = {
            Icon(
                imageVector = Icons.Default.AddCircleOutline,
                contentDescription = null,
            )
        },
        colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent),
        onClick = {
            onAddFeedClicked()
        },
    )
}

@Preview
@Composable
private fun DrawerAddItemPreview() {
    PreviewHelper {
        DrawerAddItem(
            onAddFeedClicked = {},
        )
    }
}
